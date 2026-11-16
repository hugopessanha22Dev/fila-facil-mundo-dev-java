package com.mundodev.filafacil.inference.catalog;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InferenceCatalogTest {

    @Test
    void tarefasTemRequisitosProprios() {
        CatalogoTarefas catalogo = CatalogoTarefas.carregar();
        assertEquals(5, catalogo.todas().size());

        Tarefa classificacao = catalogo.porId("classificacao");
        assertEquals("baixo", classificacao.risco());
        assertEquals("pt-BR", classificacao.idioma());
        assertFalse(classificacao.toolUse());
        assertEquals("curta", classificacao.janela());
        assertTrue(classificacao.qualidadeMinima() > 0);
        assertTrue(classificacao.tetoCusto() > 0);
        assertTrue(classificacao.tetoLatenciaMs() > 0);

        Tarefa extracao = catalogo.porId("extracao");
        assertEquals("alto", extracao.risco());
        assertEquals("json", extracao.formato());
        assertFalse(extracao.streaming());

        Tarefa busca = catalogo.porId("busca_contextual");
        assertEquals("longa", busca.janela());
        assertTrue(busca.groundednessObrigatoria());
        assertFalse(busca.cachePermitido());

        assertTrue(catalogo.porId("resumo").streaming());
        assertTrue(catalogo.porId("resposta_assistida").toolUse());
    }

    @Test
    void modelosEscolhemBaratoOuRobustoERecusamFallbackIncompativel() {
        CatalogoModelos catalogo = CatalogoModelos.carregar();
        Modelo barato = catalogo.porId("barato");
        Modelo robusto = catalogo.porId("robusto");
        assertFalse(barato.toolUse());
        assertTrue(robusto.toolUse());
        assertTrue(SeletorModelo.rank(robusto.privacidade()) > SeletorModelo.rank(barato.privacidade()));
        for (Modelo modelo : catalogo.todos()) {
            assertTrue(modelo.qualidade() > 0);
            assertTrue(modelo.custo() > 0);
            assertTrue(modelo.latenciaMs() > 0);
            assertFalse(modelo.janela().isBlank());
            assertTrue(modelo.disponibilidade() > 0);
            assertFalse(modelo.privacidade().isBlank());
            assertFalse(modelo.idiomas().isEmpty());
            assertFalse(modelo.limitacao().isBlank());
        }

        SeletorModelo seletor = new SeletorModelo(catalogo);
        CatalogoTarefas tarefas = CatalogoTarefas.carregar();
        assertEquals("barato", seletor.escolher(tarefas.porId("classificacao")).modelo().id());
        Selecao assistida = seletor.escolher(tarefas.porId("resposta_assistida"));
        assertTrue(assistida.modelo().toolUse());
        assertEquals("robusto", seletor.escolher(tarefas.porId("extracao")).modelo().id());
        assertEquals("robusto", seletor.escolher(tarefas.porId("busca_contextual")).modelo().id());

        Selecao fallback = seletor.fallback(tarefas.porId("extracao"), "robusto");
        assertTrue(fallback.recusada());
        assertNull(fallback.modelo());
        assertTrue(fallback.motivo().contains("privacidade"));
    }

    @Test
    void versoesPromovemEVoltamSemApagarANova() throws Exception {
        for (String versao : List.of("v1", "v2")) {
            ConfigVersao config = ConfigVersao.carregar(versao);
            ParametrosTarefa extracao = config.de("extracao");
            ParametrosTarefa classificacao = config.de("classificacao");
            assertFalse(extracao.streaming());
            assertTrue(extracao.temperature() <= classificacao.temperature());
            assertTrue(config.de("resumo").streaming());
            assertTrue(config.de("resposta_assistida").streaming());
            assertFalse(config.de("busca_contextual").streaming());
            assertFalse(extracao.stop() == null);
            assertTrue(extracao.maxTokens() > 0);
            assertTrue(extracao.seed() > 0);
            assertTrue(extracao.topP() > 0);
        }

        PonteiroVersao ponteiro = PonteiroVersao.carregar();
        assertEquals(0.4, ponteiro.configAtiva().de("classificacao").temperature());
        ponteiro.promover("v2");
        assertEquals("v2", ponteiro.ativa());
        assertEquals(0.3, ponteiro.configAtiva().de("classificacao").temperature());
        ponteiro.rollback();
        assertEquals("v1", ponteiro.ativa());
        assertEquals("v2", ponteiro.anterior());
        assertTrue(ponteiro.registrada("v2"));
        assertEquals(0.4, ponteiro.configAtiva().de("classificacao").temperature());

        Path arquivo = Files.createTempDirectory("inferencia").resolve("ativa.json");
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        assertEquals(0, CatalogMain.execute(
                new String[]{"promover", "v2", "--acuracia", "0.9", "--formato", "1.0"}, arquivo, out));
        assertEquals(0, CatalogMain.execute(new String[]{"rollback"}, arquivo, out));
        PonteiroVersao gravado = PonteiroVersao.carregar(arquivo);
        assertEquals("v1", gravado.ativa());
        assertEquals("v2", gravado.anterior());
        assertTrue(gravado.registrada("v2"));
        assertTrue(Files.readString(arquivo).contains("v2"));
    }

    @Test
    void gatewayAplicaRateLimitCircuitoEJsonInvalido() {
        LimitesInferencia limites = LimitesInferencia.carregar();
        Roteiro roteiro = new Roteiro();
        GatewayResiliente gateway = novo(roteiro, new Relogio.Manual());
        for (int i = 0; i < limites.rateLimitPorTarefa(); i++) {
            ExecucaoInferencia aceita = gateway.executar("classificacao", "pedido " + i);
            assertEquals(GatewayResiliente.NOME, aceita.gateway());
            assertEquals("classificacao", aceita.tarefa());
            assertTrue(aceita.aceita());
        }
        int chamadas = roteiro.chamadas;
        ExecucaoInferencia limitada = gateway.executar("classificacao", "mais uma");
        assertEquals(chamadas, roteiro.chamadas);
        assertEquals("rate_limit", limitada.tipoFalha());
        assertFalse(limitada.aceita());
        assertNull(limitada.texto());

        Relogio.Manual relogio = new Relogio.Manual();
        Roteiro falhas = new Roteiro();
        falhas.enfileirar(falha(), falha());
        GatewayResiliente circuito = novo(falhas, relogio);
        ExecucaoInferencia abriu = circuito.executar("classificacao", "falha");
        assertFalse(abriu.aceita());
        assertEquals("ABERTO", circuito.estado());
        int antes = falhas.chamadas;
        ExecucaoInferencia bloqueada = circuito.executar("classificacao", "seguinte");
        assertEquals(antes, falhas.chamadas);
        assertEquals("circuito_aberto", bloqueada.tipoFalha());
        assertTrue(bloqueada.erro());
        relogio.avancar(limites.cooldownMs());
        ExecucaoInferencia prova = circuito.executar("classificacao", "prova");
        assertTrue(prova.aceita());
        assertEquals("FECHADO", circuito.estado());
        assertEquals(antes + 1, falhas.chamadas);

        Roteiro extracao = new Roteiro();
        extracao.enfileirar(
                new RespostaProvedor("texto solto", 5, 0.001, 2, false, "nenhuma"),
                new RespostaProvedor("{\"campo\":\"valor\"}", 5, 0.002, 4, false, "nenhuma"));
        GatewayResiliente json = novo(extracao, new Relogio.Manual());
        ExecucaoInferencia corrigida = json.executar("extracao", "nota fiscal");
        assertEquals(2, extracao.chamadas);
        assertTrue(corrigida.aceita());
        assertEquals("{\"campo\":\"valor\"}", corrigida.texto());
        assertFalse(corrigida.texto().contains("texto solto"));
        assertTrue(corrigida.fallback());
        assertEquals(GatewayResiliente.NOME, corrigida.gateway());

        Roteiro lento = new Roteiro();
        lento.enfileirar(
                new RespostaProvedor("{\"ok\":true}", 5_000, 0.001, 1, false, "nenhuma"),
                new RespostaProvedor("{\"ok\":true}", 5_000, 0.001, 1, false, "nenhuma"));
        ExecucaoInferencia estourou = novo(lento, new Relogio.Manual()).executar("extracao", "demora");
        assertFalse(estourou.aceita());
        assertNull(estourou.texto());
        assertEquals("timeout", estourou.tipoFalha());
        assertEquals(2, lento.chamadas);
    }

    @Test
    void metricasSeparamLatenciaCustoTokensErroFallbackEFalha() {
        Roteiro roteiro = new Roteiro();
        roteiro.enfileirar(
                new RespostaProvedor("texto solto", 8, 0.003, 3, false, "nenhuma"),
                new RespostaProvedor("{\"campo\":\"valor\"}", 12, 0.004, 5, false, "nenhuma"));
        GatewayResiliente gateway = novo(roteiro, new Relogio.Manual());
        gateway.executar("extracao", "documento");
        gateway.executar("classificacao", "pedido atrasado");

        ResumoMetricas extracao = gateway.resumo("extracao");
        assertEquals(20, extracao.latenciaMs());
        assertEquals(0.007, extracao.custo(), 0.000_001);
        assertEquals(8, extracao.tokens());
        assertEquals(0, extracao.erros());
        assertEquals(1, extracao.fallbacks());
        assertEquals(1, extracao.porTipoFalha().get("json_invalido"));

        ResumoMetricas classificacao = gateway.resumo("classificacao");
        assertTrue(classificacao.latenciaMs() > 0);
        assertTrue(classificacao.custo() > 0);
        assertTrue(classificacao.tokens() > 0);
        assertEquals(0, classificacao.erros());
        assertEquals(0, classificacao.fallbacks());
        assertFalse(classificacao.porTipoFalha().containsKey("json_invalido"));
        assertEquals(1, gateway.porTarefa("classificacao").size());
        assertEquals("classificacao", gateway.porTarefa("classificacao").get(0).tarefa());
    }

    @Test
    void otimizacaoTemPoliticaCacheSeletivoEPoda() {
        PoliticaOtimizacao politica = PoliticaOtimizacao.carregar();
        assertEquals(List.of("batching", "cache", "compressao", "poda"),
                politica.estrategias().stream().map(PoliticaOtimizacao.Estrategia::id).toList());
        for (PoliticaOtimizacao.Estrategia estrategia : politica.estrategias()) {
            assertFalse(estrategia.uso().isBlank());
            assertFalse(estrategia.proibicao().isBlank());
            assertFalse(estrategia.risco().isBlank());
        }

        Roteiro roteiro = new Roteiro();
        Otimizador otimizador = new Otimizador(roteiro);
        assertEquals(otimizador.classificar("pedido atrasado"), otimizador.classificar("pedido atrasado"));
        assertEquals(1, roteiro.chamadas);
        otimizador.buscar("prazo do comprovante");
        otimizador.buscar("prazo do comprovante");
        assertEquals(3, roteiro.chamadas);

        String comprimido = Otimizador.comprimir("pedido   atrasado\n\n\ninstrucao");
        assertEquals("pedido atrasado\n\ninstrucao", comprimido);
        assertTrue(comprimido.contains("instrucao"));

        String contexto = "A".repeat(40) + "miolo-descartavel" + Otimizador.MARCADOR + "miolo-descartavel" + "B".repeat(40);
        String podado = Otimizador.podar(contexto);
        assertTrue(podado.contains(Otimizador.MARCADOR));
        assertFalse(podado.contains("miolo-descartavel"));

        int antes = roteiro.chamadas;
        otimizador.classificarLote(List.of("um pedido", "outro pedido"));
        assertEquals(antes + 1, roteiro.chamadas);
    }

    @Test
    void casosAdversariaisFicamForaDoGolden() throws Exception {
        RelatorioAdversarial relatorio = new AvaliacaoAdversarial().avaliar();
        assertTrue(relatorio.foraDeEscopoMarcado());
        String texto = String.join(" ", relatorio.casos());
        assertTrue(texto.contains("ambiguo"));
        assertTrue(texto.contains("incompleto"));
        assertTrue(texto.contains("longo"));
        assertTrue(texto.contains("fora-de-escopo"));
        assertTrue(texto.contains("fora_de_escopo"));

        try (var arquivos = Files.list(Path.of("src/test/resources/inference/golden"))) {
            Set<String> nomes = arquivos
                    .map(path -> path.getFileName().toString())
                    .filter(nome -> nome.endsWith(".json"))
                    .collect(Collectors.toSet());
            assertEquals(Set.of("consulta.json", "ocorrencia.json", "reclamacao.json"), nomes);
        }
        try (var arquivos = Files.list(Path.of("src/main/resources/inference/adversarial"))) {
            long total = arquivos.filter(path -> path.getFileName().toString().endsWith(".json")).count();
            assertEquals(4, total);
        }
    }

    @Test
    void regressaoBloqueiaPromocaoAcimaDoLimite() {
        PortaoRegressao portao = PortaoRegressao.carregar();
        PonteiroVersao bloqueada = PonteiroVersao.carregar();
        RelatorioPromocao queda = portao.avaliar(bloqueada, "v2", 0.8, 1.0);
        assertFalse(queda.aceita());
        assertEquals("v1", queda.versaoAtiva());
        assertEquals("v1", bloqueada.ativa());
        assertTrue(queda.mensagem().contains("bloqueada"));
        assertTrue(bloqueada.registrada("v2"));

        PonteiroVersao aceita = PonteiroVersao.carregar();
        RelatorioPromocao dentro = portao.avaliar(aceita, "v2", 0.9, 1.0);
        assertTrue(dentro.aceita());
        assertEquals("v2", dentro.versaoAtiva());
        assertEquals("v2", aceita.ativa());
        assertTrue(dentro.mensagem().contains("aceita"));
    }

    @Test
    void readmeMenuEGoldenPermanecemNoContrato() throws Exception {
        String readme = Files.readString(Path.of("README.md"));
        assertTrue(readme.contains("classificação"));
        assertTrue(readme.contains("extração"));
        assertTrue(readme.contains("resumo"));
        assertTrue(readme.contains("resposta assistida"));
        assertTrue(readme.contains("busca contextual"));
        assertTrue(readme.contains("circuit breaker"));
        assertTrue(readme.contains("não entra no cache"));
        assertTrue(readme.contains("groundedness"));
        assertTrue(readme.contains("exec:java@inference-catalog"));

        String menu = Files.readString(Path.of("src/main/java/com/mundodev/filafacil/controller/MenuController.java"));
        assertTrue(menu.contains("case \"12\""));
        assertFalse(menu.contains("case \"13\""));
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("com.mundodev.filafacil.inference.catalog.CatalogMain"));
    }

    private static GatewayResiliente novo(Roteiro roteiro, Relogio relogio) {
        return new GatewayResiliente(roteiro, LimitesInferencia.carregar(), relogio);
    }

    private static RespostaProvedor falha() {
        return new RespostaProvedor("erro", 5, 0.001, 1, true, "provedor");
    }

    private static final class Roteiro implements ProvedorInferencia {

        private final ArrayDeque<RespostaProvedor> fila = new ArrayDeque<>();
        private int chamadas;

        void enfileirar(RespostaProvedor... respostas) {
            for (RespostaProvedor resposta : respostas) {
                fila.add(resposta);
            }
        }

        @Override
        public RespostaProvedor completar(String tarefa, String texto) {
            chamadas++;
            if (fila.isEmpty()) {
                return new RespostaProvedor("{\"ok\":true}", 5, 0.002, 4, false, "nenhuma");
            }
            return fila.remove();
        }
    }
}
