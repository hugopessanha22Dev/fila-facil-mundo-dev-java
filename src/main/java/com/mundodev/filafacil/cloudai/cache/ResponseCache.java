package com.mundodev.filafacil.cloudai.cache;

import java.util.Optional;

public interface ResponseCache {

    Optional<String> get(String key, long nowMs);

    void put(String key, String value, long nowMs, int ttlSeconds);
}
