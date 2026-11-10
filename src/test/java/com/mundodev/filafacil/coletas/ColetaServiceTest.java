package com.mundodev.filafacil.coletas;

import com.mundodev.filafacil.coletas.factory.ColetaFactory;
import com.mundodev.filafacil.coletas.observer.ColetaEventPublisher;
import com.mundodev.filafacil.coletas.observer.ColetaObserver;
import com.mundodev.filafacil.coletas.observer.ContadorMemoriaObserver;
import com.mundodev.filafacil.coletas.priority.ContextoPriorizacao;
import com.mundodev.filafacil.coletas.priority.PrioridadeManualStrategy;
import com.mundodev.filafacil.coletas.priority.PrioridadePorPacotesStrategy;
import com.mundodev.filafacil.coletas.priority.PriorizacaoStrategy;
import com.mundodev.filafacil.coletas.repo.InMemoryColetaRepository;
import com.mundodev.filafacil.error.ApiException;
import com.mundodev.filafacil.error.EntradaException;
import com.mundodev.filafacil.integration.CepApiClient;
import com.mundodev.filafacil.model.EnderecoCep;
import com.mundodev.filafacil.state.AppState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColetaServiceTest {

    private AppState state;
    private FakeCepApiClient cepClient;
    private ContadorMemoriaObserver contador;
    private RecordingObserver recording;
    private InMemoryColetaRepository repository;

    @BeforeEach
    void setUp() {
        state = new AppState();
        cepClient = new FakeCepApiClient();
        contador = new ContadorMemoriaObserver();
        recording = new RecordingObserver();
        repository = new InMemoryColetaRepository();
    }

    private ColetaService serviceCom(PriorizacaoStrategy strategy) {
        return new ColetaService(
                repository,
                state,
                cepClient,
                new ColetaFactory(),
                strategy,
                new ColetaEventPublisher(List.of(contador, recording)));
    }

    @Test
    void cadastrarRejeitaCampoObrigatorioVazio() {
        ColetaService service = serviceCom(new PrioridadeManualStrategy());
        assertThrows(EntradaException.class,
                () -> service.cadastrar("", "Rua A", 1, "alta", "01001000"));
        assertEquals("entrada", state.getUltimoErro().origem());
        assertTrue(state.getColetas().isEmpty());
    }

    @Test
    void cadastrarEnriqueceComCepQuandoApiOk() {
        ColetaService service = serviceCom(new PrioridadeManualStrategy());
        cepClient.sucesso = true;
        Coleta coleta = service.cadastrar("Ana", "Rua A", 2, "alta", "01001000");

        assertEquals(1L, coleta.getId());
        assertEquals("pendente", coleta.getStatus());
        assertEquals("01001000", coleta.getCep());
        assertEquals(1, state.getColetas().size());
        assertNull(state.getUltimoErro());
        assertFalse(state.isLoading());
        assertEquals(1, contador.getCriadas());
        assertEquals(1, recording.criadas.get());
    }

    @Test
    void cadastrarNaoPersisteQuandoApiFalha() {
        ColetaService service = serviceCom(new PrioridadeManualStrategy());
        cepClient.sucesso = false;
        assertThrows(ApiException.class,
                () -> service.cadastrar("Ana", "Rua A", 2, "alta", "00000000"));
        assertTrue(state.getColetas().isEmpty());
        assertEquals("api", state.getUltimoErro().origem());
        assertEquals(0, contador.getCriadas());
    }

    @Test
    void trocaDeStrategyAlteraPrioridadeSemMudarService() {
        cepClient.sucesso = true;
        ColetaService manual = serviceCom(new PrioridadeManualStrategy());
        Coleta c1 = manual.cadastrar("Ana", "Rua A", 12, "baixa", "01001000");
        assertEquals("baixa", c1.getPrioridade());

        state = new AppState();
        repository = new InMemoryColetaRepository();
        contador.reset();
        recording = new RecordingObserver();
        ColetaService porPacotes = serviceCom(new PrioridadePorPacotesStrategy());
        Coleta c2 = porPacotes.cadastrar("Ana", "Rua A", 12, "baixa", "01001000");
        assertEquals("alta", c2.getPrioridade());
    }

    @Test
    void strategiesIsoladas() {
        assertEquals("alta", new PrioridadePorPacotesStrategy().decidir(new ContextoPriorizacao(10, null)));
        assertEquals("media", new PrioridadePorPacotesStrategy().decidir(new ContextoPriorizacao(5, null)));
        assertEquals("baixa", new PrioridadePorPacotesStrategy().decidir(new ContextoPriorizacao(2, null)));
        assertEquals("alta", new PrioridadeManualStrategy().decidir(new ContextoPriorizacao(2, "alta")));
    }

    @Test
    void factoryRejeitaDadosInvalidos() {
        ColetaFactory factory = new ColetaFactory();
        EnderecoCep cep = new EnderecoCep();
        cep.setCep("01001000");
        assertThrows(EntradaException.class, () -> factory.criar(1, "", "Rua", 1, "alta", cep));
    }

    @Test
    void atualizarStatusNotificaObservers() {
        ColetaService service = serviceCom(new PrioridadeManualStrategy());
        cepClient.sucesso = true;
        Coleta criada = service.cadastrar("Ana", "Rua A", 2, "alta", "01001000");
        service.atualizarStatus(criada.getId(), "em_rota");
        assertEquals(1, contador.getStatusAtualizados());
        assertEquals(1, recording.status.get());
        assertEquals("em_rota", state.getColetas().get(0).getStatus());
    }

    @Test
    void deletarRemoveDaListagem() {
        ColetaService service = serviceCom(new PrioridadeManualStrategy());
        cepClient.sucesso = true;
        Coleta criada = service.cadastrar("Ana", "Rua A", 2, "alta", "01001000");
        service.deletar(criada.getId());
        assertTrue(state.getColetas().isEmpty());
        assertTrue(repository.carregar().isEmpty());
    }

    @Test
    void filtrarEResumir() {
        ColetaService service = serviceCom(new PrioridadeManualStrategy());
        cepClient.sucesso = true;
        service.cadastrar("Ana", "Rua A", 2, "alta", "01001000");
        service.cadastrar("Bob", "Rua B", 3, "baixa", "01001000");

        assertEquals(2, service.filtrarPorStatus("pendente").size());
        assertEquals(1, service.filtrarPorPrioridade("alta").size());
        ColetaService.ResumoOperacional resumo = service.resumir();
        assertEquals(2, resumo.totalColetas());
        assertEquals(5, resumo.totalPacotes());
        assertEquals(1L, resumo.porPrioridade().get("alta"));
    }

    @Test
    void serviceNaoInstanciaDependencias_wiringExternoComFakes() {
        ColetaService service = serviceCom(new PrioridadeManualStrategy());
        assertNotNull(service);
        assertFalse(state.isLoading());
    }

    private static final class RecordingObserver implements ColetaObserver {
        final AtomicInteger criadas = new AtomicInteger();
        final AtomicInteger status = new AtomicInteger();

        @Override
        public void onColetaCriada(Coleta coleta) {
            criadas.incrementAndGet();
        }

        @Override
        public void onStatusAtualizado(Coleta coleta, String statusAnterior, String statusNovo) {
            status.incrementAndGet();
        }
    }

    private static final class FakeCepApiClient implements CepApiClient {
        boolean sucesso = true;

        @Override
        public CompletableFuture<EnderecoCep> buscar(String cep) {
            if (!sucesso) {
                return CompletableFuture.failedFuture(new ApiException("CEP nao encontrado (fake)"));
            }
            EnderecoCep endereco = new EnderecoCep();
            endereco.setCep(cep.replaceAll("\\D", ""));
            endereco.setBairro("Se");
            endereco.setLocalidade("Sao Paulo");
            endereco.setUf("SP");
            return CompletableFuture.completedFuture(endereco);
        }
    }
}
