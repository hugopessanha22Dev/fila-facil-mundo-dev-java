package com.mundodev.filafacil.excecoes;

import com.mundodev.filafacil.excecoes.adapters.ExcecoesMain;
import com.mundodev.filafacil.excecoes.adapters.ExcecoesSessao;
import com.mundodev.filafacil.excecoes.adapters.PublicadorMemoria;
import com.mundodev.filafacil.excecoes.application.CicloExcecao;
import com.mundodev.filafacil.excecoes.application.RepositorioExcecoes;
import com.mundodev.filafacil.excecoes.domain.EventoExcecao;
import com.mundodev.filafacil.excecoes.domain.ExcecaoEntrega;
import com.mundodev.filafacil.excecoes.domain.OrigemExcecao;
import com.mundodev.filafacil.excecoes.domain.Severidade;
import com.mundodev.filafacil.excecoes.domain.StatusExcecao;
import com.mundodev.filafacil.excecoes.domain.TipoExcecao;
import com.mundodev.filafacil.excecoes.domain.TransicaoRecusada;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcecaoTest {

    private static final Instant ABERTURA = Instant.parse("2026-11-12T15:00:00Z");
    private static final Instant JANELA = Instant.parse("2026-11-12T18:00:00Z");

    @Test
    void aberturaCompletaRecusaDadoVazioEGeraIdsDistintos() {
        ExcecaoEntrega primeira = ExcecaoEntrega.abrir(
                "EX-1", "ENT-1", TipoExcecao.cliente_ausente, OrigemExcecao.atendimento,
                Severidade.media, "cliente nao atendeu", ABERTURA, JANELA);
        ExcecaoEntrega segunda = ExcecaoEntrega.abrir(
                "EX-2", "ENT-1", TipoExcecao.reagendamento, OrigemExcecao.operacao,
                Severidade.baixa, "pedir nova janela", ABERTURA, JANELA);
        assertEquals(StatusExcecao.aberta, primeira.status());
        assertEquals(ABERTURA, primeira.abertura());
        assertNotEquals(primeira.id(), segunda.id());
        assertThrows(TransicaoRecusada.class, () -> ExcecaoEntrega.abrir(
                "EX-3", " ", TipoExcecao.cliente_ausente, OrigemExcecao.atendimento,
                Severidade.baixa, "descricao", ABERTURA, JANELA));
        assertThrows(TransicaoRecusada.class, () -> ExcecaoEntrega.abrir(
                "EX-4", "ENT-1", TipoExcecao.cliente_ausente, OrigemExcecao.atendimento,
                Severidade.baixa, "  ", ABERTURA, JANELA));
    }

    @Test
    void transicoesRespeitamStatusSeveridadeEEvento() {
        ExcecaoEntrega aberta = exemplo(Severidade.critica);
        assertThrows(TransicaoRecusada.class, () -> aberta.resolver(ABERTURA));
        assertEquals(StatusExcecao.aberta, aberta.status());

        EventoExcecao analise = aberta.analisar();
        assertEquals(StatusExcecao.aberta, analise.anterior());
        assertEquals(StatusExcecao.em_analise, analise.novo());
        assertEquals(aberta.id(), analise.excecaoId());
        assertThrows(TransicaoRecusada.class, () -> aberta.resolver(ABERTURA));
        assertEquals(StatusExcecao.em_analise, aberta.status());

        EventoExcecao escala = aberta.escalar();
        assertEquals(StatusExcecao.em_analise, escala.anterior());
        assertEquals(StatusExcecao.escalada, escala.novo());

        ExcecaoEntrega cancelavel = exemplo(Severidade.baixa);
        EventoExcecao cancelamento = cancelavel.cancelar();
        assertEquals(StatusExcecao.aberta, cancelamento.anterior());
        assertEquals(StatusExcecao.cancelada, cancelamento.novo());
    }

    @Test
    void dominioNaoImportaInfraestrutura() throws Exception {
        try (var arquivos = Files.walk(Path.of("src/main/java/com/mundodev/filafacil/excecoes/domain"))) {
            List<Path> fontes = arquivos.filter(path -> path.toString().endsWith(".java")).toList();
            assertFalse(fontes.isEmpty());
            for (Path fonte : fontes) {
                String texto = Files.readString(fonte);
                assertFalse(texto.contains("com.fasterxml.jackson"));
                assertFalse(texto.contains("io.javalin"));
                assertFalse(texto.contains("java.nio.file"));
                assertFalse(texto.contains("java.sql"));
            }
        }
    }

    @Test
    void casosDeUsoConsultamRecusamDuplicidadeEJanela() {
        PublicadorMemoria publicador = new PublicadorMemoria();
        RepositorioAlternativo repositorio = new RepositorioAlternativo();
        CicloExcecao ciclo = new CicloExcecao(repositorio, publicador, new com.mundodev.filafacil.excecoes.adapters.GatewayContextosMemoria());
        ExcecaoEntrega aberta = ciclo.abrir(
                "EX-1", "ENT-9", TipoExcecao.fora_da_janela, OrigemExcecao.operacao,
                Severidade.baixa, "chegou depois", ABERTURA, JANELA);
        ExcecaoEntrega consultada = ciclo.consultar(aberta.id());
        assertEquals(StatusExcecao.aberta, consultada.status());
        assertEquals("ENT-9", consultada.codigoEntrega());
        assertEquals(TipoExcecao.fora_da_janela, consultada.tipo());
        assertThrows(TransicaoRecusada.class, () -> ciclo.abrir(
                "EX-2", "ENT-9", TipoExcecao.fora_da_janela, OrigemExcecao.operacao,
                Severidade.baixa, "outra", ABERTURA, JANELA));
        assertEquals(StatusExcecao.aberta, ciclo.consultar("EX-1").status());

        ciclo.analisar("EX-1");
        assertThrows(TransicaoRecusada.class, () -> ciclo.resolver("EX-1", JANELA.plusSeconds(60)));
        assertEquals(StatusExcecao.em_analise, ciclo.consultar("EX-1").status());
    }

    @Test
    void memoriaGuardaAnaliseEEvento() {
        ExcecoesSessao sessao = new ExcecoesSessao();
        ExcecaoEntrega aberta = sessao.ciclo().abrir(
                sessao.proximoId(), "ENT-3", TipoExcecao.endereco_divergente, OrigemExcecao.atendimento,
                Severidade.media, "rua trocada", ABERTURA, JANELA);
        sessao.ciclo().analisar(aberta.id());
        assertEquals(StatusExcecao.em_analise, sessao.ciclo().consultar(aberta.id()).status());
        assertTrue(sessao.publicador().publicados().stream().anyMatch(evento ->
                aberta.id().equals(evento.excecaoId())
                        && evento.anterior() == StatusExcecao.aberta
                        && evento.novo() == StatusExcecao.em_analise));
    }

    @Test
    void gatewayTraduzRotaERecusaTermoCruz() {
        ExcecoesSessao sessao = new ExcecoesSessao();
        ExcecaoEntrega traduzida = sessao.ciclo().abrirPorTermo(
                "EX-R", "route_blocked", Severidade.alta, "via interditada", "ENT-7", ABERTURA, JANELA);
        assertEquals(TipoExcecao.rota_bloqueada, traduzida.tipo());
        assertEquals(OrigemExcecao.roteirizacao, traduzida.origem());
        assertThrows(TransicaoRecusada.class, () -> sessao.ciclo().abrirPorTermo(
                "EX-X", "termo_cru", Severidade.baixa, "nada", "ENT-8", ABERTURA, JANELA));
        assertTrue(sessao.ciclo().consultar("EX-R").id().equals("EX-R"));
        assertThrows(TransicaoRecusada.class, () -> sessao.ciclo().consultar("EX-X"));
    }

    @Test
    void cliAbreConsultaERecusaResolucaoPrecoce() throws Exception {
        Path coletas = Path.of("data", "coletas.json");
        byte[] antes = Files.readAllBytes(coletas);
        ExcecoesSessao sessao = new ExcecoesSessao();
        ByteArrayOutputStream aberto = new ByteArrayOutputStream();
        assertEquals(0, ExcecoesMain.execute(new String[]{
                "abrir", "--entrega", "ENT-1", "--tipo", "cliente_ausente", "--origem", "atendimento",
                "--severidade", "critica", "--descricao", "ausente", "--janela", "2026-11-12T18:00:00Z"
        }, sessao, new PrintStream(aberto)));
        String saida = aberto.toString();
        assertTrue(saida.contains("status: aberta"));
        assertTrue(saida.contains("entrega: ENT-1"));
        assertTrue(saida.contains("tipo: cliente_ausente"));
        String id = saida.lines().filter(linha -> linha.startsWith("id: ")).findFirst().orElseThrow().substring(4);

        ByteArrayOutputStream consulta = new ByteArrayOutputStream();
        assertEquals(0, ExcecoesMain.execute(new String[]{"consultar", "--id", id}, sessao, new PrintStream(consulta)));
        assertTrue(consulta.toString().contains("status: aberta"));

        ByteArrayOutputStream recusa = new ByteArrayOutputStream();
        assertEquals(2, ExcecoesMain.execute(
                new String[]{"resolver", "--id", id, "--agora", "2026-11-12T16:00:00Z"},
                sessao, new PrintStream(recusa)));
        assertTrue(recusa.toString().contains("recusa:"));
        assertEquals(StatusExcecao.aberta, sessao.ciclo().consultar(id).status());
        assertEquals(java.util.Arrays.hashCode(antes), java.util.Arrays.hashCode(Files.readAllBytes(coletas)));
    }

    @Test
    void documentosMenuEGoldenPermanecemNoContrato() throws Exception {
        String mapa = Files.readString(Path.of("docs/excecoes/context-map.md"));
        assertTrue(mapa.contains("Gestão de exceções"));
        assertTrue(mapa.contains("Roteirização"));
        assertTrue(mapa.contains("Atendimento"));
        assertTrue(mapa.contains("Financeiro"));
        assertTrue(mapa.contains("Auditoria operacional"));
        assertTrue(mapa.contains("Núcleo"));
        assertTrue(mapa.contains("Downstream"));

        String adr = Files.readString(Path.of("docs/excecoes/adr-0001-orquestrador.md"));
        assertTrue(adr.contains("anêmico"));
        assertTrue(adr.contains("entidade"));
        assertTrue(adr.contains("vocabulário"));
        assertTrue(adr.contains("contextos"));
        assertTrue(adr.contains("abrir, analisar, escalar, resolver, cancelar e consultar"));
        assertTrue(adr.contains("adaptador"));

        String readme = Files.readString(Path.of("README.md"));
        assertTrue(readme.contains("exec:java@excecoes"));
        assertTrue(readme.contains("em memória"));
        assertTrue(readme.contains("aberta"));
        String menu = Files.readString(Path.of("src/main/java/com/mundodev/filafacil/controller/MenuController.java"));
        assertTrue(menu.contains("case \"12\""));
        assertFalse(menu.contains("case \"13\""));
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("com.mundodev.filafacil.excecoes.adapters.ExcecoesMain"));
        try (var arquivos = Files.list(Path.of("src/test/resources/inference/golden"))) {
            long total = arquivos.filter(path -> path.getFileName().toString().endsWith(".json")).count();
            assertEquals(3, total);
        }
    }

    private static ExcecaoEntrega exemplo(Severidade severidade) {
        return ExcecaoEntrega.abrir(
                "EX-" + severidade, "ENT-1", TipoExcecao.pacote_danificado, OrigemExcecao.financeiro,
                severidade, "caixa amassada", ABERTURA, JANELA);
    }

    private static final class RepositorioAlternativo implements RepositorioExcecoes {

        private final Map<String, ExcecaoEntrega> itens = new LinkedHashMap<>();

        @Override
        public void salvar(ExcecaoEntrega excecao) {
            itens.put(excecao.id(), excecao);
        }

        @Override
        public Optional<ExcecaoEntrega> porId(String id) {
            return Optional.ofNullable(itens.get(id));
        }

        @Override
        public boolean existeAberta(String codigoEntrega, TipoExcecao tipo) {
            return itens.values().stream().anyMatch(excecao ->
                    excecao.status() == StatusExcecao.aberta
                            && codigoEntrega.equals(excecao.codigoEntrega())
                            && excecao.tipo() == tipo);
        }
    }
}
