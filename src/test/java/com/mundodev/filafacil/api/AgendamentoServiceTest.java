package com.mundodev.filafacil.api;

import com.mundodev.filafacil.api.error.ApiValidationException;
import com.mundodev.filafacil.api.error.PersistenceFailureException;
import com.mundodev.filafacil.api.model.Agendamento;
import com.mundodev.filafacil.api.repo.InMemoryAgendamentoRepository;
import com.mundodev.filafacil.api.service.AgendamentoService;
import com.mundodev.filafacil.api.storage.LocalFileAttachmentStorage;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendamentoServiceTest {

    @TempDir
    Path tempDir;

    private InMemoryAgendamentoRepository repository;
    private AgendamentoService service;
    private Javalin app;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAgendamentoRepository();
        service = new AgendamentoService(
                repository,
                new LocalFileAttachmentStorage(tempDir, 2048));
        app = ApiApp.create(service);
    }

    @Test
    void criarRejeitaCampoObrigatorio() {
        assertThrows(ApiValidationException.class,
                () -> service.criar("", "ABC1D23", "Doca-1", "2026-11-10T10:00:00"));
    }

    @Test
    void criarListarFiltrarAtualizarCancelar() {
        Agendamento criado = service.criar("TransDemo", "ABC1D23", "Doca-1", "2026-11-10T10:00:00");
        assertNotNull(criado.id());
        assertEquals(AgendamentoService.STATUS_AGENDADO, criado.status());
        assertEquals(1, service.listar().size());
        assertEquals(1, service.listarPorData("2026-11-10").size());
        assertEquals(0, service.listarPorData("2026-11-11").size());

        Agendamento atualizado = service.atualizarStatus(criado.id(), "em_atendimento");
        assertEquals("em_atendimento", atualizado.status());

        Agendamento cancelado = service.cancelar(criado.id());
        assertEquals(AgendamentoService.STATUS_CANCELADO, cancelado.status());
    }

    @Test
    void anexoPequenoGravaReferencia() {
        Agendamento criado = service.criar("TransDemo", "ABC1D23", "Doca-1", "2026-11-10T10:00:00");
        byte[] bytes = "comprovante".getBytes(StandardCharsets.UTF_8);
        Agendamento comAnexo = service.anexar(
                criado.id(), "nota.txt", new ByteArrayInputStream(bytes), bytes.length);
        assertNotNull(comAnexo.anexoRef());
        assertTrue(comAnexo.anexoRef().contains("nota.txt"));
    }

    @Test
    void mensagemPublicaDePersistenciaNaoExpoeSenha() {
        PersistenceFailureException ex = new PersistenceFailureException(
                new RuntimeException("FATAL password=super-secret jdbc:postgresql://u:p@host/db"));
        assertEquals(PersistenceFailureException.PUBLIC_MESSAGE, ex.getMessage());
        assertFalse(ex.getMessage().contains("super-secret"));
        assertFalse(ex.getMessage().contains("password="));
    }

    @Test
    void healthEEndpointsHttpPrincipais() {
        JavalinTest.test(app, (server, client) -> {
            var health = client.get("/health");
            assertEquals(200, health.code());
            String healthBody = health.body().string();
            assertTrue(healthBody.contains("ok"));

            var created = client.post("/agendamentos", """
                    {"transportadora":"T1","placa":"XYZ1A23","doca":"D1","dataHora":"2026-11-10T09:00:00"}
                    """);
            assertEquals(201, created.code());

            var list = client.get("/agendamentos");
            assertEquals(200, list.code());

            var filtered = client.get("/agendamentos?data=2026-11-10");
            assertEquals(200, filtered.code());

            long id = repository.findAll().get(0).id();
            var patched = client.patch("/agendamentos/" + id + "/status", "{\"status\":\"concluido\"}");
            assertEquals(200, patched.code());

            var canceled = client.post("/agendamentos/" + id + "/cancelar");
            assertEquals(200, canceled.code());
        });
    }

    @Test
    void persistenciaFalhaRetorna503SemDetalheSensivel() {
        var failingRepo = new InMemoryAgendamentoRepository() {
            @Override
            public java.util.List<Agendamento> findAll() {
                throw new PersistenceFailureException(
                        new RuntimeException("password=leak jdbc:postgresql://x:y@z/db"));
            }
        };
        Javalin failingApp = ApiApp.create(new AgendamentoService(
                failingRepo, new LocalFileAttachmentStorage(tempDir, 2048)));

        JavalinTest.test(failingApp, (server, client) -> {
            var response = client.get("/agendamentos");
            assertEquals(503, response.code());
            String body = response.body().string();
            assertTrue(body.contains(PersistenceFailureException.PUBLIC_MESSAGE)
                    || body.contains("Falha de persistencia"));
            assertFalse(body.contains("password="));
            assertFalse(body.contains("leak"));
        });
    }
}
