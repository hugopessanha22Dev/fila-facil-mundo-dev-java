package com.mundodev.filafacil.inference;

public record InferenceResponse(
        String rawJson,
        ModelProfile profileUsed,
        String modelId,
        long latencyMs,
        double estimatedCostUsd,
        boolean usedFallback) {
}
