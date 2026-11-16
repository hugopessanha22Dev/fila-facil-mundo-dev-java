package com.mundodev.filafacil.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public record AgentLimits(int maxSteps, int maxAttemptsPerTool, int maxCalls, int maxDurationMs) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public AgentLimits(int maxSteps, int maxAttemptsPerTool, int maxCalls) {
        this(maxSteps, maxAttemptsPerTool, maxCalls, 60_000);
    }

    public AgentLimits {
        if (maxSteps < 1 || maxAttemptsPerTool < 1 || maxCalls < 1 || maxDurationMs < 1) {
            throw new IllegalArgumentException("Limites do agente devem ser positivos.");
        }
    }

    public static AgentLimits load() {
        try (InputStream in = AgentLimits.class.getResourceAsStream("/agent/limits.json")) {
            if (in == null) {
                throw new IllegalStateException("limits.json ausente");
            }
            JsonNode node = MAPPER.readTree(in);
            return new AgentLimits(
                    node.path("maxSteps").asInt(),
                    node.path("maxAttemptsPerTool").asInt(),
                    node.path("maxCalls").asInt(),
                    node.path("maxDurationMs").asInt(60_000));
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler limits.json", ex);
        }
    }
}
