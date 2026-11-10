package com.mundodev.filafacil.auditoria.repo;

import java.util.Optional;

public interface CacheStore {

    Optional<String> get(String key);

    void set(String key, String value, int ttlSeconds);

    void delete(String key);
}
