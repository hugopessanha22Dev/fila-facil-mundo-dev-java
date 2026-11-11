package com.mundodev.filafacil.cloudai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class SecretSource {

    public record Decision(String value, boolean refused, String reason) {
        public static Decision ok(String value) {
            return new Decision(value, false, "");
        }

        public static Decision absent() {
            return new Decision("", false, "ausente");
        }

        public static Decision refused(String reason) {
            return new Decision("", true, reason);
        }
    }

    private SecretSource() {
    }

    public static Decision resolve(AiEnvironment env, Map<String, String> envVars, Path repoRoot) {
        String raw = envVars.get(env.secretEnvVar());
        if (raw == null || raw.isBlank()) {
            if (env == AiEnvironment.PROD) {
                return Decision.refused("chave de producao ausente");
            }
            return Decision.absent();
        }
        String value = raw.trim();
        if (pointsInsideRepo(value, repoRoot)) {
            return Decision.refused("chave aponta para arquivo dentro do repositorio");
        }
        return Decision.ok(value);
    }

    static boolean pointsInsideRepo(String value, Path repoRoot) {
        boolean looksLikePath = value.contains("/") || value.contains("\\") || value.endsWith(".env");
        if (!looksLikePath) {
            return false;
        }
        try {
            Path candidate = Path.of(value);
            if (!candidate.isAbsolute()) {
                candidate = repoRoot.resolve(candidate);
            }
            candidate = candidate.normalize().toAbsolutePath();
            Path root = repoRoot.toAbsolutePath().normalize();
            return candidate.startsWith(root) && Files.exists(candidate);
        } catch (Exception ex) {
            return false;
        }
    }
}
