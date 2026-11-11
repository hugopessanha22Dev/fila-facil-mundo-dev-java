package com.mundodev.filafacil.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagTest {

    private static final String MARKER = "prazo do comprovante digital ficticio e de 48 horas";

    private final CorpusCatalog catalog = new CorpusCatalog();
    private final RetrievalConfig config = RetrievalConfig.load();
    private final Chunker chunker = new Chunker();
    private final Retriever retriever = new Retriever();

    @TempDir
    Path temp;

    @Test
    void corpusTemSeteFamiliasERejeitaDesconhecida() {
        Set<String> dominios = catalog.load().stream().map(SourceDocument::dominio).collect(Collectors.toSet());
        assertEquals(CorpusCatalog.DOMINIOS, dominios);
        assertEquals(7, catalog.load().size());
        assertThrows(IllegalArgumentException.class, () -> catalog.parse("{\"dominio\":\"financeiro\",\"secoes\":[]}"));
    }

    @Test
    void chunkDescartaVazioDeduplicaEGuardaOrigem() {
        SourceDocument base = catalog.find("regras-comprovantes");
        SourceDocument.Section prazo = base.secoes().get(0);
        SourceDocument doc = new SourceDocument(
                base.id(), base.titulo(), base.tipo(), base.dominio(), base.versao(),
                base.dataAtualizacao(), base.owner(), base.acesso(),
                List.of(new SourceDocument.Section("vazia", "   "), prazo, prazo));
        List<KnowledgeChunk> chunks = chunker.chunk(doc, config.chunkSize(), config.overlap());
        assertEquals(1, chunks.size());
        KnowledgeChunk chunk = chunks.get(0);
        assertEquals(base.id(), chunk.docId());
        assertEquals(base.versao(), chunk.versao());
        assertEquals("prazo", chunk.secao());
        assertEquals(base.acesso(), chunk.acesso());
        assertTrue(chunk.texto().contains(MARKER));
        assertTrue(chunker.chunk(base, 30, 0).stream().noneMatch(item -> item.texto().contains(MARKER)));
    }

    @Test
    void versaoNovaTrocaSoODocumentoPublicado() throws Exception {
        ChunkIndex index = ingest();
        byte[] coletaAntes = Files.readAllBytes(index.namespaceFile("coletas"));
        double[] vetorAntes = index.vectorOf("politica-coletas").clone();
        SourceDocument v2 = catalog.find("politica-entregas").withVersao(
                "v2", "A entrega ficticia tem prazo de 48 horas. entrega prazo entrega prazo.");
        index.publish(v2, config);
        assertArrayEquals(vetorAntes, index.vectorOf("politica-coletas"));
        assertArrayEquals(coletaAntes, Files.readAllBytes(index.namespaceFile("coletas")));
        RagAnswer answer = new RagAssistant(temp).ask(
                index, "op", "operador", "qual o prazo da entrega", 1, config);
        assertEquals("v2", answer.versao());
        assertTrue(answer.evidencia().contains("48"));
        assertFalse(answer.evidencia().contains("24"));
    }

    @Test
    void filtrosDeAcessoDominioETopK() {
        KnowledgeChunk operador = chunk("faq", "atendimento", "operador", "escala", "supervisor ficticio operador");
        KnowledgeChunk entrega = chunk("politica-entregas", "entregas", "publico", "prazo", "entrega prazo entrega");
        KnowledgeChunk coleta = chunk("politica-coletas", "coletas", "publico", "janela", "entrega prazo entrega");
        List<KnowledgeChunk> chunks = List.of(operador, entrega, coleta);

        assertTrue(retriever.search(chunks, "supervisor ficticio", Retriever.SearchFilter.of("parceiro", null), config).isEmpty());
        assertEquals("faq", retriever.search(chunks, "supervisor ficticio", Retriever.SearchFilter.of("operador", null), config).get(0).chunk().docId());

        List<Retriever.Hit> soEntrega = retriever.search(
                chunks, "entrega prazo", Retriever.SearchFilter.of("operador", "entregas"), config);
        assertFalse(soEntrega.isEmpty());
        assertTrue(soEntrega.stream().allMatch(hit -> "entregas".equals(hit.chunk().dominio())));

        List<Retriever.Hit> um = retriever.search(
                chunks, "entrega prazo", Retriever.SearchFilter.of("operador", null), config.withTopK(1));
        assertEquals(1, um.size());
    }

    @Test
    void hibridoRecuperaTermoERerankSobeASecao() {
        KnowledgeChunk raro = chunk("doc", "comprovantes", "publico", "outro", "zztermo9 alpha beta gamma");
        RetrievalConfig vetor = config.withHybridWeight(0).withRerank(false).withCutoff(0.55);
        assertTrue(retriever.search(List.of(raro), "zztermo9", Retriever.SearchFilter.of("operador", null), vetor).isEmpty());
        RetrievalConfig hibrido = vetor.withHybridWeight(0.35);
        assertEquals("doc", retriever.search(List.of(raro), "zztermo9", Retriever.SearchFilter.of("operador", null), hibrido).get(0).chunk().docId());

        KnowledgeChunk errada = chunk("errado", "entregas", "publico", "prazo geral", "comprovante digital urgente");
        KnowledgeChunk certa = chunk("certo", "comprovantes", "publico", "comprovante", "comprovante alpha beta gamma delta epsilon");
        RetrievalConfig semRerank = config.withHybridWeight(0).withRerank(false).withCutoff(0.2).withTopK(2);
        assertEquals("errado", retriever.search(List.of(errada, certa), "comprovante digital urgente", Retriever.SearchFilter.of("operador", null), semRerank).get(0).chunk().docId());
        RetrievalConfig comRerank = semRerank.withRerank(true);
        assertEquals("certo", retriever.search(List.of(errada, certa), "comprovante digital urgente", Retriever.SearchFilter.of("operador", null), comRerank).get(0).chunk().docId());
    }

    @Test
    void respostaCitaERecusaQuandoNaoHaFonte() throws Exception {
        ChunkIndex index = ingest();
        RagAssistant assistant = new RagAssistant(temp);
        RagAnswer cita = assistant.ask(index, "s", "operador", "qual o prazo do comprovante digital", 1, config);
        assertTrue(cita.found());
        assertEquals("regras-comprovantes", cita.docId());
        assertEquals("v1", cita.versao());
        assertEquals("prazo", cita.secao());
        assertTrue(cita.render().contains("evidencia:"));
        assertTrue(cita.render().contains("orientacao:"));
        assertTrue(cita.render().contains("citacao: regras-comprovantes | v1 | prazo"));
        assertEquals("comprovantes", cita.dominioUsado());

        RagAnswer ausente = assistant.ask(
                index, "s2", "operador", "qual o desconto de combustivel da frota zzinexistente", 2, config);
        assertFalse(ausente.found());
        assertEquals("", ausente.docId());
        assertEquals("Nao encontrado nos documentos.", ausente.render());
    }

    @Test
    void memoriaIsolaSessaoExpiraENaoViraEvidencia() throws Exception {
        ChunkIndex index = ingest();
        RagAssistant assistant = new RagAssistant(temp);
        SessionMemory memory = new SessionMemory(temp);
        assistant.ask(index, "op-a", "operador", "janela da coleta", 0, config);
        assistant.ask(index, "op-b", "operador", "prazo da entrega", 0, config);
        String arquivoA = Files.readString(memory.file("op-a"));
        String arquivoB = Files.readString(memory.file("op-b"));
        assertTrue(arquivoA.contains("coletas"));
        assertTrue(arquivoB.contains("entregas"));
        assertFalse(arquivoA.contains("entregas"));

        memory.save(new SessionMemory.State("op-m", "resumo", "coletas", List.of("regra secreta 99 horas"), 0));
        RagAnswer semFonte = assistant.ask(index, "op-m", "operador", "regra secreta 99 horas", 1_000, config);
        assertFalse(semFonte.found());
        assertFalse(semFonte.render().contains("99"));

        assistant.ask(index, "op-t", "operador", "janela da coleta", 0, config);
        long depois = config.sessionTtlMinutes() * 60_000L + 5;
        RagAnswer expirada = assistant.ask(index, "op-t", "operador", "qual a regra", depois, config);
        assertNull(expirada.dominioUsado());

        assistant.ask(index, "op-f", "operador", "prazo do comprovante digital", 0, config);
        RagAnswer seguida = assistant.ask(index, "op-f", "operador", "qual o prazo", 1_000, config);
        assertEquals("comprovantes", seguida.dominioUsado());
        assertEquals("regras-comprovantes", seguida.docId());
    }

    @Test
    void avaliacaoMedeRecuperacaoEClassificaFalhas() throws Exception {
        ChunkIndex index = ingest();
        RetrievalEval eval = new RetrievalEval();
        RetrievalEval.Report report = eval.evaluate(index, temp, config);
        assertTrue(report.precision() > 0);
        assertEquals(1.0, report.recall(), 0.001);
        assertTrue(report.relevancia() > 0);
        assertEquals(1.0, report.cobertura(), 0.001);
        assertEquals(1.0, report.groundedness(), 0.001);
        assertEquals(1.0, report.semEvidencia(), 0.001);
        assertTrue(report.diagnosticos().contains("documento_desatualizado"));
        assertTrue(report.diagnosticos().contains("chunk_mal_dividido"));
        assertTrue(report.diagnosticos().contains("filtro_incorreto"));
        assertEquals(240, report.chunkSize());
        assertTrue(report.rerank());
        eval.write(temp.resolve("eval-report.json"), report);
        String json = Files.readString(temp.resolve("eval-report.json"));
        assertTrue(json.contains("precision"));
        assertTrue(json.contains("chunk_mal_dividido"));
        try (var files = Files.list(Path.of("src/test/resources/inference/golden"))) {
            long total = files.filter(path -> path.getFileName().toString().endsWith(".json")).count();
            assertEquals(3, total);
        }
    }

    @Test
    void cliEReadmeUsamEntrypointLocal() throws Exception {
        assertEquals(0, RagMain.execute(new String[]{"ingest"}, temp));
        assertEquals(0, RagMain.execute(new String[]{
                "ask", "--session", "op-1", "--role", "operador", "--text", "prazo", "do", "comprovante", "digital"
        }, temp));
        String answer = Files.readString(temp.resolve("last-answer.txt"));
        assertTrue(answer.contains("regras-comprovantes"));
        String menu = Files.readString(Path.of("src/main/java/com/mundodev/filafacil/controller/MenuController.java"));
        assertTrue(menu.contains("case \"12\""));
        assertFalse(menu.contains("case \"13\""));
        assertFalse(menu.contains("rag"));
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("com.mundodev.filafacil.rag.RagMain"));
        String readme = Files.readString(Path.of("README.md"));
        assertTrue(readme.contains("exec:java@rag"));
        assertTrue(readme.contains("-Dexec.args=ingest"));
        assertTrue(readme.contains("TokenEmbedder"));
    }

    private ChunkIndex ingest() throws Exception {
        ChunkIndex index = new ChunkIndex(temp);
        for (SourceDocument doc : catalog.load()) {
            index.publish(doc, config);
        }
        return index;
    }

    private static KnowledgeChunk chunk(String doc, String dominio, String acesso, String secao, String texto) {
        SourceDocument source = new SourceDocument(
                doc, doc, "tipo", dominio, "v1", "2026-03-01", "operacao", acesso,
                List.of(new SourceDocument.Section(secao, texto)));
        return new Chunker().chunk(source, 240, 20).get(0);
    }
}
