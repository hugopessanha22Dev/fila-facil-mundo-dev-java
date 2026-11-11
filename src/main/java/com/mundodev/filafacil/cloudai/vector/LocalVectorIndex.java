package com.mundodev.filafacil.cloudai.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LocalVectorIndex {

    public static final double CUTOFF = 0.55;

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Path domainDir;

    public LocalVectorIndex(Path root, String environment, String domain) {
        this.domainDir = root.resolve(environment).resolve("embeddings").resolve(domain);
    }

    public void reprocess(String version, List<OperationalDocument> documents) throws IOException {
        Path dir = versionDir(version);
        Files.createDirectories(dir);
        ArrayNode array = MAPPER.createArrayNode();
        for (OperationalDocument document : documents) {
            ObjectNode node = array.addObject();
            node.put("id", document.id());
            node.put("familia", document.familia().id());
            node.put("texto", document.texto());
            ArrayNode vector = node.putArray("vector");
            for (double value : TokenEmbedder.embed(document.texto())) {
                vector.add(value);
            }
        }
        MAPPER.writeValue(dir.resolve("index.json").toFile(), array);
        Files.writeString(domainDir.resolve("active.txt"), version);
    }

    public boolean versionExists(String version) {
        return Files.exists(versionDir(version).resolve("index.json"));
    }

    public String activeVersion() throws IOException {
        Path active = domainDir.resolve("active.txt");
        if (!Files.exists(active)) {
            return "";
        }
        return Files.readString(active).trim();
    }

    public Optional<ScoredDocument> search(String text) throws IOException {
        return search(text, activeVersion());
    }

    public Optional<ScoredDocument> search(String text, String version) throws IOException {
        Path index = versionDir(version).resolve("index.json");
        if (version == null || version.isBlank() || !Files.exists(index)) {
            return Optional.empty();
        }
        ArrayNode array = (ArrayNode) MAPPER.readTree(index.toFile());
        double[] query = TokenEmbedder.embed(text);
        ScoredDocument best = null;
        for (var node : array) {
            double[] vector = new double[TokenEmbedder.DIMENSIONS];
            ArrayNode stored = (ArrayNode) node.get("vector");
            for (int i = 0; i < vector.length && i < stored.size(); i++) {
                vector[i] = stored.get(i).asDouble();
            }
            double score = TokenEmbedder.cosine(query, vector);
            if (score < CUTOFF) {
                continue;
            }
            if (best == null || score > best.similarity()) {
                OperationalDocument document = new OperationalDocument(
                        node.path("id").asText(),
                        DocumentFamily.parse(node.path("familia").asText()),
                        node.path("texto").asText());
                best = new ScoredDocument(document, version, score);
            }
        }
        return Optional.ofNullable(best);
    }

    public Path versionDir(String version) {
        return domainDir.resolve("v" + version);
    }

    public List<String> versionsOnDisk() throws IOException {
        List<String> versions = new ArrayList<>();
        if (!Files.isDirectory(domainDir)) {
            return versions;
        }
        try (var stream = Files.list(domainDir)) {
            stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("v"))
                    .forEach(versions::add);
        }
        return versions;
    }
}
