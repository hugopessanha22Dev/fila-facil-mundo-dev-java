package com.mundodev.filafacil.auditoria.repo;

import com.mundodev.filafacil.auditoria.model.EntregaFiltro;
import com.mundodev.filafacil.auditoria.model.EntregaRow;
import com.mundodev.filafacil.auditoria.model.PageResult;
import com.mundodev.filafacil.auditoria.model.ResumoAuditoria;
import com.mundodev.filafacil.error.EntradaException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Store em memoria com seed equivalente ao SQL — para testes e modo FILAFACIL_DB_URL=memory.
 * Simula INNER/LEFT/RIGHT JOIN e transacao com rollback.
 */
public class InMemoryAuditoriaStore implements EntregaQueryRepository, ReembolsoRepository {

    private final List<EntregaRow> entregas = new ArrayList<>();
    private final Map<Long, Map<String, Object>> reembolsos = new LinkedHashMap<>();
    private final Map<Long, String> motoristas = new LinkedHashMap<>();
    private final AtomicBoolean failNextApproval = new AtomicBoolean(false);

    public InMemoryAuditoriaStore() {
        seed();
    }

    private void seed() {
        motoristas.put(1L, "Motorista Ana");
        motoristas.put(2L, "Motorista Bruno");
        motoristas.put(3L, "Motorista Carla");
        motoristas.put(4L, "Motorista sem entregas");

        entregas.add(row(1, "entregue", "2026-11-01", 10000, 1, "Cliente Alpha", 1, "Motorista Ana", null, null));
        entregas.add(row(2, "atrasada", "2026-11-02", 15000, 1, "Cliente Alpha", 2, "Motorista Bruno", "atraso", "pendente"));
        entregas.add(row(3, "danificada", "2026-11-03", 20000, 2, "Cliente Beta", 1, "Motorista Ana", "avaria", "pendente"));
        entregas.add(row(4, "entregue", "2026-11-04", 8000, 2, "Cliente Beta", 2, "Motorista Bruno", null, null));
        entregas.add(row(5, "incompleta", "2026-11-05", 12000, 3, "Cliente Gama", 3, "Motorista Carla", "faltante", "aprovado"));
        entregas.add(row(6, "entregue", "2026-11-06", 9000, 3, "Cliente Gama", 1, "Motorista Ana", null, null));
        entregas.add(row(7, "atrasada", "2026-11-07", 11000, 1, "Cliente Alpha", 3, "Motorista Carla", "atraso", "negado"));
        entregas.add(row(8, "entregue", "2026-11-08", 7000, 2, "Cliente Beta", 3, "Motorista Carla", null, null));

        putReembolso(1, 2, "pendente", "atraso", 5000);
        putReembolso(2, 3, "pendente", "danificado", 20000);
        putReembolso(3, 5, "aprovado", "incompleta", 3000);
        putReembolso(4, 7, "negado", "atraso", 2000);
    }

    private static EntregaRow row(
            long id, String status, String data, int valor,
            long clienteId, String cliente, long motoristaId, String motorista,
            String ocorrencia, String reembolso) {
        return new EntregaRow(id, status, LocalDate.parse(data), valor,
                clienteId, cliente, motoristaId, motorista, ocorrencia, reembolso);
    }

