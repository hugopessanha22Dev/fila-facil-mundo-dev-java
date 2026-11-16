package com.mundodev.filafacil.agent;

import com.mundodev.filafacil.inference.tools.MockOperationalTools;
import com.mundodev.filafacil.rag.ChunkIndex;
import com.mundodev.filafacil.rag.RetrievalConfig;
import com.mundodev.filafacil.state.AppState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentAvancadoTest {

    private static final String ATRASO = "cliente pergunta status da entrega atrasada codigo ENT-100";

    @TempDir
    Path temp;

    @Test
    void tempoMaximoParaSemNovaChamada() {
        AgentLimits limites = AgentLimits.load();
        assertEquals(8, limites.maxSteps());
        assertEquals(2, limites.maxAttemptsPerTool());
        assertEquals(8, limites.maxCalls());
        OpsWorld world = OpsWorld.demo();
        AgentLoop loop = new AgentLoop(
                new AgentTools(world, regras(), new MockOperationalTools(new AppState())),
                new AgentLimits(8, 2, 8, 10),
                new RelogioManual(0, 100));
        AgentLoop.Run run = loop.run(ATRASO);
        assertEquals("tempo", run.parada());
        assertTrue(run.acoes().isEmpty());
        assertTrue(run.trace().isEmpty());
    }

    @Test
    void permissaoConferidaEChamadaSemPermissaoNaoExecuta() {
        OpsWorld world = OpsWorld.demo();
        AgentTools tools = new AgentTools(world, regras(), new MockOperationalTools(new AppState()));
        AgentTools.ToolOutcome autorizada = tools.call("consultar_coleta", Map.of("codigo", "ENT-100"));
        assertTrue(autorizada.executada());
        assertTrue(autorizada.eventos().contains("permissao_conferida"));
        AgentTools.ToolOutcome negada = tools.call("notificar", Map.of("codigo", "ENT-100", "mensagem", "aviso"), "restrito");
        assertFalse(negada.executada());
        assertEquals("sem_permissao", negada.erro());
        assertTrue(world.notices().isEmpty());
    }

    @Test
    void entregaFilaHistoricoEPortalMantemAConsultaAtual() {
        OpsWorld world = OpsWorld.demo();
        AgentTools tools = new AgentTools(world, regras(), new MockOperationalTools(new AppState()));
        AgentTools.ToolOutcome entrega = tools.call("consultar_entrega", Map.of("codigo", "ENT-100"));
        assertEquals("atrasada", entrega.dados().get("status"));
        assertEquals("Cliente Atraso", entrega.dados().get("cliente"));
        AgentTools.ToolOutcome fila = tools.call("publicar_fila", Map.of("codigo", "ENT-100", "mensagem", "aviso"));
        assertEquals("publicada", fila.dados().get("publicacao"));
        assertEquals("fila", world.notices().get(0).resultado());
        AgentTools.ToolOutcome historico = tools.call("consultar_historico", Map.of("codigo", "ENT-100"));
        assertEquals(List.of(), historico.dados().get("eventos"));
        AgentTools.ToolOutcome portal = tools.call("consultar_portal", Map.of("codigo", "ENT-100"));
        assertEquals("portal interno ENT-100", portal.dados().get("pagina"));
        assertFalse(String.valueOf(portal.dados().get("pagina")).startsWith("http"));

        AgentLoop.Run atraso = loop(world).run(ATRASO);
        assertEquals("consultar_coleta", atraso.trace().get(0).ferramenta());
    }

    @Test
    void erroTemporarioResolveEPermanenteEscalaSemZerarFlag() {
        OpsWorld temporario = OpsWorld.demo();
        temporario.setFalhasTemporarias(1);
        AgentLoop.Run resolvido = loop(temporario).run(ATRASO);
        assertEquals("resolvido", resolvido.parada());
        assertTrue(resolvido.retries() >= 1);
        assertEquals(0, temporario.falhasTemporarias());

        OpsWorld permanente = OpsWorld.demo();
        permanente.setErroPermanente(true);
        AgentLoop.Run escalado = loop(permanente).run(ATRASO);
        assertEquals("escalado", escalado.parada());
        assertEquals(1, permanente.escalations().size());
        assertFalse(escalado.sucesso());

        OpsWorld indisponivel = OpsWorld.demo();
        indisponivel.setConsultaIndisponivel(true);
        AgentLoop mesmo = loop(indisponivel);
        mesmo.run("qual a previsao do tempo amanha");
        AgentLoop.Run segunda = mesmo.run(ATRASO);
        assertEquals("ferramenta_indisponivel", segunda.parada());
        assertTrue(indisponivel.consultaIndisponivel());
    }

    @Test
    void etapasEResumoRegistramRespostaEPendencia() {
        AgentLoop.Run atraso = loop(OpsWorld.demo()).run(ATRASO);
        assertEquals("resolvido", atraso.parada());
        assertTrue(atraso.etapas().contains("resposta final"));
        AgentLoop.Run pendente = loop(OpsWorld.demo()).run(
                "notificar entrega impacto alto codigo ENT-100 sem aprovacao");
        assertTrue(pendente.resumo().contains("pendencias: notificar"));
        assertFalse(pendente.resumo().contains("concluida"));
    }

    @Test
    void planoInseguroNaoExecutaFerramenta() {
        OpsWorld world = OpsWorld.demo();
        AgentLoop.Run run = loop(world).run("plano inseguro de alterar status");
        assertEquals("plano_bloqueado", run.parada());
        assertTrue(run.acoes().isEmpty());
        assertTrue(world.notices().isEmpty());
        assertTrue(world.occurrences().isEmpty());
    }

    @Test
    void altoImpactoEsperaAprovacaoERegistroComumSegue() {
        OpsWorld sem = OpsWorld.demo();
        AgentLoop.Run esperando = loop(sem).run("notificar entrega impacto alto codigo ENT-100 sem aprovacao");
        assertEquals("aguardando_aprovacao", esperando.parada());
        assertTrue(sem.notices().isEmpty());

        OpsWorld com = OpsWorld.demo();
        AgentLoop.Run aprovada = loop(com).run(
                "notificar entrega impacto alto codigo ENT-100 aprovacao concedida");
        assertEquals("resolvido", aprovada.parada());
        assertEquals(1, com.notices().size());

        OpsWorld comum = OpsWorld.demo();
        AgentLoop.Run registro = loop(comum).run(
                "registrar ocorrencia de endereco incorreto codigo ENT-200 tipo endereco descricao rua sem numero");
        assertEquals("resolvido", registro.parada());
        assertEquals(1, comum.notices().size());
        assertEquals("sucesso", comum.notices().get(0).resultado());
    }

    @Test
    void avaliacaoTemTrezeCenariosEMetricasDeTrajetoria() {
        AgentEval eval = new AgentEval();
        assertEquals(13, eval.scenarios().size());
        List<String> ids = eval.scenarios().stream().map(AgentEval.Scenario::id).toList();
        assertTrue(ids.containsAll(List.of(
                "sucesso", "codigo-inexistente", "ambiguo", "consulta-indisponivel", "erro-registro",
                "critico", "fora-escopo", "loop", "sem-dados-minimos",
                "erro-temporario", "erro-permanente", "plano-inseguro", "aguardando-aprovacao")));
        AgentEval.Report report = eval.evaluate(regras(), AgentLimits.load());
        assertTrue(report.aprovado());
        String texto = report.render();
        assertTrue(texto.contains("retries"));
        assertTrue(texto.contains("robustez"));
        assertTrue(texto.contains("seguranca"));
        assertTrue(texto.contains("qualidadeTrajetoria"));
        AgentEval.ScenarioResult temporario = report.cenarios().stream()
                .filter(item -> "erro-temporario".equals(item.id())).findFirst().orElseThrow();
        assertTrue(temporario.retries() >= 1);
        assertTrue(temporario.robustez());
        assertTrue(temporario.seguranca());
        assertTrue(temporario.qualidadeTrajetoria());
    }

    @Test
    void fluxoNovoNaoAlteraColetas() throws Exception {
        Path coletas = Path.of("data", "coletas.json");
        byte[] antes = Files.readAllBytes(coletas);
        loop(OpsWorld.demo()).run(ATRASO);
        assertEquals(java.util.Arrays.hashCode(antes), java.util.Arrays.hashCode(Files.readAllBytes(coletas)));
    }

    private AgentLoop loop(OpsWorld world) {
        return new AgentLoop(new AgentTools(world, regras(), new MockOperationalTools(new AppState())), AgentLimits.load());
    }

    private RagRules regras() {
        return new RagRules(new ChunkIndex(temp), RetrievalConfig.load());
    }
}
