package com.mundodev.filafacil.coletas;

import com.mundodev.filafacil.coletas.factory.ColetaFactory;
import com.mundodev.filafacil.coletas.observer.ColetaEventPublisher;
import com.mundodev.filafacil.coletas.priority.ContextoPriorizacao;
import com.mundodev.filafacil.coletas.priority.PriorizacaoStrategy;
import com.mundodev.filafacil.coletas.repo.ColetaRepository;
import com.mundodev.filafacil.error.ApiException;
import com.mundodev.filafacil.error.AppException;
import com.mundodev.filafacil.error.EntradaException;
import com.mundodev.filafacil.integration.CepApiClient;
import com.mundodev.filafacil.model.EnderecoCep;
import com.mundodev.filafacil.state.AppState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Coordena o dominio de coletas. Dependencias injetadas (sem new internos).
 */
public class ColetaService {

    private static final Set<String> PRIORIDADES = Set.of("baixa", "media", "alta");
    private static final Set<String> STATUS = Set.of("pendente", "em_rota", "concluida");

    private final ColetaRepository repository;
    private final AppState state;
    private final CepApiClient cepApiClient;
    private final ColetaFactory factory;
    private final PriorizacaoStrategy priorizacaoStrategy;
    private final ColetaEventPublisher eventPublisher;

    public ColetaService(
            ColetaRepository repository,
            AppState state,
            CepApiClient cepApiClient,
            ColetaFactory factory,
            PriorizacaoStrategy priorizacaoStrategy,
            ColetaEventPublisher eventPublisher) {
        this.repository = repository;
        this.state = state;
        this.cepApiClient = cepApiClient;
        this.factory = factory;
        this.priorizacaoStrategy = priorizacaoStrategy;
        this.eventPublisher = eventPublisher;
        carregarEstadoInicial();
    }

    private void carregarEstadoInicial() {
        state.setLoading(true);
        try {
            List<Coleta> carregadas = repository.carregar();
            state.setColetas(carregadas);
            state.limparErro();
        } catch (AppException ex) {
            state.setUltimoErro(ex.getOrigem(), ex.getMessage());
            state.setColetas(List.of());
        } finally {
            state.setLoading(false);
        }
    }

    public Coleta cadastrar(String nome, String endereco, Integer pacotes, String prioridade, String cep) {
        if (isBlank(cep)) {
            throw registrarEntrada("CEP e obrigatorio.");
        }

        state.setLoading(true);
        state.limparErro();
        try {
            String prioridadeDecidida = priorizacaoStrategy.decidir(new ContextoPriorizacao(pacotes, prioridade));
            EnderecoCep enderecoCep = aguardarCep(cep);
            Coleta coleta = factory.criar(proximoId(), nome, endereco, pacotes, prioridadeDecidida, enderecoCep);

            List<Coleta> atualizadas = new ArrayList<>(state.getColetas());
            atualizadas.add(coleta);
            repository.salvar(atualizadas);
            state.setColetas(atualizadas);
            eventPublisher.notifyCriada(coleta);
            return coleta;
        } catch (AppException ex) {
            state.setUltimoErro(ex.getOrigem(), ex.getMessage());
            throw ex;
        } finally {
            state.setLoading(false);
        }
    }

    public List<Coleta> listar() {
        state.setFiltros(AppState.FiltrosAtivos.nenhum());
        return state.getColetas();
    }

    public List<Coleta> filtrarPorStatus(String status) {
        String statusNorm = normalizar(status);
        if (!STATUS.contains(statusNorm)) {
            throw registrarEntrada("Status invalido. Use: pendente, em_rota, concluida");
        }
        state.setFiltros(new AppState.FiltrosAtivos(statusNorm, null));
        state.limparErro();
        return state.getColetas().stream()
                .filter(c -> statusNorm.equals(c.getStatus()))
                .toList();
    }

    public List<Coleta> filtrarPorPrioridade(String prioridade) {
        String prioridadeNorm = normalizar(prioridade);
        if (!PRIORIDADES.contains(prioridadeNorm)) {
            throw registrarEntrada("Prioridade invalida. Use: baixa, media, alta");
        }
        state.setFiltros(new AppState.FiltrosAtivos(null, prioridadeNorm));
        state.limparErro();
        return state.getColetas().stream()
                .filter(c -> prioridadeNorm.equals(c.getPrioridade()))
                .toList();
    }

    public void atualizarStatus(Long id, String novoStatus) {
        if (id == null || id <= 0) {
            throw registrarEntrada("Id invalido.");
        }
        String statusNorm = normalizar(novoStatus);
        if (!STATUS.contains(statusNorm)) {
            throw registrarEntrada("Status invalido. Use: pendente, em_rota, concluida");
        }

        List<Coleta> atualizadas = new ArrayList<>(state.getColetas());
        Coleta encontrada = atualizadas.stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElse(null);
        if (encontrada == null) {
            throw registrarEntrada("Coleta nao encontrada para o id " + id + ".");
        }

        String anterior = encontrada.getStatus();
        encontrada.setStatus(statusNorm);
        try {
            repository.salvar(atualizadas);
            state.setColetas(atualizadas);
            state.limparErro();
            eventPublisher.notifyStatusAtualizado(encontrada, anterior, statusNorm);
        } catch (AppException ex) {
            state.setUltimoErro(ex.getOrigem(), ex.getMessage());
            throw ex;
        }
    }

    public void deletar(Long id) {
        if (id == null || id <= 0) {
            throw registrarEntrada("Id invalido.");
        }
        List<Coleta> atuais = state.getColetas();
        boolean existe = atuais.stream().anyMatch(c -> id.equals(c.getId()));
        if (!existe) {
            throw registrarEntrada("Coleta nao encontrada para o id " + id + ".");
        }
        List<Coleta> restantes = atuais.stream()
                .filter(c -> !id.equals(c.getId()))
                .toList();
        try {
            repository.salvar(new ArrayList<>(restantes));
            state.setColetas(restantes);
            state.limparErro();
        } catch (AppException ex) {
            state.setUltimoErro(ex.getOrigem(), ex.getMessage());
            throw ex;
        }
    }

    public ResumoOperacional resumir() {
        List<Coleta> coletas = state.getColetas();
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

    public AppState getState() {
        return state;
    }

    private EnderecoCep aguardarCep(String cep) {
        try {
            return cepApiClient.buscar(cep).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException("Consulta de CEP interrompida.", ex);
        } catch (ExecutionException | CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof ApiException apiEx) {
                throw apiEx;
            }
            throw new ApiException("Falha ao consultar CEP: " + cause.getMessage(), cause);
        }
    }

    private long proximoId() {
        return state.getColetas().stream()
                .map(Coleta::getId)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;
    }

    private EntradaException registrarEntrada(String mensagem) {
        state.setUltimoErro("entrada", mensagem);
        return new EntradaException(mensagem);
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
