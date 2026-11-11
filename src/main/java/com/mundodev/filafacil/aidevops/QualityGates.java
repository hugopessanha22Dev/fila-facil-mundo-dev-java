package com.mundodev.filafacil.aidevops;

public record QualityGates(
        double minValidJson,
        double minSchemaAdherence,
        double minAccuracy,
        double maxCostUsd,
        double maxLatencyMs,
        double maxAccuracyDrop,
        double maxExtractionDrop) {
}
