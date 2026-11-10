package com.mundodev.filafacil.inference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.error.InferenceException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpOpenAiCompatibleGateway implements InferenceGateway {

    private final InferenceConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public HttpOpenAiCompatibleGateway(InferenceConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build(), new ObjectMapper());
    }

    public HttpOpenAiCompatibleGateway(InferenceConfig config, HttpClient httpClient, ObjectMapper mapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new InferenceException(
                    "FILAFACIL_LLM_API_KEY ausente. Defina a chave ou use FILAFACIL_LLM_MODE=mock.");
        }

        InferenceException lastError = null;
        int attempts = Math.max(1, config.maxRetries() + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            long start = System.nanoTime();
            try {
                String body = buildRequestBody(request);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(trimSlash(config.baseUrl()) + "/chat/completions"))
                        .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                        .header("Authorization", "Bearer " + config.apiKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                long latencyMs = (System.nanoTime() - start) / 1_000_000L;
                if (response.statusCode() >= 500 || response.statusCode() == 429) {
                    throw new InferenceException("Erro transitório do provedor HTTP " + response.statusCode());
                }
                if (response.statusCode() >= 400) {
                    throw new InferenceException("Erro do provedor HTTP " + response.statusCode() + ": " + response.body());
                }

                String content = extractContent(response.body());
                String json = stripMarkdownFence(content);
                String systemPrompt = request.resolvedSystemPrompt();
                int promptTokens = estimateTokens(systemPrompt + request.mensagem());
                int completionTokens = estimateTokens(json);
                double cost = ((promptTokens + completionTokens) / 1000.0) * config.pricePer1kTokensUsd();
                System.out.println("[inference-http] profile=" + request.profile()
                        + " model=" + request.modelId()
                        + " attempt=" + attempt
                        + " latencyMs=" + latencyMs
                        + " costUsd~" + round6(cost)
                        + " ok=true");
                return new InferenceResponse(json, request.profile(), request.modelId(), latencyMs, round6(cost), false);
            } catch (InferenceException ex) {
                lastError = ex;
                System.out.println("[inference-http] attempt=" + attempt + " falhou: " + ex.getMessage());
            } catch (Exception ex) {
                lastError = new InferenceException("Falha de rede/timeout na inferencia: " + ex.getMessage(), ex);
                System.out.println("[inference-http] attempt=" + attempt + " falhou: " + lastError.getMessage());
            }
        }
        throw lastError != null ? lastError : new InferenceException("Falha desconhecida na inferencia HTTP.");
    }

    private String buildRequestBody(InferenceRequest request) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", request.modelId());
        root.put("temperature", request.params().temperature());
        root.put("top_p", request.params().topP());
        root.put("max_tokens", request.params().maxTokens());
        if (request.params().seed() != null) {
            root.put("seed", request.params().seed());
        }
        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", request.resolvedSystemPrompt());
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", request.mensagem());
        return mapper.writeValueAsString(root);
    }

    private String extractContent(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
            throw new InferenceException("Resposta do provedor sem content utilizavel.");
        }
        return content.asText();
    }

    private static String stripMarkdownFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                return trimmed.substring(firstNl + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private static String trimSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }

    private static double round6(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
