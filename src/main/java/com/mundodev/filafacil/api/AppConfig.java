package com.mundodev.filafacil.api;

import java.util.Locale;
import java.util.Objects;

/**
 * Configuracao externa via env vars. Sem credenciais no codigo.
 */
public record AppConfig(
        String mode,
        int port,
        String dbUrl,
        String dbUser,
        String dbPassword,
        String s3Bucket,
        String awsRegion,
        String anexosDir,
        long maxAnexoBytes) {

    public static final String MODE_LOCAL = "local";
    public static final String MODE_PROD = "prod";

    public boolean local() {
        return MODE_LOCAL.equalsIgnoreCase(mode);
    }

    public boolean prod() {
        return MODE_PROD.equalsIgnoreCase(mode);
    }

    /** Fallback offline quando Docker/Postgres nao esta disponivel. */
    public boolean memoryDb() {
        return "memory".equalsIgnoreCase(dbUrl);
    }

    public static AppConfig fromEnv() {
        String mode = env("FILAFACIL_API_MODE", MODE_LOCAL).toLowerCase(Locale.ROOT);
        if (!MODE_LOCAL.equals(mode) && !MODE_PROD.equals(mode)) {
            throw new IllegalArgumentException("FILAFACIL_API_MODE deve ser local ou prod.");
        }
        int port = Integer.parseInt(env("FILAFACIL_API_PORT", "8080"));
        return new AppConfig(
                mode,
                port,
                env("FILAFACIL_DB_URL", "jdbc:postgresql://localhost:5432/filafacil"),
                env("FILAFACIL_DB_USER", "filafacil"),
                env("FILAFACIL_DB_PASSWORD", "filafacil"),
                env("FILAFACIL_S3_BUCKET", ""),
                env("FILAFACIL_AWS_REGION", "us-east-1"),
                env("FILAFACIL_ANEXOS_DIR", "data/anexos"),
                Long.parseLong(env("FILAFACIL_MAX_ANEXO_BYTES", "2097152")));
    }

    /** Nunca incluir senha na string de diagnostico. */
    public String safeSummary() {
        return "mode=" + mode
                + " port=" + port
                + " dbUrl=" + redactUrl(dbUrl)
                + " dbUser=" + dbUser
                + " s3Bucket=" + (s3Bucket == null || s3Bucket.isBlank() ? "(none)" : s3Bucket)
                + " region=" + awsRegion
                + " anexosDir=" + anexosDir
                + " maxAnexoBytes=" + maxAnexoBytes;
    }

    static String redactUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(?i):[^/@]+@", ":***@");
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return Objects.requireNonNull(defaultValue);
        }
        return value.trim();
    }
}
