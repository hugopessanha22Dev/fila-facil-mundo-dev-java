package com.mundodev.filafacil.service;

import com.mundodev.filafacil.model.Coleta;
import com.mundodev.filafacil.store.ColetaFileStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ColetaService {

    private static final Set<String> PRIORIDADES = Set.of("baixa", "media", "alta");
    private static final Set<String> STATUS = Set.of("pendente", "em_rota", "concluida");

    private final ColetaFileStore store;
    private final List<Coleta> coletas;

    public ColetaService(ColetaFileStore store) {
        this.store = store;
        this.coletas = store.carregar();
    }

    public Coleta cadastrar(String nome, String endereco, Integer pacotes, String prioridade) {
        validarCadastro(nome, endereco, pacotes, prioridade);
        String prioridadeNorm = normalizar(prioridade);
        if (!PRIORIDADES.contains(prioridadeNorm)) {
            throw new IllegalArgumentException("Prioridade invalida. Use: baixa, media, alta");
        }

        Coleta coleta = new Coleta(
                proximoId(),
                nome.trim(),
                endereco.trim(),
                pacotes,
                prioridadeNorm,
                "pendente");
        coletas.add(coleta);
        store.salvar(coletas);
        return coleta;
    }

    public List<Coleta> listar() {
        return List.copyOf(coletas);
    }

    public List<Coleta> filtrarPorStatus(String status) {
        String statusNorm = normalizar(status);
        if (!STATUS.contains(statusNorm)) {
            throw new IllegalArgumentException("Status invalido. Use: pendente, em_rota, concluida");
        }
        return coletas.stream()
                .filter(c -> statusNorm.equals(c.getStatus()))
                .toList();
    }

    public List<Coleta> filtrarPorPrioridade(String prioridade) {
        String prioridadeNorm = normalizar(prioridade);
        if (!PRIORIDADES.contains(prioridadeNorm)) {
            throw new IllegalArgumentException("Prioridade invalida. Use: baixa, media, alta");
        }
        return coletas.stream()
                .filter(c -> prioridadeNorm.equals(c.getPrioridade()))
                .toList();
    }

    public void atualizarStatus(Long id, String novoStatus) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id invalido.");
        }
        String statusNorm = normalizar(novoStatus);
        if (!STATUS.contains(statusNorm)) {
            throw new IllegalArgumentException("Status invalido. Use: pendente, em_rota, concluida");
        }

        Coleta encontrada = coletas.stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Coleta nao encontrada para o id " + id + "."));

        encontrada.setStatus(statusNorm);
        store.salvar(coletas);
    }

    public ResumoOperacional resumir() {
        long totalColetas = coletas.stream().count();
        int totalPacotes = coletas.stream()
                .map(Coleta::getPacotes)
                .reduce(0, Integer::sum);

        Map<String, Long> porPrioridade = PRIORIDADES.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> coletas.stream().filter(c -> p.equals(c.getPrioridade())).count(),
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<String, Long> porStatus = STATUS.stream()
                .collect(Collectors.toMap(
                        s -> s,
                        s -> coletas.stream().filter(c -> s.equals(c.getStatus())).count(),
                        (a, b) -> a,
                        LinkedHashMap::new));

        return new ResumoOperacional(totalColetas, totalPacotes, porPrioridade, porStatus);
    }

    private long proximoId() {
        return coletas.stream()
                .map(Coleta::getId)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;
    }

    private void validarCadastro(String nome, String endereco, Integer pacotes, String prioridade) {
        if (isBlank(nome)) {
            throw new IllegalArgumentException("Nome do cliente e obrigatorio.");
        }
        if (isBlank(endereco)) {
            throw new IllegalArgumentException("Endereco e obrigatorio.");
        }
        if (pacotes == null) {
            throw new IllegalArgumentException("Quantidade de pacotes e obrigatoria.");
        }
        if (pacotes <= 0) {
            throw new IllegalArgumentException("Quantidade de pacotes deve ser maior que zero.");
        }
        if (isBlank(prioridade)) {
            throw new IllegalArgumentException("Prioridade e obrigatoria.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizar(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public record ResumoOperacional(
            long totalColetas,
            int totalPacotes,
            Map<String, Long> porPrioridade,
            Map<String, Long> porStatus) {
    }
}
