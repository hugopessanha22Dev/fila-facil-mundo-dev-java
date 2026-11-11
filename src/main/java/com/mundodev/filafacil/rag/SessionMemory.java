package com.mundodev.filafacil.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SessionMemory {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public record State(String id, String summary, String lastDomain, List<String> facts, long updatedAtEpochMs) {
    }

    private final Path root;

    public SessionMemory(Path root) {
        this.root = root.resolve("sessions");
    }

    public State load(String id, long nowMs, int ttlMinutes) throws IOException {
        Path file = file(id);
        if (!Files.exists(file)) {
            return null;
        }
        var node = MAPPER.readTree(file.toFile());
        long updated = node.path("updatedAtEpochMs").asLong();
        long ttlMs = ttlMinutes * 60_000L;
        if (nowMs - updated > ttlMs) {
            Files.deleteIfExists(file);
            return null;
        }
        List<String> facts = new ArrayList<>();
        for (var fact : node.path("facts")) {
            facts.add(fact.asText());
        }
        String domain = node.path("lastDomain").asText("");
        return new State(
                id,
                node.path("summary").asText(""),
                domain.isBlank() ? null : domain,
                List.copyOf(facts),
                updated);
    }

    public void save(State state) throws IOException {
        Files.createDirectories(root);
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", state.id());
        node.put("summary", state.summary());
        node.put("lastDomain", state.lastDomain() == null ? "" : state.lastDomain());
        ArrayNode facts = node.putArray("facts");
        for (String fact : state.facts()) {
            facts.add(fact);
        }
        node.put("updatedAtEpochMs", state.updatedAtEpochMs());
        MAPPER.writeValue(file(state.id()).toFile(), node);
    }

    public Path file(String id) {
        String safe = id.replaceAll("[^A-Za-z0-9\\-]", "_");
        return root.resolve(safe + ".json");
    }
}
