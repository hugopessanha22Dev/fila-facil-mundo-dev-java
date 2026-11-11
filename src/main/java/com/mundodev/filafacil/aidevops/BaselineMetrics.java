package com.mundodev.filafacil.aidevops;

public record BaselineMetrics(
        double accuracy,
        double extraction,
        double validJson,
        double schemaAdherence,
        double summary,
        double costUsd,
        double latencyMs) {
}
