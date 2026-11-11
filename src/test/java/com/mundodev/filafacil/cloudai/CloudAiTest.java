package com.mundodev.filafacil.cloudai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.cloudai.cache.MemoryResponseCache;
import com.mundodev.filafacil.cloudai.functions.PayloadValidator;
import com.mundodev.filafacil.cloudai.functions.PromptCleaner;
import com.mundodev.filafacil.cloudai.functions.ResponseNormalizer;
import com.mundodev.filafacil.cloudai.infra.InfraTagValidator;
import com.mundodev.filafacil.cloudai.store.ArtifactStore;
import com.mundodev.filafacil.cloudai.vector.DocumentCatalog;
import com.mundodev.filafacil.cloudai.vector.DocumentFamily;
import com.mundodev.filafacil.cloudai.vector.LocalVectorIndex;
import com.mundodev.filafacil.cloudai.vector.OperationalDocument;
import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;
import com.mundodev.filafacil.inference.ModelProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudAiTest {

    private static final QuotaPolicy TIGHT = new QuotaPolicy(1, 200, 1, 1000, 1, 60, 1);

    @TempDir
    Path temp;

    @Test
    void quotasDeHomologEProdSaoMaisRestritasQueDev() {
        QuotaCatalog catalog = new QuotaCatalog();
        QuotaPolicy dev = catalog.forEnv(AiEnvironment.DEV);
        QuotaPolicy homolog = catalog.forEnv(AiEnvironment.HOMOLOG);
        QuotaPolicy prod = catalog.forEnv(AiEnvironment.PROD);
        assertTrue(prod.rateLimitPerMinute() < homolog.rateLimitPerMinute());
        assertTrue(homolog.rateLimitPerMinute() < dev.rateLimitPerMinute());
        assertTrue(prod.tokenQuota() < dev.tokenQuota());
        assertFalse(AiEnvironment.HOMOLOG.secretEnvVar().equals(AiEnvironment.DEV.secretEnvVar()));
        assertFalse(AiEnvironment.PROD.secretEnvVar().equals(AiEnvironment.DEV.secretEnvVar()));
    }

    @Test
    void homologEProdNaoHerdamChaveDeDevEProdRecusaArquivoDoRepo() throws Exception {
        SecretSource.Decision homolog = SecretSource.resolve(
                AiEnvironment.HOMOLOG,
                Map.of("FILAFACIL_LLM_API_KEY", "dev-only"),
                temp);
        assertTrue(homolog.value().isBlank());
        assertFalse(homolog.refused());

        SecretSource.Decision prodEmpty = SecretSource.resolve(AiEnvironment.PROD, Map.of(), temp);
        assertTrue(prodEmpty.refused());

        Path secretFile = temp.resolve("segredo.env");
        Files.writeString(secretFile, "nao-ler");
        SecretSource.Decision prodFile = SecretSource.resolve(
                AiEnvironment.PROD,
                Map.of("FILAFACIL_LLM_API_KEY_PROD", secretFile.toString()),
                temp);
        assertTrue(prodFile.refused());
        assertTrue(prodFile.reason().contains("repositorio"));
    }

    @Test
    void redigeChavesAntesDePersistir() throws Exception {
        assertFalse(Redactor.mask("token gsk_abcDEF123").contains("gsk_"));
        assertFalse(Redactor.mask("token sk-abcDEF123").contains("sk-"));
        assertFalse(Redactor.mask("token AQ.Ab8exemplo").contains("AQ."));
        assertEquals("limpa", PromptCleaner.clean("limpa"));

        ArtifactStore store = new ArtifactStore(temp, "dev");
        Path prompt = store.savePrompt("triage-cloud", 1, "usar gsk_abcDEF123");
        String saved = Files.readString(prompt);
        assertFalse(saved.contains("gsk_"));
        store.savePrompt("triage-cloud", 1, "OUTRO");
        assertEquals(saved, Files.readString(prompt));

        ObjectNode body = new ObjectMapper().createObjectNode();
        body.put("promptVersion", "triage-cloud/v1");
        body.put("erro", "falha sk-abcDEF123");
        Path inference = store.saveInference("2026-11-11", "inf-1", body);
        String json = Files.readString(inference);
        assertFalse(json.contains("sk-"));
        assertTrue(json.contains("triage-cloud/v1"));
        assertTrue(inference.toString().replace('\\', '/').contains("dev/inferences/"));
    }

    @Test
    void filaRejeitaTextoVazioSemChamarModelo() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Path root = temp.resolve("fila");
        try (TriagePipeline pipeline = new TriagePipeline(
                root, AiEnvironment.DEV, TIGHT, okGateway(calls), new MemoryResponseCache(), "v1", "v1")) {
            pipeline.enqueue("", "cliente");
            pipeline.drain(1_000L);
            assertEquals(0, calls.get());
            String stored = Files.readString(root.resolve("dev/queue/messages.json"));
            assertTrue(stored.contains("REJEITADA"));
        }
    }

    @Test
    void funcoesServerlessValidamLimpamENormalizam() {
        assertFalse(PayloadValidator.validate("", "cliente").valid());
        assertTrue(PayloadValidator.validate("coleta", "cliente").valid());
        assertFalse(PromptCleaner.clean("gsk_abcDEF123").contains("gsk_"));
        ResponseNormalizer.Reply reply = ResponseNormalizer.normalize("{\"tipoSolicitacao\":\"consulta_entrega\",\"resumoOperacional\":\"ok\"}");
        assertEquals("consulta_entrega", reply.tipo());
        assertEquals("revisar manualmente", reply.proximaAcao());
    }

    @Test
    void documentosDasQuatroFamiliasEFamiliaInvalida() {
        var docs = DocumentCatalog.loadDefault();
        assertEquals(4, docs.size());
        assertTrue(docs.stream().anyMatch(doc -> doc.familia() == DocumentFamily.REGRAS_DE_COLETA));
        assertTrue(docs.stream().anyMatch(doc -> doc.familia() == DocumentFamily.POLITICA_DE_ENTREGA));
        assertTrue(docs.stream().anyMatch(doc -> doc.familia() == DocumentFamily.TIPOS_DE_OCORRENCIA));
        assertTrue(docs.stream().anyMatch(doc -> doc.familia() == DocumentFamily.INSTRUCOES_OPERACIONAIS));
        assertThrows(IllegalArgumentException.class, () -> DocumentCatalog.parse("[{\"id\":\"a\",\"familia\":\"fiscal\",\"texto\":\"x\"}]"));
    }

    @Test
    void indiceVersionadoEncontraContextoEPreservaVersaoAntiga() throws Exception {
        LocalVectorIndex index = new LocalVectorIndex(temp, "dev", "operacao");
        List<OperationalDocument> docs = DocumentCatalog.loadDefault();
        index.reprocess("1", docs);
        index.reprocess("2", List.of(new OperationalDocument("outro", DocumentFamily.REGRAS_DE_COLETA, "zzzz unico")));
        var hit = index.search("Regra de coleta conferir CEP quantidade de pacotes", "1");
        assertTrue(hit.isPresent());
        assertEquals("1", hit.get().version());
        assertEquals(DocumentFamily.REGRAS_DE_COLETA, hit.get().document().familia());
        assertTrue(index.search("zzzzqqqq sem nexo", "1").isEmpty());
        long before = Files.size(index.versionDir("2").resolve("index.json"));
        index.reprocess("1", docs);
        assertEquals(before, Files.size(index.versionDir("2").resolve("index.json")));
        assertTrue(index.versionExists("1"));
        assertTrue(index.versionExists("2"));
    }

    @Test
    void drainComSucessoTimeoutELimiteDeTaxa() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Path root = temp.resolve("sucesso");
        try (TriagePipeline pipeline = new TriagePipeline(
                root, AiEnvironment.DEV, TIGHT, okGateway(calls), new MemoryResponseCache(), "v1", "v1")) {
            pipeline.enqueue("Regra de coleta conferir CEP e pacotes", "cliente");
            RunSummary summary = pipeline.drain(1_000L);
            assertEquals(1, calls.get());
            assertTrue(summary.render().contains("chamadas=1"));
            assertTrue(summary.alerta());
            assertTrue(Files.exists(root.resolve("dev/datasets/raw/operacao/batch.json")));
            assertTrue(Files.exists(root.resolve("dev/datasets/processed/operacao/batch.json")));
        }

        AtomicInteger slow = new AtomicInteger();
        QuotaPolicy timeout = new QuotaPolicy(1, 200, 10, 1000, 10000, 60, 1);
        try (TriagePipeline pipeline = pipeline(slow, request -> {
            slow.incrementAndGet();
            try {
                Thread.sleep(2500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            throw new InferenceException("timeout");
        }, timeout)) {
            pipeline.enqueue("consulta de entrega", "operador");
            pipeline.drain(2_000L);
            assertEquals(1, slow.get());
        }

        AtomicInteger limited = new AtomicInteger();
        try (TriagePipeline pipeline = pipeline(limited, okGateway(limited), TIGHT)) {
            pipeline.enqueue("primeira solicitacao de coleta", "cliente");
            pipeline.enqueue("segunda solicitacao diferente", "parceiro");
            pipeline.drain(3_000L);
            assertEquals(1, limited.get());
        }
    }

    @Test
    void cacheReusaRespostaEErraQuandoVersaoOuPrazoMuda() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        QuotaPolicy quota = new QuotaPolicy(5, 200, 10, 10000, 10000, 10, 1);
        MemoryResponseCache cache = new MemoryResponseCache();
        try (TriagePipeline pipeline = new TriagePipeline(temp, AiEnvironment.DEV, quota, okGateway(calls), cache, "v1", "v1")) {
            pipeline.enqueue("Regra de coleta conferir CEP", "cliente");
            pipeline.drain(0L);
            pipeline.enqueue("Regra de coleta conferir CEP", "cliente");
            pipeline.drain(1_000L);
            assertEquals(1, calls.get());
            pipeline.enqueue("Regra de coleta conferir CEP", "cliente");
            pipeline.drain(20_000L);
            assertEquals(2, calls.get());
        }
        try (TriagePipeline other = new TriagePipeline(temp.resolve("v2"), AiEnvironment.DEV, quota, okGateway(calls), cache, "v2", "v1")) {
            other.enqueue("Regra de coleta conferir CEP", "cliente");
            other.drain(1_000L);
            assertEquals(3, calls.get());
        }
    }

    @Test
    void cliGratuitaNaoAbreMenuNovoERecusaProvedorPago() throws Exception {
        Map<String, String> env = Map.of("FILAFACIL_AI_ENV", "dev", "FILAFACIL_AI_GATEWAY", "mock");
        assertEquals(0, CloudAiMain.execute(
                new String[]{"enqueue", "--text", "consulta de coleta", "--origin", "cliente"},
                env, temp, temp));
        assertEquals(0, CloudAiMain.execute(new String[]{"drain"}, env, temp, temp));

        var paid = CloudAiMain.gateway(
                Map.of("FILAFACIL_AI_GATEWAY", "http", "FILAFACIL_LLM_BASE_URL", "https://api.openai.com/v1", "FILAFACIL_LLM_API_KEY", "sk-pago"),
                SecretSource.Decision.ok("sk-pago"));
        assertTrue(paid instanceof com.mundodev.filafacil.inference.MockInferenceGateway);

        String menu = Files.readString(Path.of("src/main/java/com/mundodev/filafacil/controller/MenuController.java"));
        assertTrue(menu.contains("12 - Pipeline inter"));
        assertFalse(menu.contains("13 -"));
        assertEquals(1, CloudAiMain.execute(
                new String[]{"drain"},
                Map.of("FILAFACIL_AI_ENV", "prod"),
                temp.resolve("prod"),
                temp));
        String readme = Files.readString(Path.of("README.md"));
        assertTrue(readme.contains("exec:java@cloud-ai"));
        assertTrue(readme.contains("enqueue"));
        assertTrue(readme.contains("drain"));
    }

    @Test
    void infraExigeTagsENaoUsaAdministrador() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InfraTagValidator.validate(mapper.readTree(Path.of("infra/ai/platform.json").toFile()));
        ObjectNode invalid = mapper.createObjectNode();
        invalid.putArray("resources").addObject().put("type", "sqs");
        assertThrows(IllegalArgumentException.class, () -> InfraTagValidator.validate(invalid));
        String iam = Files.readString(Path.of("infra/ai/iam-ai-role-policy.json"));
        assertFalse(iam.contains("AdministratorAccess"));
        assertFalse(iam.contains("\"Action\": \"*\""));
        String scaling = Files.readString(Path.of("infra/ai/auto-scaling.json"));
        assertTrue(scaling.contains("\"minInstances\": 1"));
        assertTrue(scaling.contains("\"maxInstances\": 3"));
    }

    private TriagePipeline pipeline(AtomicInteger calls, InferenceGateway gateway, QuotaPolicy quota) throws Exception {
        return new TriagePipeline(temp.resolve(calls.toString() + System.nanoTime()), AiEnvironment.DEV, quota, gateway, new MemoryResponseCache(), "v1", "v1");
    }

    private static InferenceGateway okGateway(AtomicInteger calls) {
        return request -> {
            calls.incrementAndGet();
            return new InferenceResponse(
                    "{\"tipoSolicitacao\":\"consulta_entrega\",\"resumoOperacional\":\"ok\",\"proximaAcao\":\"conferir coleta\"}",
                    ModelProfile.LIGHT,
                    "mock",
                    1,
                    0,
                    false);
        };
    }

}
