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
import java.util.regex.Pattern;

public final class SessionMemory {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Pattern SEGREDO = Pattern.compile("(?i)(?:gsk_|sk-)[A-Za-z0-9_\\-]+");
    private static final Pattern DIGITOS = Pattern.compile("\\d{11,}");

    public record State(
            String id,
            String summary,
            String lastDomain,
            List<String> facts,
            long updatedAtEpochMs,
            String tenant,
            String perfil,
            String formato) {
    }

    private final Path root;

    public SessionMemory(Path root) {
        this.root = root.resolve("sessions");
    }

    public State load(String id, long nowMs, int ttlMinutes) throws IOException {
        return load(id, "operacao", nowMs, ttlMinutes);
    }

    public State load(String id, String tenant, long nowMs, int ttlMinutes) throws IOException {
        Path file = file(id);
        if (!Files.exists(file)) {
            return null;
        }
        var node = MAPPER.readTree(file.toFile());
        String storedTenant = texto(node.path("tenant").asText("operacao"), "operacao");
        if (!storedTenant.equals(tenant)) {
            return null;
        }
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
                updated,
                storedTenant,
                node.path("perfil").asText(""),
                node.path("formato").asText(""));
    }

    public void save(State state) throws IOException {
        Files.createDirectories(root);
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", state.id());
        node.put("summary", redact(state.summary()));
        node.put("lastDomain", state.lastDomain() == null ? "" : state.lastDomain());
        ArrayNode facts = node.putArray("facts");
        for (String fact : state.facts()) {
            String limpo = redact(fact);
            if (!limpo.isBlank()) {
                facts.add(limpo);
            }
        }
        node.put("updatedAtEpochMs", state.updatedAtEpochMs());
        node.put("tenant", texto(state.tenant(), "operacao"));
        node.put("perfil", redact(state.perfil()));
        node.put("formato", redact(state.formato()));
        MAPPER.writeValue(file(state.id()).toFile(), node);
    }

    public Path file(String id) {
        String safe = id.replaceAll("[^A-Za-z0-9\\-]", "_");
        return root.resolve(safe + ".json");
    }

    static String redact(String value) {
        if (value == null) {
            return "";
        }
        String semChave = SEGREDO.matcher(value).replaceAll("");
        return DIGITOS.matcher(semChave).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    private static String texto(String value, String padrao) {
        return value == null || value.isBlank() ? padrao : value;
    }
}
