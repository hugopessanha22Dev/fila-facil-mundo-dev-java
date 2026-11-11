package com.mundodev.filafacil.cloudai.cache;

import redis.clients.jedis.Jedis;

import java.util.Optional;

public final class RedisResponseCache implements ResponseCache, AutoCloseable {

    private final Jedis jedis;

    private RedisResponseCache(Jedis jedis) {
        this.jedis = jedis;
    }

    public static Optional<RedisResponseCache> tryOpen(String host, int port) {
        try {
            Jedis jedis = new Jedis(host, port, 300);
            jedis.ping();
            return Optional.of(new RedisResponseCache(jedis));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> get(String key, long nowMs) {
        String value = jedis.get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(String key, String value, long nowMs, int ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        jedis.setex(key, ttlSeconds, value);
    }

    @Override
    public void close() {
        jedis.close();
    }
}
