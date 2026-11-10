package com.mundodev.filafacil.inference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Le .env na raiz do projeto sem sobrescrever variavel ja definida no processo.
 * System.getenv nao pode ser alterado; o mapa fica so para InferenceConfig.
 */
final class EnvFile {

    private static final Map<String, String> FROM_FILE = new HashMap<>();
    private static boolean loaded;

    private EnvFile() {
    }

    static String get(String key) {
        loadOnce();
        String fromProcess = System.getenv(key);
        if (fromProcess != null && !fromProcess.isBlank()) {
            return fromProcess;
        }
        return FROM_FILE.get(key);
    }

    private static synchronized void loadOnce() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = Path.of(".env");
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                put(line);
            }
        } catch (IOException ex) {
            System.out.println("[env] nao foi possivel ler .env: " + ex.getMessage());
        }
    }

    private static void put(String rawLine) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String key = line.substring(0, eq).trim();
        String value = unquote(line.substring(eq + 1).trim());
        if (!key.isEmpty() && !value.isEmpty()) {
            FROM_FILE.put(key, value);
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
