package com.mundodev.filafacil.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RetrievalEval {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final String MARKER = "prazo do comprovante digital ficticio e de 48 horas";

    public record Question(
            String id,
            String text,
            String expectedDoc,
            String expectedVersion,
            String expectedSection,
            boolean absent) {
    }

    public record Report(
            double precision,
            double recall,
            double relevancia,
            double cobertura,
            double groundedness,
            double semEvidencia,
            int chunkSize,
            int overlap,
            int topK,
            boolean rerank,
            List<String> diagnosticos,
            List<String> governanca) {

        public String render() {
            return "precision=" + precision
                    + " recall=" + recall
                    + " relevancia=" + relevancia
                    + " cobertura=" + cobertura
                    + " groundedness=" + groundedness
                    + " semEvidencia=" + semEvidencia
                    + " chunkSize=" + chunkSize
                    + " overlap=" + overlap
                    + " topK=" + topK
                    + " filtro=acesso+dominio+tipo+versao"
                    + " rerank=" + rerank
                    + " diagnosticos=" + diagnosticos
                    + " governanca=" + governanca
                    + " evita chunkSize=chunk_mal_dividido"
                    + " evita filtro=filtro_incorreto"
                    + " evita versao=documento_desatualizado";
        }
    }

    private final Retriever retriever = new Retriever();

    public List<Question> questions() {
        try (InputStream in = RetrievalEval.class.getResourceAsStream("/rag/questions.json")) {
            if (in == null) {
                throw new IllegalStateException("questions.json ausente");
            }
            List<Question> questions = new ArrayList<>();
            for (JsonNode node : MAPPER.readTree(in)) {
                questions.add(new Question(
                        node.path("id").asText(),
                        node.path("text").asText(),
                        node.path("expectedDoc").asText(),
                        node.path("expectedVersion").asText(),
                        node.path("expectedSection").asText(),
                        node.path("absent").asBoolean()));
            }
            return List.copyOf(questions);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler questions.json", ex);
        }
    }

    public Report evaluate(ChunkIndex index, Path root, RetrievalConfig config) throws IOException {
        RagAssistant assistant = new RagAssistant(root.resolve("eval-sessions"));
        List<Question> questions = questions();
        int evidence = 0;
        int absent = 0;
        double precisionSum = 0;
        double recallSum = 0;
        double relevanceSum = 0;
        int relevanceCount = 0;
        int covered = 0;
        int groundedHits = 0;
        int groundedTotal = 0;
        int absentOk = 0;
        long now = 1_000L;
        for (Question question : questions) {
            String domain = DomainIntent.domainOf(question.text());
            List<Retriever.Hit> hits = retriever.search(
                    index.chunks(),
                    question.text(),
                    Retriever.SearchFilter.of("operador", domain),
                    config);
            RagAnswer answer = assistant.ask(index, "eval-" + question.id(), "operador", question.text(), now, config);
            now += 1;
            if (question.absent()) {
                absent++;
                if (!answer.found()) {
                    absentOk++;
                }
                continue;
            }
            evidence++;
            int relevant = 0;
            double relevantScore = 0;
            for (Retriever.Hit hit : hits) {
                if (question.expectedDoc().equals(hit.chunk().docId())
                        && question.expectedSection().equals(hit.chunk().secao())) {
                    relevant++;
                    relevantScore += hit.score();
                }
            }
            precisionSum += hits.isEmpty() ? 0 : relevant / (double) hits.size();
            recallSum += relevant > 0 ? 1 : 0;
            if (relevant > 0) {
                covered++;
                relevanceSum += relevantScore / relevant;
                relevanceCount++;
            }
            if (answer.found()) {
                groundedTotal++;
                boolean cited = hits.stream().anyMatch(hit ->
                        hit.chunk().docId().equals(answer.docId())
                                && hit.chunk().versao().equals(answer.versao())
                                && hit.chunk().secao().equals(answer.secao()));
                if (cited) {
                    groundedHits++;
                }
            }
        }
        List<String> diagnosticos = diagnose(new CorpusCatalog(), config);
        List<String> governanca = governanca(config);
        return new Report(
                ratio(precisionSum, evidence),
                ratio(recallSum, evidence),
                relevanceCount == 0 ? 0 : relevanceSum / relevanceCount,
                ratio(covered, evidence),
                ratio(groundedHits, groundedTotal),
                ratio(absentOk, absent),
                config.chunkSize(),
                config.overlap(),
                config.topK(),
                config.rerank(),
                diagnosticos,
                governanca);
    }

    public List<String> diagnose(CorpusCatalog catalog, RetrievalConfig config) {
        List<String> labels = new ArrayList<>();
        KnowledgeChunk antigo = new Chunker()
                .chunk(catalog.find("politica-entregas"), config.chunkSize(), config.overlap())
                .get(0);
        String stale = RagAnswer.classifyStale(RagAnswer.cited(antigo, "entregas", 1), "v2");
        if (!stale.isBlank()) {
            labels.add(stale);
        }
        boolean split = new Chunker().chunk(catalog.find("regras-comprovantes"), 30, 0).stream()
                .noneMatch(chunk -> chunk.texto().contains(MARKER));
        String chunk = RagAnswer.classifyChunk(split);
        if (!chunk.isBlank()) {
            labels.add(chunk);
        }
        KnowledgeChunk faq = new Chunker().chunk(catalog.find("faq-atendimento"), 240, 20).get(0);
        boolean hidden = retriever.search(
                List.of(faq),
                faq.texto(),
                Retriever.SearchFilter.of("parceiro", "atendimento"),
                config.withCutoff(0.1)).isEmpty();
        String filter = RagAnswer.classifyFilter(hidden);
        if (!filter.isBlank()) {
            labels.add(filter);
        }
        return List.copyOf(labels);
    }

    public List<String> governanca(RetrievalConfig config) {
        Map<String, String[]> catalogo = falhas();
        List<String> linhas = new ArrayList<>();
        if (respostaUsaExpirado()) {
            linhas.add(linha(catalogo, "documento_expirado"));
        }
        if (topKInsuficiente(config)) {
            linhas.add(linha(catalogo, "top_k_insuficiente"));
        }
        if (expansaoTrocaDominio()) {
            linhas.add(linha(catalogo, "expansao_inadequada"));
        }
        if (rerankingFraco(config)) {
            linhas.add(linha(catalogo, "reranking_fraco"));
        }
        return List.copyOf(linhas);
    }

    private Map<String, String[]> falhas() {
        try (InputStream in = RetrievalEval.class.getResourceAsStream("/rag/falhas-governanca.json")) {
            if (in == null) {
                throw new IllegalStateException("falhas-governanca.json ausente");
            }
            Map<String, String[]> catalogo = new LinkedHashMap<>();
            for (JsonNode node : MAPPER.readTree(in)) {
                catalogo.put(node.path("id").asText(), new String[]{
                        node.path("falha").asText(),
                        node.path("acao").asText()
                });
            }
            return catalogo;
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler falhas-governanca.json", ex);
        }
    }

    private static String linha(Map<String, String[]> catalogo, String id) {
        String[] item = catalogo.get(id);
        if (item == null) {
            throw new IllegalStateException("falha ausente: " + id);
        }
        return item[0] + " | " + item[1];
    }

    private static boolean respostaUsaExpirado() {
        SourceDocument doc = SourceDocument.vigente(
                "expirado", "expirado", "politica", "entregas", "v0", "2020-01-01", "operacao", "publico",
                List.of(new SourceDocument.Section("prazo", "entrega prazo expirada")));
        KnowledgeChunk chunk = new Chunker().chunk(doc.withStatus("expirado"), 240, 20).get(0);
        RagAnswer answer = RagAnswer.cited(chunk, "entregas", 1);
        return answer.found() && "expirado".equals(chunk.status());
    }

    private boolean topKInsuficiente(RetrievalConfig config) {
        KnowledgeChunk primeiro = pedaco("a", "coletas", "janela", "coleta entrega alpha alpha");
        KnowledgeChunk esperado = pedaco("b", "entregas", "prazo", "coleta entrega beta");
        String query = "coleta entrega alpha";
        RetrievalConfig estreito = config.withTopK(1).withMaxTopK(1).withHybridWeight(1).withRerank(false).withCutoff(0.1);
        RetrievalConfig amplo = estreito.withMaxTopK(2);
        Retriever.SearchFilter filtro = Retriever.SearchFilter.of("operador", null);
        List<KnowledgeChunk> chunks = List.of(primeiro, esperado);
        boolean deFora = retriever.search(chunks, query, filtro, estreito).stream()
                .noneMatch(hit -> "b".equals(hit.chunk().docId()));
        boolean dentro = retriever.search(chunks, query, filtro, amplo).stream()
                .anyMatch(hit -> "b".equals(hit.chunk().docId()));
        return deFora && dentro;
    }

    private static boolean expansaoTrocaDominio() {
        String original = "prazo da coleta";
        String ruim = QueryExpander.trocaDominio(original, "entrega");
        return original.contains("coleta") && !ruim.contains("coleta") && ruim.contains("entrega");
    }

    private boolean rerankingFraco(RetrievalConfig config) {
        KnowledgeChunk generico = pedaco("errado", "entregas", "prazo geral", "comprovante digital urgente");
        KnowledgeChunk vigente = pedaco("certo", "comprovantes", "comprovante", "comprovante alpha beta gamma delta epsilon");
        RetrievalConfig semRerank = config.withHybridWeight(0).withRerank(false).withCutoff(0.2).withTopK(2);
        List<Retriever.Hit> hits = retriever.search(
                List.of(generico, vigente),
                "comprovante digital urgente",
                Retriever.SearchFilter.of("operador", null),
                semRerank);
        return !hits.isEmpty() && !"comprovante".equals(hits.get(0).chunk().secao());
    }

    private static KnowledgeChunk pedaco(String doc, String dominio, String secao, String texto) {
        SourceDocument source = SourceDocument.vigente(
                doc, doc, "tipo", dominio, "v1", "2026-03-01", "operacao", "publico",
                List.of(new SourceDocument.Section(secao, texto)));
        return new Chunker().chunk(source, 240, 20).get(0);
    }

    public void write(Path file, Report report) throws IOException {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("precision", report.precision());
        node.put("recall", report.recall());
        node.put("relevancia", report.relevancia());
        node.put("cobertura", report.cobertura());
        node.put("groundedness", report.groundedness());
        node.put("semEvidencia", report.semEvidencia());
        node.put("chunkSize", report.chunkSize());
        node.put("overlap", report.overlap());
        node.put("topK", report.topK());
        node.put("filtro", "acesso+dominio+tipo+versao");
        node.put("rerank", report.rerank());
        ArrayNode labels = node.putArray("diagnosticos");
        for (String label : report.diagnosticos()) {
            labels.add(label);
        }
        ArrayNode governanca = node.putArray("governanca");
        for (String item : report.governanca()) {
            governanca.add(item);
        }
        node.put("evitaChunkMalDividido", "chunkSize");
        node.put("evitaFiltroIncorreto", "filtro");
        node.put("evitaDocumentoDesatualizado", "versao");
        MAPPER.writeValue(file.toFile(), node);
    }

    private static double ratio(double hits, int total) {
        if (total == 0) {
            return 0;
        }
        return hits / total;
    }
}
