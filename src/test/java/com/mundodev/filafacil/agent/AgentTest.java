package com.mundodev.filafacil.agent;

import com.mundodev.filafacil.inference.tools.MockOperationalTools;
import com.mundodev.filafacil.rag.ChunkIndex;
import com.mundodev.filafacil.rag.CorpusCatalog;
import com.mundodev.filafacil.rag.RetrievalConfig;
import com.mundodev.filafacil.rag.SourceDocument;
import com.mundodev.filafacil.state.AppState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTest {

    @TempDir
    Path temp;

    @Test
    void cartaAceitaLogisticaRecusaForaEParaSemDadoOuNoTeto() throws Exception {
        AgentLoop agent = newLoop(AgentLimits.load());
        AgentLoop.Run aceito = agent.run("cliente pergunta status da entrega atrasada codigo ENT-100");
        assertEquals("resolvido", aceito.parada());
        assertFalse(aceito.acoes().isEmpty());
        assertTrue(aceito.acoes().stream().allMatch(AgentTools.PERMITIDAS::contains));

        AgentLoop.Run fora = newLoop(AgentLimits.load()).run("qual a previsao do tempo amanha");
        assertEquals("fora_dominio", fora.parada());
        assertTrue(fora.acoes().isEmpty());

        OpsWorld semDado = OpsWorld.demo();
        AgentLoop.Run ausente = new AgentLoop(new AgentTools(semDado, rules(), new MockOperationalTools(new AppState())), AgentLimits.load())
                .run("quero o status da entrega atrasada");
        assertEquals("dado_ausente", ausente.parada());
        assertTrue(semDado.occurrences().isEmpty());
        assertTrue(semDado.notices().isEmpty());

        AgentLoop.Run teto = newLoop(new AgentLimits(1, 2, 8))
                .run("cliente pergunta status da entrega atrasada codigo ENT-100");
        assertEquals("limite", teto.parada());
        assertEquals(1, teto.acoes().size());
    }

    @Test
    void schemasRecusamDadoIncompletoEDevolvemConsultaEscala() throws Exception {
        OpsWorld world = OpsWorld.demo();
        AgentTools tools = new AgentTools(world, rules(), new MockOperationalTools(new AppState()));

        AgentTools.ToolOutcome existe = tools.call("consultar_coleta", Map.of("codigo", "ENT-100"));
        assertEquals(true, existe.dados().get("encontrada"));
        assertEquals("atrasada", existe.dados().get("status"));
        assertEquals("Cliente Atraso", existe.dados().get("cliente"));

        AgentTools.ToolOutcome ausente = tools.call("consultar_coleta", Map.of("codigo", "ENT-999"));
        assertEquals(false, ausente.dados().get("encontrada"));
        assertEquals(AgentTools.AUSENTE, ausente.dados().get("status"));
        assertEquals(AgentTools.AUSENTE, ausente.dados().get("cliente"));

        AgentTools.ToolOutcome semDescricao = tools.call("registrar_ocorrencia", Map.of(
                "codigo", "ENT-200", "tipo", "endereco", "descricao", ""));
        assertEquals("argumento_invalido", semDescricao.erro());
        assertTrue(world.occurrences().isEmpty());

        AgentTools.ToolOutcome semCodigo = tools.call("notificar", Map.of("codigo", "", "mensagem", "aviso"));
        assertEquals("argumento_invalido", semCodigo.erro());
        assertTrue(world.notices().isEmpty());

        AgentTools.ToolOutcome escala = tools.call("escalar_humano", Map.of(
                "motivo", "risco operacional",
                "codigo", "ENT-100",
                "estado", "critico",
                "dados", "status=atrasada; cliente=Cliente Atraso"));
        assertTrue(escala.ok());
        OpsWorld.Escalation registro = world.escalations().get(0);
        assertEquals("risco operacional", registro.motivo());
        assertEquals("critico", registro.estado());
        assertTrue(registro.dados().contains("Cliente Atraso"));
    }

    @Test
    void falhaDeConsultaNaoEncontraEFalhaDeEscritaNaoGrava() throws Exception {
        OpsWorld indisponivel = OpsWorld.demo();
        indisponivel.setConsultaIndisponivel(true);
        AgentTools consulta = new AgentTools(indisponivel, rules(), new MockOperationalTools(new AppState()));
        AgentTools.ToolOutcome fora = consulta.call("consultar_coleta", Map.of("codigo", "ENT-100"));
        assertEquals("indisponivel", fora.erro());
        assertEquals(false, fora.dados().get("encontrada"));

        OpsWorld escrita = OpsWorld.demo();
        escrita.setErroEscrita(true);
        AgentTools registro = new AgentTools(escrita, rules(), new MockOperationalTools(new AppState()));
        AgentTools.ToolOutcome erro = registro.call("registrar_ocorrencia", Map.of(
                "codigo", "ENT-200", "tipo", "endereco", "descricao", "rua sem numero"));
        assertEquals("erro_escrita", erro.erro());
        assertTrue(escrita.occurrences().isEmpty());
    }

    @Test
    void filaGuardaNotificacaoERegraEntraNoTrace() throws Exception {
        OpsWorld world = OpsWorld.demo();
        AgentLoop loop = new AgentLoop(new AgentTools(world, rules(), new MockOperationalTools(new AppState())), AgentLimits.load());
        AgentLoop.Run run = loop.run(
                "motorista informa falha operacional na coleta codigo COL-300 tipo coleta descricao cliente ausente na janela");
        assertEquals("resolvido", run.parada());
        assertEquals(1, world.notices().size());
        OpsWorld.Notice notice = world.notices().get(0);
        assertTrue(notice.payload().contains("COL-300"));
        assertTrue(notice.instante() > 0);
        assertEquals("sucesso", notice.resultado());

        AgentLoop.Run regra = newLoop(AgentLimits.load()).run("cliente pergunta status da entrega atrasada codigo ENT-100");
        assertTrue(regra.trace().stream().anyMatch(step ->
                "consultar_regras".equals(step.ferramenta()) && step.resultado().contains("procedimento-atraso | v1 | aviso")));
        assertTrue(regra.dados().get("regra").contains("procedimento-atraso"));
    }

    @Test
    void adaptadorNaoAlteraContratoNemColetasJson() throws Exception {
        Path coletas = Path.of("data", "coletas.json");
        byte[] antes = Files.readAllBytes(coletas);
        AppState state = new AppState();
        state.addColeta(new com.mundodev.filafacil.coletas.Coleta(1L, "Cliente Modulo2", "rua", 1, "baixa", "pendente"));
        MockOperationalTools triage = new MockOperationalTools(state);
        AgentTools tools = new AgentTools(OpsWorld.demo(), rules(), triage);
        AgentTools.ToolOutcome consulta = tools.call("consultar_coleta", Map.of("codigo", "1"));
        assertEquals(true, consulta.dados().get("encontrada"));
        assertEquals("pendente", consulta.dados().get("status"));
        assertEquals("alta", tools.call("calcular_prioridade", Map.of("tipo", "reclamacao", "sentimento", "")).dados().get("prioridade"));
        newLoop(AgentLimits.load()).run("cliente pergunta status da entrega atrasada codigo ENT-100");
        assertEquals(java.util.Arrays.hashCode(antes), java.util.Arrays.hashCode(Files.readAllBytes(coletas)));
    }

    @Test
    void consultaAconteceAntesDoRegistroENaoInventaCliente() throws Exception {
        OpsWorld world = OpsWorld.demo();
        AgentLoop.Run run = new AgentLoop(
                new AgentTools(world, rules(), new MockOperationalTools(new AppState())), AgentLimits.load())
                .run("registrar ocorrencia de endereco incorreto codigo ENT-200 tipo endereco descricao rua sem numero");
        int consulta = indexOf(run, "consultar_coleta");
        int registro = indexOf(run, "registrar_ocorrencia");
        assertTrue(consulta >= 0 && consulta < registro);
        assertEquals(1, world.occurrences().size());
        AgentLoop.Step gravado = run.trace().get(registro);
        assertTrue(gravado.eventos().indexOf("confirmado") < gravado.eventos().indexOf("gravado"));

        AgentLoop.Run inexistente = newLoop(AgentLimits.load()).run("status da entrega atrasada codigo ENT-999");
        assertEquals("nao_encontrado", inexistente.parada());
        assertEquals(AgentTools.AUSENTE, inexistente.dados().get("cliente"));
        assertEquals(AgentTools.AUSENTE, inexistente.dados().get("status"));
        assertFalse(inexistente.sucesso());
    }

    @Test
    void guardrailsBloqueiamRegistroIncompletoFerramentaProibidaFalhaRepetidaELoop() throws Exception {
        OpsWorld incompleto = OpsWorld.demo();
        AgentLoop.Run semDescricao = new AgentLoop(
                new AgentTools(incompleto, rules(), new MockOperationalTools(new AppState())), AgentLimits.load())
                .run("registrar ocorrencia codigo ENT-200 tipo endereco");
        assertEquals("argumento_invalido", semDescricao.parada());
        assertTrue(incompleto.occurrences().isEmpty());

        AgentLoop.Run proibida = newLoop(AgentLimits.load()).run("usar ferramenta apagar_banco na entrega");
        assertEquals("fora_escopo", proibida.parada());
        assertTrue(proibida.acoes().isEmpty());

        OpsWorld queda = OpsWorld.demo();
        queda.setConsultaIndisponivel(true);
        AgentLoop.Run repetida = new AgentLoop(
                new AgentTools(queda, rules(), new MockOperationalTools(new AppState())), AgentLimits.load())
                .run("status da entrega atrasada codigo ENT-100");
        assertEquals("ferramenta_indisponivel", repetida.parada());
        assertEquals(2, repetida.acoes().size());
        assertTrue(repetida.acoes().stream().allMatch("consultar_coleta"::equals));

        AgentLoop.Run repeticao = newLoop(AgentLimits.load()).run("repetir consulta da entrega codigo ENT-100");
        assertEquals("loop", repeticao.parada());
        assertEquals(1, repeticao.acoes().size());
        assertEquals("parar:loop", repeticao.trace().get(repeticao.trace().size() - 1).decisao());
    }

    @Test
    void politicaCobreAtrasoEnderecoColetaECritico() throws Exception {
        AgentLoop.Run atraso = newLoop(AgentLimits.load()).run("cliente pergunta status da entrega atrasada codigo ENT-100");
        assertEquals("resolvido", atraso.status());
        assertEquals("atrasada", atraso.dados().get("status"));
        assertEquals("Cliente Atraso", atraso.dados().get("cliente"));
        assertTrue(atraso.proximaEtapa().contains("regra"));

        OpsWorld endereco = OpsWorld.demo();
        AgentLoop.Run enderecoRun = new AgentLoop(
                new AgentTools(endereco, rules(), new MockOperationalTools(new AppState())), AgentLimits.load())
                .run("registrar ocorrencia de endereco incorreto codigo ENT-200 tipo endereco descricao rua sem numero");
        assertEquals("resolvido", enderecoRun.parada());
        assertEquals("registrada", enderecoRun.dados().get("ocorrencia"));
        assertEquals("enfileirada", enderecoRun.dados().get("notificacao"));

        AgentLoop.Run coleta = newLoop(AgentLimits.load()).run(
                "motorista informa falha operacional na coleta codigo COL-300 tipo coleta descricao cliente ausente na janela");
        assertEquals("resolvido", coleta.parada());
        assertEquals("falha", coleta.dados().get("status"));

        OpsWorld critico = OpsWorld.demo();
        AgentLoop.Run criticoRun = new AgentLoop(
                new AgentTools(critico, rules(), new MockOperationalTools(new AppState())), AgentLimits.load())
                .run("ocorrencia critica com risco operacional codigo ENT-100");
        assertEquals("escalado", criticoRun.parada());
        assertEquals(1, critico.escalations().size());
        assertTrue(critico.escalations().get(0).dados().contains("atrasada"));
        assertTrue(critico.occurrences().isEmpty());
        assertEquals(1, criticoRun.escalonamentos());
    }

    @Test
    void avaliacaoCobreOsNoveCenariosEAsOitoMetricas() throws Exception {
        AgentEval eval = new AgentEval();
        assertEquals(9, eval.scenarios().size());
        AgentEval.Report report = eval.evaluate(rules(), AgentLimits.load());
        assertTrue(report.aprovado());
        assertEquals(9, report.cenarios().size());
        String rendered = report.render();
        assertTrue(rendered.contains("sucesso"));
        assertTrue(rendered.contains("custoEstimado"));
        assertTrue(rendered.contains("latencia"));
        assertTrue(rendered.contains("passos"));
        assertTrue(rendered.contains("ferramentas"));
        assertTrue(rendered.contains("falhas"));
        assertTrue(rendered.contains("escalonamentos"));
        assertTrue(rendered.contains("aderencia"));
        assertTrue(rendered.contains("total=9"));
        AgentEval.ScenarioResult sucesso = report.cenarios().get(0);
        assertTrue(sucesso.sucesso());
        assertFalse(sucesso.ferramentas().isEmpty());
        AgentEval.ScenarioResult fora = report.cenarios().stream()
                .filter(item -> "fora-escopo".equals(item.id())).findFirst().orElseThrow();
        assertTrue(fora.ferramentas().isEmpty());
        assertTrue(fora.aderencia());
        AgentEval.ScenarioResult semDados = report.cenarios().stream()
                .filter(item -> "sem-dados-minimos".equals(item.id())).findFirst().orElseThrow();
        assertEquals("argumento_invalido", semDados.parada());
        assertFalse(report.cenarios().stream().filter(item -> "codigo-inexistente".equals(item.id())).findFirst().orElseThrow().sucesso());
    }

    @Test
    void cliEvalEMenuPermanecemNoContrato() throws Exception {
        assertEquals(0, AgentMain.execute(new String[]{
                "run", "--text", "cliente", "pergunta", "status", "da", "entrega", "atrasada", "codigo", "ENT-100"
        }, temp, temp));
        String run = Files.readString(temp.resolve("last-run.txt"));
        assertTrue(run.contains("resolvido"));
        assertTrue(run.contains("Cliente Atraso"));
        assertEquals(0, AgentMain.execute(new String[]{"eval"}, temp, temp));
        String report = Files.readString(temp.resolve("eval-report.json"));
        assertTrue(report.contains("\"aprovado\" : true"));
        String menu = Files.readString(Path.of("src/main/java/com/mundodev/filafacil/controller/MenuController.java"));
        assertTrue(menu.contains("case \"12\""));
        assertFalse(menu.contains("case \"13\""));
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("com.mundodev.filafacil.agent.AgentMain"));
        String readme = Files.readString(Path.of("README.md"));
        assertTrue(readme.contains("exec:java@agent"));
        assertTrue(readme.contains("fora_dominio"));
        try (var files = Files.list(Path.of("src/test/resources/inference/golden"))) {
            long total = files.filter(path -> path.getFileName().toString().endsWith(".json")).count();
            assertEquals(3, total);
        }
    }

    private AgentLoop newLoop(AgentLimits limits) throws Exception {
        return AgentLoop.create(rules(), limits);
    }

    private RagRules rules() throws Exception {
        ChunkIndex index = new ChunkIndex(temp);
        RetrievalConfig config = RetrievalConfig.load();
        for (SourceDocument doc : new CorpusCatalog().load()) {
            index.publish(doc, config);
        }
        return new RagRules(index, config);
    }

    private static int indexOf(AgentLoop.Run run, String tool) {
        for (int i = 0; i < run.trace().size(); i++) {
            if (tool.equals(run.trace().get(i).ferramenta())) {
                return i;
            }
        }
        return -1;
    }
}
