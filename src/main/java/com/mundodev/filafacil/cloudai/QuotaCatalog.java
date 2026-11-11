package com.mundodev.filafacil.cloudai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public final class QuotaCatalog {

    private final ObjectMapper mapper = new ObjectMapper();

    public QuotaPolicy forEnv(AiEnvironment env) {
        try (InputStream in = QuotaCatalog.class.getResourceAsStream("/cloudai/cloud-ai-quotas.json")) {
            if (in == null) {
                throw new IllegalStateException("cloud-ai-quotas.json ausente");
            }
            JsonNode root = mapper.readTree(in);
            JsonNode node = root.path(env.id());
            if (node.isMissingNode()) {
                throw new IllegalStateException("Quota ausente para " + env.id());
            }
            return new QuotaPolicy(
                    node.path("timeoutSeconds").asInt(),
                    node.path("maxTokens").asInt(),
                    node.path("rateLimitPerMinute").asInt(),
                    node.path("tokenQuota").asInt(),
                    node.path("alertTokenThreshold").asInt(),
                    node.path("cacheTtlSeconds").asInt(),
                    node.path("maxAttempts").asInt());
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Falha ao ler quotas", ex);
        }
    }
}
