package com.mundodev.filafacil.api;

import com.mundodev.filafacil.api.repo.AgendamentoRepository;
import com.mundodev.filafacil.api.repo.InMemoryAgendamentoRepository;
import com.mundodev.filafacil.api.repo.JdbcAgendamentoRepository;
import com.mundodev.filafacil.api.service.AgendamentoService;
import com.mundodev.filafacil.api.storage.AttachmentStorage;
import com.mundodev.filafacil.api.storage.LocalFileAttachmentStorage;
import com.mundodev.filafacil.api.storage.S3AttachmentStorage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Path;

/**
 * Entrypoint HTTP da API de agendamento de docas (separado do terminal).
 */
public class ApiMain {

    private static final Logger log = LoggerFactory.getLogger(ApiMain.class);

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnv();
        log.info("Iniciando API FilaFacil. {}", config.safeSummary());

        HikariDataSource dataSource = null;
        AgendamentoRepository repository;
        if (config.memoryDb()) {
            log.info("Usando repositorio em memoria (FILAFACIL_DB_URL=memory). Preferir Postgres via docker compose.");
            repository = new InMemoryAgendamentoRepository();
        } else {
            dataSource = createDataSource(config);
            JdbcAgendamentoRepository jdbc = new JdbcAgendamentoRepository(dataSource);
            jdbc.ensureSchema();
            repository = jdbc;
        }

        AttachmentStorage storage = createStorage(config);
        AgendamentoService service = new AgendamentoService(repository, storage);
        Javalin app = ApiApp.create(service);

        HikariDataSource dsFinal = dataSource;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Encerrando API...");
            app.stop();
            if (dsFinal != null) {
                dsFinal.close();
            }
        }));

        app.start(config.port());
        log.info("API ouvindo em http://localhost:{}/health", config.port());
    }

    static HikariDataSource createDataSource(AppConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.dbUrl());
        hc.setUsername(config.dbUser());
        hc.setPassword(config.dbPassword());
        hc.setMaximumPoolSize(5);
        hc.setPoolName("filafacil-api");
        return new HikariDataSource(hc);
    }

    static AttachmentStorage createStorage(AppConfig config) {
        if (config.prod()) {
            S3Client s3 = S3Client.builder()
                    .region(Region.of(config.awsRegion()))
                    .build();
            return new S3AttachmentStorage(s3, config.s3Bucket(), config.maxAnexoBytes());
        }
        return new LocalFileAttachmentStorage(Path.of(config.anexosDir()), config.maxAnexoBytes());
    }
}
