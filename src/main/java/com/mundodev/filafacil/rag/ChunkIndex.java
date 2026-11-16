package com.mundodev.filafacil.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ChunkIndex {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Path root;
    private final Chunker chunker = new Chunker();
    private final List<KnowledgeChunk> chunks = new ArrayList<>();
    private final List<Execucao> execucoes = new ArrayList<>();

    public record Execucao(String id, List<String> documentos) {
    }

    public ChunkIndex(Path root) {
        this.root = root;
    }

    public void publish(SourceDocument doc, RetrievalConfig config) throws IOException {
        List<KnowledgeChunk> fresh = chunker.chunk(doc, config.chunkSize(), config.overlap());
        List<KnowledgeChunk> arquivados = chunks.stream()
                .filter(chunk -> chunk.docId().equals(doc.id()))
                .map(KnowledgeChunk::substituido)
                .toList();
        chunks.removeIf(chunk -> chunk.docId().equals(doc.id()));
        chunks.addAll(fresh);
        chunks.addAll(arquivados);
        writeNamespace(doc.dominio());
        registrar(doc);
    }

    public List<Execucao> execucoes() {
        return List.copyOf(execucoes);
    }

    public void load() throws IOException {
        chunks.clear();
        Path ns = root.resolve("ns");
        if (!Files.isDirectory(ns)) {
            return;
        }
        try (var dirs = Files.list(ns)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                Path file = dir.resolve("chunks.json");
                if (Files.exists(file)) {
                    chunks.addAll(read(file));
                }
            }
        }
    }

    public List<KnowledgeChunk> chunks() {
        return List.copyOf(chunks);
    }

    public double[] vectorOf(String docId) {
        return chunks.stream()
                .filter(chunk -> chunk.docId().equals(docId))
                .map(KnowledgeChunk::vector)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("vetor ausente: " + docId));
    }

    public Path namespaceFile(String dominio) {
        return root.resolve("ns").resolve(dominio).resolve("chunks.json");
    }

    private void writeNamespace(String dominio) throws IOException {
        Path file = namespaceFile(dominio);
        Files.createDirectories(file.getParent());
        ArrayNode array = MAPPER.createArrayNode();
        for (KnowledgeChunk chunk : chunks) {
            if (!dominio.equals(chunk.dominio())) {
                continue;
            }
            ObjectNode node = array.addObject();
            node.put("id", chunk.id());
            node.put("docId", chunk.docId());
            node.put("titulo", chunk.titulo());
            node.put("tipo", chunk.tipo());
            node.put("dominio", chunk.dominio());
            node.put("versao", chunk.versao());
            node.put("dataAtualizacao", chunk.dataAtualizacao());
            node.put("owner", chunk.owner());
            node.put("acesso", chunk.acesso());
            node.put("secao", chunk.secao());
            node.put("texto", chunk.texto());
            node.put("status", chunk.status());
            node.put("conteudoHash", chunk.conteudoHash());
            node.put("tenant", chunk.tenant());
            node.put("namespace", chunk.namespace());
            ArrayNode vector = node.putArray("vector");
            for (double value : chunk.vector()) {
                vector.add(value);
            }
        }
        MAPPER.writeValue(file.toFile(), array);
    }

    private List<KnowledgeChunk> read(Path file) throws IOException {
        ArrayNode array = (ArrayNode) MAPPER.readTree(file.toFile());
        List<KnowledgeChunk> loaded = new ArrayList<>();
        for (var node : array) {
            ArrayNode stored = (ArrayNode) node.get("vector");
            double[] vector = new double[stored.size()];
            for (int i = 0; i < stored.size(); i++) {
                vector[i] = stored.get(i).asDouble();
            }
            loaded.add(new KnowledgeChunk(
                    node.path("id").asText(),
                    node.path("docId").asText(),
                    node.path("titulo").asText(),
                    node.path("tipo").asText(),
                    node.path("dominio").asText(),
                    node.path("versao").asText(),
                    node.path("dataAtualizacao").asText(),
                    node.path("owner").asText(),
                    node.path("acesso").asText(),
                    node.path("secao").asText(),
                    node.path("texto").asText(),
                    texto(node, "status", "vigente"),
                    texto(node, "conteudoHash", ""),
                    texto(node, "tenant", "operacao"),
                    texto(node, "namespace", "ativo"),
                    vector));
        }
        return loaded;
    }

    private void registrar(SourceDocument doc) throws IOException {
        execucoes.add(new Execucao("exec-" + (execucoes.size() + 1), List.of(doc.id())));
        MAPPER.writeValue(root.resolve("execucoes.json").toFile(), execucoes);
    }

    private static String texto(JsonNode node, String campo, String padrao) {
        String valor = node.path(campo).asText("");
        return valor.isBlank() ? padrao : valor;
    }
}
