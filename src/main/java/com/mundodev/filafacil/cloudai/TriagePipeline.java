package com.mundodev.filafacil.cloudai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.cloudai.cache.MemoryResponseCache;
import com.mundodev.filafacil.cloudai.cache.ResponseCache;
import com.mundodev.filafacil.cloudai.functions.PayloadValidator;
import com.mundodev.filafacil.cloudai.functions.PromptCleaner;
import com.mundodev.filafacil.cloudai.functions.ResponseNormalizer;
import com.mundodev.filafacil.cloudai.queue.FileRequestQueue;
import com.mundodev.filafacil.cloudai.queue.QueueMessage;
import com.mundodev.filafacil.cloudai.queue.QueueState;
import com.mundodev.filafacil.cloudai.store.ArtifactStore;
import com.mundodev.filafacil.cloudai.vector.DocumentCatalog;
import com.mundodev.filafacil.cloudai.vector.LocalVectorIndex;
import com.mundodev.filafacil.cloudai.vector.OperationalDocument;
import com.mundodev.filafacil.cloudai.vector.ScoredDocument;
import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceParams;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;
import com.mundodev.filafacil.inference.ModelProfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TriagePipeline implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DOMAIN = "operacao";
    private static final String PROMPT_ID = "triage-cloud";

    private final AiEnvironment environment;
    private final QuotaPolicy quota;
    private final InferenceGateway gateway;
    private final ResponseCache cache;
    private final FileRequestQueue queue;
    private final LocalVectorIndex index;
    private final ArtifactStore artifacts;
    private final String promptVersion;
    private final String documentVersion;
    private final ExecutorService calls = Executors.newSingleThreadExecutor();
    private final ArrayDeque<Long> callTimes = new ArrayDeque<>();
    private int usedTokens;
    private int modelCalls;
    private long latencyMs;
    private int errors;
    private double cost;
    private boolean alert;

    public TriagePipeline(
            Path root,
            AiEnvironment environment,
            QuotaPolicy quota,
            InferenceGateway gateway,
            ResponseCache cache,
            String promptVersion,
            String documentVersion) throws IOException {
        this.environment = environment;
        this.quota = quota;
        this.gateway = gateway;
        this.cache = cache;
        this.promptVersion = promptVersion;
        this.documentVersion = documentVersion;
        this.queue = new FileRequestQueue(root, environment.id());
        this.index = new LocalVectorIndex(root, environment.id(), DOMAIN);
        this.artifacts = new ArtifactStore(root, environment.id());
        ensureIndex();
    }

    public String enqueue(String text, String origin) throws IOException {
        return queue.enqueue(text, origin);
    }

    public RunSummary drain(long nowMs) throws IOException {
        QueueMessage message;
        while ((message = queue.nextAccepted()) != null) {
            process(message, nowMs);
        }
        return summary();
    }

    public int modelCalls() {
        return modelCalls;
    }

    public RunSummary summary() throws IOException {
        return new RunSummary(usedTokens, modelCalls, latencyMs, errors, artifacts.storageBytes(), queue.depth(), cost, alert);
    }

    private void process(QueueMessage message, long nowMs) throws IOException {
        PayloadValidator.Result validation = PayloadValidator.validate(message.text(), message.origin());
        if (!validation.valid()) {
            queue.update(message.withState(QueueState.REJEITADA, validation.reason()));
            errors++;
            return;
        }
        Optional<ScoredDocument> hit = index.search(message.text(), documentVersion);
        String context = hit.map(item -> item.document().texto()).orElse("");
        String prompt = loadPrompt() + "\nContexto: " + (context.isBlank() ? "ausente" : context);
        String cleaned = PromptCleaner.clean(prompt + "\n" + message.text());
        artifacts.savePrompt(PROMPT_ID, versionNumber(promptVersion), cleaned);
        String cacheKey = MemoryResponseCache.key(environment.id(), promptVersion, documentVersion, message.text());
        Optional<String> cached = cache.get(cacheKey, nowMs);
        if (cached.isPresent()) {
            finish(message, cached.get(), hit, "cache", 0, 0, 0, "");
            return;
        }
        if (usedTokens >= quota.tokenQuota()) {
            failLimited(message, "quota de tokens");
            return;
        }
        if (!allowCall(nowMs)) {
            failLimited(message, "limite de taxa");
            return;
        }
        try {
            InferenceResponse response = callLimited(message.text(), cleaned, nowMs);
            ResponseNormalizer.Reply reply = ResponseNormalizer.normalize(response.rawJson());
            String json = replyJson(message.id(), reply, hit);
            cache.put(cacheKey, json, nowMs, quota.cacheTtlSeconds());
            int tokens = Math.max(1, response.rawJson().length() / 4);
            usedTokens += tokens;
            latencyMs += response.latencyMs();
            cost += response.estimatedCostUsd();
            if (usedTokens >= quota.alertTokenThreshold()) {
                alert = true;
            }
            finish(message, json, hit, "sucesso", tokens, response.latencyMs(), response.estimatedCostUsd(), "");
        } catch (InferenceException ex) {
            errors++;
            String reason = Redactor.mask(ex.getMessage());
            queue.update(message.withState(QueueState.FALHA, reason));
            recordInference(message.id(), "", 0, 0, 0, "erro", reason);
        }
    }

    private void failLimited(QueueMessage message, String reason) throws IOException {
        errors++;
        queue.update(message.withState(QueueState.FALHA, reason));
        recordInference(message.id(), "", 0, 0, 0, "erro", reason);
    }

    private InferenceResponse callLimited(String text, String prompt, long nowMs) {
        InferenceException last = null;
        InferenceRequest request = new InferenceRequest(
                text,
                ModelProfile.LIGHT,
                new InferenceParams(0.2, 0.9, quota.maxTokens(), 42L),
                "cloud-ai-free",
                prompt);
        int attempts = Math.max(1, quota.maxAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            modelCalls++;
            callTimes.addLast(nowMs);
            try {
                var future = calls.submit(() -> gateway.complete(request));
                return future.get(Math.max(1, quota.timeoutSeconds()), TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                last = new InferenceException("timeout");
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                if (cause instanceof InferenceException inference) {
                    last = inference;
                } else {
                    last = new InferenceException(cause.getMessage(), cause);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new InferenceException("interrompido", ex);
            }
        }
        throw last == null ? new InferenceException("falha de modelo") : last;
    }

    private boolean allowCall(long nowMs) {
        long windowStart = nowMs - 60_000L;
        while (!callTimes.isEmpty() && callTimes.peekFirst() < windowStart) {
            callTimes.removeFirst();
        }
        return callTimes.size() < quota.rateLimitPerMinute();
    }

    private void finish(
            QueueMessage message,
            String json,
            Optional<ScoredDocument> hit,
            String status,
            int tokens,
            long latency,
            double estimatedCost,
            String error) throws IOException {
        queue.update(message.withState(QueueState.CONCLUIDA, ""));
        recordInference(message.id(), json, tokens, latency, estimatedCost, status, error);
        artifacts.saveEvaluation("cloud-ai-free", LocalDate.now().toString(), "triagem",
                "{\"status\":\"" + status + "\",\"id\":\"" + message.id() + "\"}");
    }

    private void recordInference(
            String id,
            String resposta,
            int tokens,
            long latency,
            double estimatedCost,
            String status,
            String error) throws IOException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("modelo", "cloud-ai-free");
        body.put("promptVersion", PROMPT_ID + "/" + promptVersion);
        body.put("latenciaMs", latency);
        body.put("tokens", tokens);
        body.put("custoEstimado", estimatedCost);
        body.put("status", status);
        body.put("erro", error);
        body.put("resposta", resposta);
        artifacts.saveInference(LocalDate.now().toString(), id, body);
    }

    private String replyJson(String id, ResponseNormalizer.Reply reply, Optional<ScoredDocument> hit) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("inferenceId", id);
        node.put("tipoSolicitacao", reply.tipo());
        node.put("resumo", reply.resumo());
        node.put("proximaAcao", reply.proximaAcao());
        ObjectNode contexto = node.putObject("contexto");
        if (hit.isEmpty()) {
            contexto.put("ausente", true);
        } else {
            contexto.put("familia", hit.get().document().familia().id());
            contexto.put("versao", hit.get().version());
            contexto.put("similaridade", hit.get().similarity());
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception ex) {
            return "{\"inferenceId\":\"" + id + "\"}";
        }
    }

    private void ensureIndex() throws IOException {
        List<OperationalDocument> documents = DocumentCatalog.loadDefault();
        if (!index.versionExists(documentVersion)) {
            index.reprocess(documentVersion, documents);
        }
        String raw = MAPPER.writeValueAsString(documents);
        artifacts.writeDatasets(DOMAIN, raw, raw);
    }

    private String loadPrompt() throws IOException {
        try (var in = TriagePipeline.class.getResourceAsStream("/cloudai/prompt-triage-v1.txt")) {
            if (in == null) {
                return "Classifique a solicitacao.";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int versionNumber(String version) {
        String digits = version.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 1;
        }
        return Integer.parseInt(digits);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void close() {
        calls.shutdownNow();
    }
}
