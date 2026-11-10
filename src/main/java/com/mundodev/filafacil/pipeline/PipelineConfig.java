package com.mundodev.filafacil.pipeline;

import java.nio.file.Path;

public record PipelineConfig(
        Path pipelineRoot,
        String dbUrl,
        String dbUser,
        String dbPassword,
        int maxRetries) {

    public static PipelineConfig fromEnv() {
        String root = env("FILAFACIL_PIPELINE_ROOT", "data/pipeline");
        return new PipelineConfig(
                Path.of(root),
                env("FILAFACIL_DB_URL", "jdbc:postgresql://localhost:5432/filafacil"),
                env("FILAFACIL_DB_USER", "filafacil"),
                env("FILAFACIL_DB_PASSWORD", "filafacil"),
                Integer.parseInt(env("FILAFACIL_PIPELINE_RETRIES", "2")));
    }

    public boolean memoryDb() {
        return dbUrl == null || dbUrl.isBlank() || "memory".equalsIgnoreCase(dbUrl);
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v.trim();
    }
}
