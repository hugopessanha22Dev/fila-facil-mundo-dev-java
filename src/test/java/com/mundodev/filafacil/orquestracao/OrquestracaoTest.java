package com.mundodev.filafacil.orquestracao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrquestracaoTest {

    @TempDir
    Path temp;

    @Test
    void roteamentoSeparaRegistroETriagem() {
        Fluxo registro = Roteador.classificar("registrar ocorrência de endereco codigo ENT-200");
        Fluxo triagem = Roteador.classificar("triagem da ocorrencia de volume");
        assertEquals("agente", registro.id());
        assertEquals("triagem", triagem.id());
        assertEquals("classificador de ocorrencia", triagem.destino());
        assertTrue(triagem.saida().contains("categoria"));
        assertTrue(triagem.saida().contains("prioridade"));
        assertEquals("fora_escopo", Roteador.classificar("qual a previsao do tempo amanha").id());
        assertEquals("atendimento", Roteador.classificar("cliente pergunta a regra do comprovante").id());
        assertEquals("busca", Roteador.classificar("operador busca a politica de prazo").id());
        assertEquals("classificador", Roteador.classificar("classificar prioridade da entrega").id());
        assertEquals("resumo", Roteador.classificar("resumir historico operacional").id());
    }

    @Test
    void etapasBuscaRevisaoEFalhaSemDadoInventado() throws Exception {
        Orquestrador orquestrador = Orquestrador.criar(temp.resolve("sync"));
        Registro busca = orquestrador.executar("operador busca a politica de prazo");
        assertEquals(Orquestrador.ETAPAS, busca.etapas());
        assertEquals("nao_aplicada", busca.ferramenta());

        Registro revisao = orquestrador.executar("notificar entrega impacto alto codigo ENT-100 sem aprovacao");
        assertEquals("revisao_humana", revisao.status());
        assertFalse(revisao.resposta().contains("concluida"));

        Registro falha = orquestrador.executar("classificar prioridade da entrega", false, 9);
        assertEquals("falhou", falha.status());
        assertEquals("ausente", falha.resposta());
    }

    @Test
    void filaAceitaRetryTimeoutENaoAlteraColetas() throws Exception {
        Path coletas = Path.of("data", "coletas.json");
        byte[] antes = Files.readAllBytes(coletas);
        Orquestrador espera = Orquestrador.criar(temp.resolve("espera"));
        Registro embedding = espera.executar("gerar embedding do documento");
        Registro reprocessar = espera.executar("reprocessar documento operacional");
        Registro lote = espera.executar("analise em lote das entregas");
        assertEquals("aceito", embedding.status());
        assertEquals(List.of("aceito"), embedding.trilha());
        assertEquals("aceito", reprocessar.status());
        assertEquals("aceito", lote.status());
        assertFalse(Files.exists(temp.resolve("espera").resolve("trabalhos").resolve(embedding.id() + ".txt")));

        Orquestrador retry = Orquestrador.criar(temp.resolve("retry"), new RelogioPasso(), 1);
        Registro trabalho = retry.executar("gerar embedding do documento");
        retry.drenar();
        Registro concluido = retry.registro(trabalho.id());
        assertEquals("concluido", concluido.status());
        assertEquals(List.of("aceito", "processando", "concluido"), concluido.trilha());
        assertTrue(concluido.tentativas() >= 2);
        assertEquals("retry", concluido.erros());

        RelogioPasso relogio = new RelogioPasso();
        Orquestrador timeout = Orquestrador.criar(temp.resolve("timeout"), relogio, 0);
        Registro lento = timeout.executar("gerar embedding do documento");
        relogio.avancar(11);
        timeout.drenar();
        Registro esgotado = timeout.registro(lento.id());
        assertEquals("falhou", esgotado.status());
        assertEquals("timeout", esgotado.erros());
        assertFalse("processando".equals(esgotado.status()));
        assertEquals(java.util.Arrays.hashCode(antes), java.util.Arrays.hashCode(Files.readAllBytes(coletas)));
    }

    @Test
    void contratosIsolamVetorEUsamFallbackDeModelo() throws Exception {
        Contratos contratos = Contratos.carregar();
        assertEquals(8, contratos.integracoes().size());
        assertEquals(
                List.of("modelo", "vetor", "coletas", "entregas", "ocorrencias", "store", "ferramenta-externa", "fila"),
                contratos.integracoes().stream().map(Contratos.Integracao::nome).toList());
        Orquestrador orquestrador = Orquestrador.criar(temp.resolve("contratos"));
        ConsultaEndereco.Resultado primeiro = orquestrador.endereco("ENT-100");
        ConsultaEndereco.Resultado segundo = orquestrador.endereco("ENT-200");
        ConsultaEndereco.Resultado terceiro = orquestrador.endereco("ENT-300");
        assertTrue(primeiro.ok());
        assertFalse(primeiro.pagina().contains("http"));
        assertTrue(segundo.ok());
        assertEquals("limite", terceiro.erro());

        Registro semVetor = orquestrador.executar("operador busca a politica de prazo", true, 0);
        assertEquals(Orquestrador.AUSENCIA, semVetor.resposta());
        Registro fallback = orquestrador.executar("classificar prioridade da entrega", false, 1);
        assertTrue(fallback.fallback());
        assertEquals("concluido", fallback.status());
        Registro classificador = orquestrador.executar("classificar prioridade da entrega", true, 0);
        assertEquals("classificador", classificador.fluxo());
        assertEquals("concluido", classificador.status());
        assertFalse(Orquestrador.AUSENCIA.equals(classificador.resposta()));
    }

    @Test
    void registroOcultaSegredoRecusaRegraEAnexaFeedback() throws Exception {
        Orquestrador orquestrador = Orquestrador.criar(temp.resolve("registro"));
        Registro classificacao = orquestrador.executar("classificar prioridade da entrega");
        assertEquals(Orquestrador.VERSAO, classificacao.versao());
        assertEquals("ausente", classificacao.feedback());
        assertFalse(classificacao.intencao().isBlank());
        assertFalse(classificacao.modelo().isBlank());
        Registro comFeedback = orquestrador.feedback(classificacao.id(), "util");
        assertEquals("util", comFeedback.feedback());
        assertEquals(classificacao.status(), comFeedback.status());

        Registro segredo = orquestrador.executar("cliente pergunta a regra gsk_segredo123");
        assertEquals("bloqueado", segredo.status());
        assertFalse(segredo.entrada().contains("gsk_"));
        assertFalse(segredo.resposta().contains("gsk_"));
        Registro documento = orquestrador.executar("cliente 12345678901 pergunta a regra");
        assertEquals("bloqueado", documento.status());
        assertFalse(documento.entrada().contains("12345678901"));

        Registro semFonte = orquestrador.executar("operador busca a politica de prazo", true, 0);
        assertEquals(Orquestrador.AUSENCIA, semFonte.resposta());
        assertEquals("sem_evidencia", semFonte.validacao());
    }

    @Test
    void avaliacaoEDecisoes() throws Exception {
        Avaliacao.Relatorio relatorio = Avaliacao.avaliar(temp.resolve("avaliacao"));
        assertEquals(List.of(), relatorio.itens().stream().filter(item -> !item.bateu()).map(Avaliacao.Item::id).toList());
        assertTrue(relatorio.aprovado());
        assertEquals(0, relatorio.inventadas());
        assertTrue(relatorio.itens().stream().anyMatch(item -> "vetor".equals(item.id()) && !item.inventada()));
        assertTrue(relatorio.itens().stream().anyMatch(item -> "classificador-vetor".equals(item.id()) && "concluido".equals(item.status())));
        List<String> nomes = List.of(
                "adr-0001-camada-central.md",
                "adr-0002-sincrono-assincrono.md",
                "adr-0003-banco-vetorial.md",
                "adr-0004-agente-ou-fluxo.md");
        for (String nome : nomes) {
            String texto = Files.readString(Path.of("docs", "orquestracao", nome));
            assertTrue(texto.contains("Alternativa rejeitada"));
            assertTrue(texto.contains("Criterio de revisao"));
        }
        String central = Files.readString(Path.of("docs", "orquestracao", "adr-0001-camada-central.md"));
        String sincrono = Files.readString(Path.of("docs", "orquestracao", "adr-0002-sincrono-assincrono.md"));
        String vetor = Files.readString(Path.of("docs", "orquestracao", "adr-0003-banco-vetorial.md"));
        String agente = Files.readString(Path.of("docs", "orquestracao", "adr-0004-agente-ou-fluxo.md"));
        assertTrue(central.contains("chamada direta"));
        assertTrue(sincrono.contains("broker"));
        assertTrue(vetor.contains("fine-tuning"));
        assertTrue(vetor.contains("prompt stuffing"));
        assertTrue(agente.contains("agente para classificacao"));
    }

    @Test
    void cliMenuColetasEGolden() throws Exception {
        Path coletas = Path.of("data", "coletas.json");
        byte[] antes = Files.readAllBytes(coletas);
        assertEquals(0, OrquestracaoMain.execute(new String[]{"eval"}, temp.resolve("cli")));
        String relatorio = Files.readString(temp.resolve("cli").resolve("eval-report.json"));
        assertTrue(relatorio.contains("\"aprovado\" : true"));
        String menu = Files.readString(Path.of("src/main/java/com/mundodev/filafacil/controller/MenuController.java"));
        assertTrue(menu.contains("case \"12\""));
        assertFalse(menu.contains("case \"13\""));
        assertFalse(menu.contains("rag"));
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("ai-orquestracao"));
        assertTrue(pom.contains("com.mundodev.filafacil.orquestracao.OrquestracaoMain"));
        String readme = Files.readString(Path.of("README.md"));
        assertTrue(readme.contains("exec:java@ai-orquestracao"));
        assertTrue(readme.contains("Nao encontrado nos documentos."));
        try (var files = Files.list(Path.of("src/test/resources/inference/golden"))) {
            long total = files.filter(path -> path.getFileName().toString().endsWith(".json")).count();
            assertEquals(3, total);
        }
        assertEquals(java.util.Arrays.hashCode(antes), java.util.Arrays.hashCode(Files.readAllBytes(coletas)));
    }
}
