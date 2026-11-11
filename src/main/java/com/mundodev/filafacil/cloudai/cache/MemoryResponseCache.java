package com.mundodev.filafacil.cloudai.cache;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoryResponseCache implements ResponseCache {

    private record Entry(String value, long expiresAtMs) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key, long nowMs) {
        Entry entry = entries.get(key);
        if (entry == null || nowMs >= entry.expiresAtMs) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    @Override
    public void put(String key, String value, long nowMs, int ttlSeconds) {
        entries.put(key, new Entry(value, nowMs + ttlSeconds * 1000L));
    }

    public static String key(String environment, String promptVersion, String documentVersion, String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        return environment + "|" + promptVersion + "|" + documentVersion + "|" + normalized;
    }
}
