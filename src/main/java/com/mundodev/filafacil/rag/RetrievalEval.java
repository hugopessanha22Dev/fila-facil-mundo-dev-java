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
import java.util.List;

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
            List<String> diagnosticos) {

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
                diagnosticos);
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
