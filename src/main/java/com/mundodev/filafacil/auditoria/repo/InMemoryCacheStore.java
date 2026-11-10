package com.mundodev.filafacil.auditoria.repo;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCacheStore implements CacheStore {

    private record Entry(String value, Instant expiresAt) {
    }

    private final Map<String, Entry> data = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        Entry entry = data.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            data.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        data.put(key, new Entry(value, Instant.now().plusSeconds(Math.max(1, ttlSeconds))));
    }

    @Override
    public void delete(String key) {
        data.remove(key);
    }
}
