package com.mundodev.filafacil.auditoria;

public record AuditoriaConfig(
        boolean enabled,
        String dbUrl,
        String dbUser,
        String dbPassword,
        String redisHost,
        int redisPort,
        int redisTtlSeconds,
        String mongoUri,
        String mongoDatabase) {

    public static AuditoriaConfig fromEnv() {
        boolean enabled = !"off".equalsIgnoreCase(env("FILAFACIL_AUDITORIA", "on"));
        return new AuditoriaConfig(
                enabled,
                env("FILAFACIL_DB_URL", "jdbc:postgresql://localhost:5432/filafacil"),
                env("FILAFACIL_DB_USER", "filafacil"),
                env("FILAFACIL_DB_PASSWORD", "filafacil"),
                env("FILAFACIL_REDIS_HOST", "localhost"),
                Integer.parseInt(env("FILAFACIL_REDIS_PORT", "6379")),
                Integer.parseInt(env("FILAFACIL_REDIS_TTL_SECONDS", "60")),
                env("FILAFACIL_MONGO_URI", "mongodb://localhost:27017"),
                env("FILAFACIL_MONGO_DB", "filafacil_auditoria"));
    }

    public boolean memoryMode() {
        return "memory".equalsIgnoreCase(dbUrl);
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}
