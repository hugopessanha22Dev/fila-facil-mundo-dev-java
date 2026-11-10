package com.mundodev.filafacil.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mundodev.filafacil.api.dto.CreateAgendamentoRequest;
import com.mundodev.filafacil.api.dto.UpdateStatusRequest;
import com.mundodev.filafacil.api.error.ApiValidationException;
import com.mundodev.filafacil.api.error.PersistenceFailureException;
import com.mundodev.filafacil.api.model.Agendamento;
import com.mundodev.filafacil.api.service.AgendamentoService;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ApiApp {

    private static final Logger log = LoggerFactory.getLogger(ApiApp.class);

    private ApiApp() {
    }

    public static Javalin create(AgendamentoService service) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, true));
            config.showJavalinBanner = false;
        });

        app.before(ctx -> log.info("HTTP {} {}", ctx.method(), ctx.path()));

        app.get("/health", ctx -> {
            log.info("health ok");
            ctx.json(Map.of("status", "ok"));
        });

        app.post("/agendamentos", ctx -> {
            CreateAgendamentoRequest body = ctx.bodyAsClass(CreateAgendamentoRequest.class);
            Agendamento created = service.criar(
                    body.transportadora(), body.placa(), body.doca(), body.dataHora());
            ctx.status(201).json(created);
        });

        app.get("/agendamentos", ctx -> {
            String data = ctx.queryParam("data");
            List<Agendamento> list = (data == null || data.isBlank())
                    ? service.listar()
                    : service.listarPorData(data);
            ctx.json(list);
        });

        app.patch("/agendamentos/{id}/status", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            UpdateStatusRequest body = ctx.bodyAsClass(UpdateStatusRequest.class);
            ctx.json(service.atualizarStatus(id, body.status()));
        });

        app.post("/agendamentos/{id}/cancelar", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(service.cancelar(id));
        });

        app.post("/agendamentos/{id}/anexo", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            UploadedFile file = ctx.uploadedFile("file");
            if (file == null) {
                throw new ApiValidationException("Campo multipart 'file' e obrigatorio.");
            }
            ctx.json(service.anexar(id, file.filename(), file.content(), file.size()));
        });

        app.exception(ApiValidationException.class, (ex, ctx) -> {
            log.warn("Validacao: {}", ex.getMessage());
            ctx.status(400).json(errorBody("validacao", ex.getMessage()));
        });

        app.exception(PersistenceFailureException.class, (ex, ctx) -> {
            log.error("Persistencia indisponivel.");
            ctx.status(503).json(errorBody("persistencia", PersistenceFailureException.PUBLIC_MESSAGE));
        });

        app.exception(NumberFormatException.class, (ex, ctx) -> {
            ctx.status(400).json(errorBody("validacao", "Id invalido."));
        });

        app.exception(Exception.class, (ex, ctx) -> {
            log.error("Erro inesperado: {}", ex.getClass().getSimpleName());
            ctx.status(500).json(errorBody("interno", "Erro interno."));
        });

        return app;
    }

    private static Map<String, String> errorBody(String tipo, String mensagem) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("tipo", tipo);
        body.put("mensagem", mensagem);
        return body;
    }
}
