package com.mundodev.filafacil.auditoria;

import com.mundodev.filafacil.auditoria.repo.InMemoryAuditEventStore;
import com.mundodev.filafacil.auditoria.repo.InMemoryAuditoriaStore;
import com.mundodev.filafacil.auditoria.repo.InMemoryCacheStore;
import com.mundodev.filafacil.auditoria.repo.JedisCacheStore;
import com.mundodev.filafacil.auditoria.repo.JdbcEntregaQueryRepository;
import com.mundodev.filafacil.auditoria.repo.JdbcReembolsoRepository;
import com.mundodev.filafacil.auditoria.repo.MongoAuditEventStore;
import com.mundodev.filafacil.auditoria.service.AuditoriaService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Composition root do modulo de auditoria.
 * Preferencia: Postgres + Redis + Mongo; fallback memory se env/DB indisponivel.
 */
public final class AuditoriaModule {

    private AuditoriaModule() {
    }

    public static AuditoriaService create(AuditoriaConfig config) {
        if (!config.enabled()) {
            return null;
        }
        if (config.memoryMode()) {
            System.out.println("[auditoria] modo memory (FILAFACIL_DB_URL=memory)");
            return createMemory(config.redisTtlSeconds());
        }
        try {
            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl(config.dbUrl());
            hc.setUsername(config.dbUser());
            hc.setPassword(config.dbPassword());
            hc.setMaximumPoolSize(3);
            hc.setPoolName("filafacil-auditoria");
            HikariDataSource ds = new HikariDataSource(hc);
            // smoke
            try (var c = ds.getConnection()) {
                c.createStatement().execute("SELECT 1");
            }

            var cache = tryRedis(config);
            var audit = tryMongo(config);
            System.out.println("[auditoria] Postgres OK | cache="
                    + cache.getClass().getSimpleName()
                    + " | audit=" + audit.getClass().getSimpleName());
            return new AuditoriaService(
                    new JdbcEntregaQueryRepository(ds),
                    new JdbcReembolsoRepository(ds),
                    cache,
                    audit,
                    config.redisTtlSeconds());
        } catch (Exception ex) {
            System.out.println("[auditoria] Postgres indisponivel (" + ex.getClass().getSimpleName()
                    + "). Usando store em memoria. Suba: docker compose up -d");
            return createMemory(config.redisTtlSeconds());
        }
    }

    public static AuditoriaService createMemory(int ttlSeconds) {
        InMemoryAuditoriaStore store = new InMemoryAuditoriaStore();
        return new AuditoriaService(
                store,
                store,
                new InMemoryCacheStore(),
                new InMemoryAuditEventStore(),
                ttlSeconds);
    }

    private static com.mundodev.filafacil.auditoria.repo.CacheStore tryRedis(AuditoriaConfig config) {
        try {
            JedisCacheStore jedis = new JedisCacheStore(config.redisHost(), config.redisPort());
            jedis.set("filafacil:auditoria:ping", "1", 5);
            return jedis;
        } catch (Exception ex) {
            System.out.println("[auditoria] Redis indisponivel — cache em memoria.");
            return new InMemoryCacheStore();
        }
    }

    private static com.mundodev.filafacil.auditoria.repo.AuditEventStore tryMongo(AuditoriaConfig config) {
        try {
            return new MongoAuditEventStore(config.mongoUri(), config.mongoDatabase());
        } catch (Exception ex) {
            System.out.println("[auditoria] Mongo indisponivel — eventos em memoria.");
            return new InMemoryAuditEventStore();
        }
    }
}
