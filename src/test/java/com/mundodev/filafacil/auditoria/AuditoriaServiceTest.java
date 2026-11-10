package com.mundodev.filafacil.auditoria;

import com.mundodev.filafacil.auditoria.model.EntregaFiltro;
import com.mundodev.filafacil.auditoria.model.EntregaRow;
import com.mundodev.filafacil.auditoria.model.PageResult;
import com.mundodev.filafacil.auditoria.model.ResumoAuditoria;
import com.mundodev.filafacil.auditoria.repo.InMemoryAuditoriaStore;
import com.mundodev.filafacil.auditoria.repo.InMemoryAuditEventStore;
import com.mundodev.filafacil.auditoria.repo.InMemoryCacheStore;
import com.mundodev.filafacil.auditoria.service.AuditoriaService;
import com.mundodev.filafacil.error.EntradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaServiceTest {

    private InMemoryAuditoriaStore store;
    private InMemoryCacheStore cache;
    private InMemoryAuditEventStore events;
    private AuditoriaService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryAuditoriaStore();
        cache = new InMemoryCacheStore();
        events = new InMemoryAuditEventStore();
        service = new AuditoriaService(store, store, cache, events, 60);
    }

    @Test
    void paginacaoComFiltroStatus() {
        PageResult<EntregaRow> page1 = service.listarEntregas(
                new EntregaFiltro("entregue", null, null, null, null, null), 1, 2);
        assertEquals(2, page1.items().size());
        assertTrue(page1.total() >= 2);
        assertTrue(page1.hasNext() || page1.total() <= 2);
    }

    @Test
    void filtroPeriodoECliente() {
        PageResult<EntregaRow> page = service.listarEntregas(
                new EntregaFiltro(null, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"), 1L, null, null),
                1, 10);
        assertFalse(page.items().isEmpty());
        assertTrue(page.items().stream().allMatch(e -> e.clienteId() == 1L));
    }

    @Test
    void leftJoinIncluiSemOcorrencia() {
        long sem = service.demoLeftJoin().stream().filter(e -> e.ocorrenciaTipo() == null).count();
        assertTrue(sem >= 1);
    }

    @Test
    void rightJoinIncluiMotoristaSemEntrega() {
        assertTrue(service.demoRightJoin().stream().anyMatch(s -> s.contains("sem entregas") || s.contains("entregas=0")));
    }

    @Test
    void aprovacaoCommitEEvento() {
        service.aprovarReembolso(1L);
        assertEquals("aprovado", store.findById(1L).orElseThrow().get("status"));
        assertFalse(service.listarEventos(5).isEmpty());
        assertEquals("REEMBOLSO_APROVADO", service.listarEventos(5).get(0).tipo());
    }

    @Test
    void aprovacaoComFalhaFazRollback() {
        store.forceFailNextApproval();
        assertThrows(IllegalStateException.class, () -> service.aprovarReembolso(2L));
        assertEquals("pendente", store.findById(2L).orElseThrow().get("status"));
        assertTrue(service.listarEventos(5).isEmpty());
    }

    @Test
    void cacheHitEInvalidacao() {
        ResumoAuditoria first = service.resumoCacheado();
        assertFalse(first.fromCache());
        ResumoAuditoria second = service.resumoCacheado();
        assertTrue(second.fromCache());
        service.aprovarReembolso(1L);
        ResumoAuditoria third = service.resumoCacheado();
        assertFalse(third.fromCache());
    }

    @Test
    void paginaInvalida() {
        assertThrows(EntradaException.class, () -> service.listarEntregas(EntregaFiltro.vazio(), 0, 5));
    }
}