    private void putReembolso(long id, long entregaId, String status, String motivo, int valor) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("entregaId", entregaId);
        m.put("status", status);
        m.put("motivo", motivo);
        m.put("valorCentavos", valor);
        reembolsos.put(id, m);
    }

    @Override
    public PageResult<EntregaRow> listarPaginado(EntregaFiltro filtro, int page, int size) {
        if (page < 1) {
            throw new EntradaException("Pagina deve ser >= 1.");
        }
        if (size < 1) {
            throw new EntradaException("Tamanho deve ser >= 1.");
        }
        List<EntregaRow> filtered = entregas.stream()
                .filter(e -> matches(e, filtro == null ? EntregaFiltro.vazio() : filtro))
                .sorted(Comparator.comparingLong(EntregaRow::id))
                .toList();
        long total = filtered.size();
        int from = (page - 1) * size;
        if (from >= filtered.size()) {
            return new PageResult<>(List.of(), page, size, total);
        }
        int to = Math.min(from + size, filtered.size());
        return new PageResult<>(filtered.subList(from, to), page, size, total);
    }

    private boolean matches(EntregaRow e, EntregaFiltro f) {
        if (f.status() != null && !f.status().isBlank()
                && !f.status().equalsIgnoreCase(e.status())) {
            return false;
        }
        if (f.dataInicio() != null && e.dataEntrega().isBefore(f.dataInicio())) {
            return false;
        }
        if (f.dataFim() != null && e.dataEntrega().isAfter(f.dataFim())) {
            return false;
        }
        if (f.clienteId() != null && !f.clienteId().equals(e.clienteId())) {
            return false;
        }
        if (f.motoristaId() != null && !f.motoristaId().equals(e.motoristaId())) {
            return false;
        }
        if (f.temReembolso() != null) {
            boolean tem = e.reembolsoStatus() != null;
            if (f.temReembolso() != tem) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<EntregaRow> listarComClienteMotorista() {
        // INNER JOIN equivalente: todas as entregas seedadas ja tem cliente/motorista
        return List.copyOf(entregas);
    }

    @Override
    public List<EntregaRow> listarComOcorrenciaOpcional() {
        // LEFT JOIN: inclui entregas com ocorrencia null
        return List.copyOf(entregas);
    }

    @Override
    public List<String> motoristasComEntregasRightJoin() {
        // RIGHT JOIN motoristas <- entregas: inclui motorista 4 sem entrega
        Map<Long, Long> countByMotorista = entregas.stream()
                .collect(Collectors.groupingBy(EntregaRow::motoristaId, Collectors.counting()));
        List<String> out = new ArrayList<>();
        for (Map.Entry<Long, String> m : motoristas.entrySet()) {
            long cnt = countByMotorista.getOrDefault(m.getKey(), 0L);
            out.add(m.getKey() + "|" + m.getValue() + "|entregas=" + cnt);
        }
        return out;
    }

    @Override
    public ResumoAuditoria calcularResumo() {
        long total = entregas.size();
        long pend = reembolsos.values().stream().filter(r -> "pendente".equals(r.get("status"))).count();
        long apr = reembolsos.values().stream().filter(r -> "aprovado".equals(r.get("status"))).count();
        return new ResumoAuditoria(total, pend, apr, false);
    }

    @Override
    public Optional<Map<String, Object>> findById(long id) {
        Map<String, Object> r = reembolsos.get(id);
        return r == null ? Optional.empty() : Optional.of(Map.copyOf(r));
    }

    @Override
    public Map<String, Object> aprovarEmTransacao(long id, boolean failAfterLock) {
        Map<String, Object> current = reembolsos.get(id);
        if (current == null) {
            throw new EntradaException("Reembolso nao encontrado: " + id);
        }
        Map<String, Object> antigo = Map.copyOf(current);
        if (!"pendente".equals(String.valueOf(current.get("status")).toLowerCase(Locale.ROOT))) {
            throw new EntradaException("Reembolso nao esta pendente.");
        }
        boolean shouldFail = failAfterLock || failNextApproval.getAndSet(false);
        if (shouldFail) {
            // rollback: nao altera current
            throw new IllegalStateException("Falha simulada na aprovacao (rollback).");
        }
        current.put("status", "aprovado");
        long entregaId = ((Number) current.get("entregaId")).longValue();
        for (int i = 0; i < entregas.size(); i++) {
            EntregaRow e = entregas.get(i);
            if (e.id() == entregaId) {
                entregas.set(i, new EntregaRow(
                        e.id(), e.status(), e.dataEntrega(), e.valorCentavos(),
                        e.clienteId(), e.clienteNome(), e.motoristaId(), e.motoristaNome(),
                        e.ocorrenciaTipo(), "aprovado"));
            }
        }
        return antigo;
    }

    public void forceFailNextApproval() {
        failNextApproval.set(true);
    }
}
