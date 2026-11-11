package com.mundodev.filafacil.cloudai;

public enum AiEnvironment {
    DEV("dev", "FILAFACIL_LLM_API_KEY"),
    HOMOLOG("homolog", "FILAFACIL_LLM_API_KEY_HOMOLOG"),
    PROD("prod", "FILAFACIL_LLM_API_KEY_PROD");

    private final String id;
    private final String secretEnvVar;

    AiEnvironment(String id, String secretEnvVar) {
        this.id = id;
        this.secretEnvVar = secretEnvVar;
    }

    public String id() {
        return id;
    }

    public String secretEnvVar() {
        return secretEnvVar;
    }

    public static AiEnvironment parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEV;
        }
        String value = raw.trim().toLowerCase();
        for (AiEnvironment env : values()) {
            if (env.id.equals(value)) {
                return env;
            }
        }
        throw new IllegalArgumentException("FILAFACIL_AI_ENV invalido: " + raw);
    }
}
