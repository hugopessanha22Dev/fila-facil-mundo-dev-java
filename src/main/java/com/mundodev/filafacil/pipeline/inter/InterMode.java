package com.mundodev.filafacil.pipeline.inter;

public enum InterMode {
    BATCH,
    INCREMENTAL;

    public static InterMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return INCREMENTAL;
        }
        return "batch".equalsIgnoreCase(raw.trim()) ? BATCH : INCREMENTAL;
    }
}
