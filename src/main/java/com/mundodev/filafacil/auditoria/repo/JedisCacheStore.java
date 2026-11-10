package com.mundodev.filafacil.auditoria.repo;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;

public class JedisCacheStore implements CacheStore, AutoCloseable {

    private final JedisPool pool;

    public JedisCacheStore(String host, int port) {
        this.pool = new JedisPool(host, port);
    }

    @Override
    public Optional<String> get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return Optional.ofNullable(jedis.get(key));
        }
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, Math.max(1, ttlSeconds), value);
        }
    }

    @Override
    public void delete(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
