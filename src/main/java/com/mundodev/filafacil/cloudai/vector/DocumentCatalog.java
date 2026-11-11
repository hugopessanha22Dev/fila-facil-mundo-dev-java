package com.mundodev.filafacil.cloudai.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class DocumentCatalog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DocumentCatalog() {
    }

    public static List<OperationalDocument> loadDefault() {
        try (InputStream in = DocumentCatalog.class.getResourceAsStream("/cloudai/operational-docs.json")) {
            if (in == null) {
                throw new IllegalStateException("operational-docs.json ausente");
            }
            return parse(MAPPER.readTree(in));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Falha ao ler documentos", ex);
        }
    }

    public static List<OperationalDocument> parse(String json) {
        try {
            return parse(MAPPER.readTree(json));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("documentos invalidos", ex);
        }
    }

    private static List<OperationalDocument> parse(JsonNode root) {
        List<OperationalDocument> docs = new ArrayList<>();
        if (!root.isArray()) {
            throw new IllegalArgumentException("documentos invalidos");
        }
        for (JsonNode node : root) {
            DocumentFamily family = DocumentFamily.parse(node.path("familia").asText());
            docs.add(new OperationalDocument(
                    node.path("id").asText(),
                    family,
                    node.path("texto").asText()));
        }
        return List.copyOf(docs);
    }
}
