package com.mundodev.filafacil.auditoria.service;

import com.mundodev.filafacil.auditoria.model.AuditEvent;
import com.mundodev.filafacil.auditoria.model.EntregaFiltro;
import com.mundodev.filafacil.auditoria.model.EntregaRow;
import com.mundodev.filafacil.auditoria.model.PageResult;
import com.mundodev.filafacil.auditoria.model.ResumoAuditoria;
import com.mundodev.filafacil.auditoria.repo.AuditEventStore;
import com.mundodev.filafacil.auditoria.repo.CacheStore;
import com.mundodev.filafacil.auditoria.repo.EntregaQueryRepository;
import com.mundodev.filafacil.auditoria.repo.ReembolsoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AuditoriaService {

    public static final String CACHE_KEY_RESUMO = "filafacil:auditoria:resumo:v1";

    private final EntregaQueryRepository entregaQueryRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final CacheStore cacheStore;
    private final AuditEventStore auditEventStore;
    private final int cacheTtlSeconds;

    public AuditoriaService(
            EntregaQueryRepository entregaQueryRepository,
            ReembolsoRepository reembolsoRepository,
            CacheStore cacheStore,
            AuditEventStore auditEventStore,
            int cacheTtlSeconds) {
        this.entregaQueryRepository = entregaQueryRepository;
        this.reembolsoRepository = reembolsoRepository;
        this.cacheStore = cacheStore;
        this.auditEventStore = auditEventStore;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public PageResult<EntregaRow> listarEntregas(EntregaFiltro filtro, int page, int size) {
        return entregaQueryRepository.listarPaginado(filtro, page, size);
    }

    public List<EntregaRow> demoInnerJoin() {
        return entregaQueryRepository.listarComClienteMotorista();
    }

    public List<EntregaRow> demoLeftJoin() {
        return entregaQueryRepository.listarComOcorrenciaOpcional();
    }

    public List<String> demoRightJoin() {
        return entregaQueryRepository.motoristasComEntregasRightJoin();
    }

    public ResumoAuditoria resumoCacheado() {
        var cached = cacheStore.get(CACHE_KEY_RESUMO);
        if (cached.isPresent()) {
            String[] parts = cached.get().split("\\|");
            return new ResumoAuditoria(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1]),
                    Long.parseLong(parts[2]),
                    true);
        }
        ResumoAuditoria fresh = entregaQueryRepository.calcularResumo();
        cacheStore.set(
                CACHE_KEY_RESUMO,
                fresh.totalEntregas() + "|" + fresh.totalReembolsosPendentes() + "|" + fresh.totalReembolsosAprovados(),
                cacheTtlSeconds);
        return new ResumoAuditoria(
                fresh.totalEntregas(),
                fresh.totalReembolsosPendentes(),
                fresh.totalReembolsosAprovados(),
                false);
    }

    public Map<String, Object> aprovarReembolso(long id) {
        return aprovarReembolso(id, false);
    }

    public Map<String, Object> aprovarReembolso(long id, boolean failAfterLock) {
        Map<String, Object> antigo = reembolsoRepository.aprovarEmTransacao(id, failAfterLock);
        cacheStore.delete(CACHE_KEY_RESUMO);
        Map<String, Object> novo = Map.of(
                "id", antigo.get("id"),
                "status", "aprovado",
                "entregaId", antigo.get("entregaId"));
        auditEventStore.append(new AuditEvent(
                "REEMBOLSO_APROVADO",
                "terminal-local",
                "reembolso",
                String.valueOf(id),
                antigo,
                novo,
                Instant.now()));
        return novo;
    }

    public List<AuditEvent> listarEventos(int limit) {
        return auditEventStore.listRecent(limit);
    }
}
