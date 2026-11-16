package com.mundodev.filafacil.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagGovernancaTest {

    private final CorpusCatalog catalog = new CorpusCatalog();
    private final RetrievalConfig config = RetrievalConfig.load();
    private final Retriever retriever = new Retriever();
    private final Chunker chunker = new Chunker();

    @TempDir
    Path temp;

    @Test
    void statusHashLinhagemEExecucao() throws Exception {
        SourceDocument coleta = catalog.find("politica-coletas");
        SourceDocument igual = coleta.withVersao("v9", coleta.secoes().get(0).texto());
        assertEquals(coleta.conteudoHash(), igual.conteudoHash());
        assertEquals(coleta.versao(), igual.versaoAnterior());

        ChunkIndex index = ingest();
        double[] vetorColeta = index.vectorOf("politica-coletas").clone();
        SourceDocument v2 = catalog.find("politica-entregas").withVersao(
                "v2", "A entrega vigente tem prazo de 48 horas. entrega prazo entrega prazo.");
        index.publish(v2, config);

        assertArrayEquals(vetorColeta, index.vectorOf("politica-coletas"));
        ChunkIndex.Execucao ultima = index.execucoes().get(index.execucoes().size() - 1);
        assertEquals(List.of("politica-entregas"), ultima.documentos());
        assertTrue(index.chunks().stream().anyMatch(chunk ->
                "politica-entregas".equals(chunk.docId())
                        && "v1".equals(chunk.versao())
                        && "historico".equals(chunk.namespace())));

        List<Retriever.Hit> hits = retriever.search(
                index.chunks(),
                "qual o prazo da entrega",
                Retriever.SearchFilter.of("operador", "entregas"),
                config);
        assertFalse(hits.isEmpty());
        assertEquals("v2", hits.get(0).chunk().versao());
        assertTrue(hits.stream().allMatch(hit -> "ativo".equals(hit.chunk().namespace()) && "vigente".equals(hit.chunk().status())));
    }

    @Test
    void tenantVigenciaERevisao() {
        RetrievalConfig folgado = config.withHybridWeight(1).withRerank(false).withCutoff(0.1);
        SourceDocument base = SourceDocument.vigente(
                "doc", "doc", "tipo", "coletas", "v1", "2026-03-01", "operacao", "publico",
                List.of(new SourceDocument.Section("janela", "coleta janela coleta janela")));
        KnowledgeChunk tenantA = chunker.chunk(base.withTenant("tenant-a"), 240, 20).get(0);
        KnowledgeChunk tenantB = chunker.chunk(base.withTenant("tenant-b"), 240, 20).get(0);
        List<Retriever.Hit> soA = retriever.search(
                List.of(tenantA, tenantB),
                "coleta janela",
                Retriever.SearchFilter.of("operador", null).comTenant("tenant-a"),
                folgado);
        assertFalse(soA.isEmpty());
        assertTrue(soA.stream().allMatch(hit -> "tenant-a".equals(hit.chunk().tenant())));

        KnowledgeChunk expirado = chunker.chunk(base.withStatus("expirado"), 240, 20).get(0);
        assertTrue(retriever.search(
                List.of(expirado), "coleta janela", Retriever.SearchFilter.of("operador", "coletas"), folgado).isEmpty());

        KnowledgeChunk revisao = chunker.chunk(base.withStatus("em_revisao"), 240, 20).get(0);
        assertTrue(retriever.search(
                List.of(revisao), "coleta janela", Retriever.SearchFilter.of("operador", "coletas"), folgado).isEmpty());
        assertFalse(retriever.search(
                List.of(revisao), "coleta janela", Retriever.SearchFilter.of("revisor", "coletas"), folgado).isEmpty());
    }

    @Test
    void expansaoAcrescentaSinonimoEMantemAPergunta() {
        String expandida = QueryExpander.expand("recibo digital");
        assertTrue(expandida.contains("recibo"));
        assertTrue(expandida.contains("comprovante"));

        KnowledgeChunk comprovante = chunker.chunk(catalog.find("regras-comprovantes"), 240, 20).get(0);
        RetrievalConfig lexical = config.withHybridWeight(1).withRerank(false).withCutoff(0.2);
        List<Retriever.Hit> hits = retriever.search(
                List.of(comprovante),
                "recibo",
                Retriever.SearchFilter.of("operador", "comprovantes"),
                lexical);
        assertEquals("regras-comprovantes", hits.get(0).chunk().docId());
    }

    @Test
    void compressaoRemoveRepetidoEGuardaTermoDistintivo() {
        Retriever.Hit repetido = hit("O prazo do comprovante digital ficticio e de 48 horas.");
        Retriever.Hit copia = hit("O prazo do comprovante digital ficticio e de 48 horas.");
        Retriever.Hit distintivo = hit("zzalfa evidencia operacional");
        List<Retriever.Hit> contexto = ContextoCompressor.comprimir(
                List.of(repetido, copia, distintivo), "zzalfa");
        assertEquals(1, contexto.stream().filter(item -> item.chunk().texto().contains("48 horas")).count());
        assertTrue(contexto.stream().anyMatch(item -> item.chunk().texto().contains("zzalfa")));
    }

    @Test
    void topKSimplesEAmpliado() {
        RetrievalConfig limitado = config.withTopK(1).withMaxTopK(2).withHybridWeight(1).withRerank(false).withCutoff(0.1);
        KnowledgeChunk primeira = pedaco("c1", "coletas", "coleta janela alpha");
        KnowledgeChunk segunda = pedaco("c2", "coletas", "coleta janela beta");
        List<Retriever.Hit> simples = retriever.search(
                List.of(primeira, segunda),
                "coleta janela",
                Retriever.SearchFilter.of("operador", "coletas"),
                limitado);
        assertEquals(1, simples.size());

        KnowledgeChunk coleta = pedaco("col", "coletas", "coleta entrega alpha");
        KnowledgeChunk entrega = pedaco("ent", "entregas", "coleta entrega beta");
        List<Retriever.Hit> ampla = retriever.search(
                List.of(coleta, entrega),
                "coleta entrega",
                Retriever.SearchFilter.of("operador", null),
                limitado);
        assertTrue(ampla.size() <= limitado.maxTopK());
        assertTrue(ampla.stream().anyMatch(hit -> "coletas".equals(hit.chunk().dominio())));
        assertTrue(ampla.stream().anyMatch(hit -> "entregas".equals(hit.chunk().dominio())));
    }

    @Test
    void memoriaGuardaFormatoIsolaTenantERecusaSegredo() throws Exception {
        ChunkIndex index = ingest();
        RagAssistant assistant = new RagAssistant(temp);
        SessionMemory memory = new SessionMemory(temp);
        assistant.ask(index, "fmt", "operador", "perfil noturno formato curto janela da coleta", 0, config);
        assistant.ask(index, "fmt", "operador", "qual a janela", 1_000, config);
        String arquivo = Files.readString(memory.file("fmt"));
        assertTrue(arquivo.contains("curto"));
        assertTrue(arquivo.contains("noturno"));

        memory.save(new SessionMemory.State(
                "s1", "resumo do tenant A", "coletas", List.of("fato A"), 0, "tenant-a", "noturno", "curto"));
        assertNull(memory.load("s1", "tenant-b", 1_000, 30));
        assertEquals("resumo do tenant A", memory.load("s1", "tenant-a", 1_000, 30).summary());

        memory.save(new SessionMemory.State(
                "sec",
                "chave sk-abcsegredo e doc 12345678901",
                "coletas",
                List.of("sk-abcsegredo", "12345678901", "gsk_outrosegredo"),
                0,
                "operacao",
                "",
                ""));
        String segredo = Files.readString(memory.file("sec"));
        assertFalse(segredo.contains("sk-"));
        assertFalse(segredo.contains("gsk_"));
        assertFalse(segredo.contains("12345678901"));
    }

    @Test
    void relatorioMarcaFalhasDeGovernanca() throws Exception {
        RetrievalEval eval = new RetrievalEval();
        RetrievalEval.Report report = eval.evaluate(ingest(), temp, config);
        String texto = String.join("\n", report.governanca());
        assertTrue(texto.contains("documento expirado"));
        assertTrue(texto.contains("bloquear na busca ativa"));
        assertTrue(texto.contains("top-k insuficiente"));
        assertTrue(texto.contains("ampliar o teto"));
        assertTrue(texto.contains("expansão inadequada"));
        assertTrue(texto.contains("preservar a intenção"));
        assertTrue(texto.contains("reranking fraco"));
        assertTrue(texto.contains("religar o reranking"));
        assertEquals(1.0, report.cobertura(), 0.001);
        assertEquals(1.0, report.groundedness(), 0.001);
        assertEquals(1.0, report.semEvidencia(), 0.001);
    }

    private ChunkIndex ingest() throws Exception {
        ChunkIndex index = new ChunkIndex(temp);
        for (SourceDocument doc : catalog.load()) {
            index.publish(doc, config);
        }
        return index;
    }

    private static Retriever.Hit hit(String texto) {
        KnowledgeChunk chunk = pedaco("ctx", "comprovantes", texto);
        return new Retriever.Hit(chunk, 1);
    }

    private static KnowledgeChunk pedaco(String doc, String dominio, String texto) {
        SourceDocument source = SourceDocument.vigente(
                doc, doc, "tipo", dominio, "v1", "2026-03-01", "operacao", "publico",
                List.of(new SourceDocument.Section("secao", texto)));
        return new Chunker().chunk(source, 240, 20).get(0);
    }
}
