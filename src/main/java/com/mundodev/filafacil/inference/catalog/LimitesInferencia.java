package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;

public record LimitesInferencia(
        int rateLimitPorTarefa,
        int rateLimitPorProvedor,
        int timeoutMs,
        int maxTentativas,
        int backoffInicialMs,
        int limiarCircuito,
        int cooldownMs,
        double limiteRegressao) {

    public LimitesInferencia {
        if (rateLimitPorTarefa < 1 || rateLimitPorProvedor < 1 || maxTentativas < 1 || limiarCircuito < 1) {
            throw new IllegalArgumentException("Limites de inferencia devem ser positivos.");
        }
    }

    public static LimitesInferencia carregar() {
        JsonNode node = Recursos.ler("/inference/catalog/limites.json");
        return new LimitesInferencia(
                node.path("rateLimitPorTarefa").asInt(),
                node.path("rateLimitPorProvedor").asInt(),
                node.path("timeoutMs").asInt(),
                node.path("maxTentativas").asInt(),
                node.path("backoffInicialMs").asInt(),
                node.path("limiarCircuito").asInt(),
                node.path("cooldownMs").asInt(),
                node.path("limiteRegressao").asDouble());
    }
}
