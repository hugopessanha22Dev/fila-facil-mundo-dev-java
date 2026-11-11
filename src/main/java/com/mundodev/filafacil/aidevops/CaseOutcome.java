package com.mundodev.filafacil.aidevops;

public record CaseOutcome(
        String id,
        String category,
        boolean inputRejected,
        boolean jsonValid,
        boolean schemaOk,
        boolean summaryPresent,
        boolean tipoMatch,
        boolean prioridadeMatch,
        boolean fallback,
        boolean humanIntervention,
        int retries,
        int modelCalls,
        long latencyMs,
        double costUsd,
        int tokens,
        String model,
        String error,
        String redactedResponse,
        String promptVersion,
        String configVersion) {

    public boolean accepted() {
        return schemaOk && !humanIntervention && !inputRejected;
    }
}
