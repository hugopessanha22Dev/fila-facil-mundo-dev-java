package com.mundodev.filafacil.pipeline.inter;

public enum LatePolicy {
    MARK,
    REJECT,
    ACCEPT;

    public static LatePolicy parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return MARK;
        }
        return switch (raw.trim().toLowerCase()) {
            case "reject" -> REJECT;
            case "accept" -> ACCEPT;
            default -> MARK;
        };
    }
}
