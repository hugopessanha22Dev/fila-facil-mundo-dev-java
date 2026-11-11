package com.mundodev.filafacil.aidevops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record ReleaseCombo(
        String promptVersion,
        String datasetVersion,
        String embeddingVersion,
        String schemaVersion,
        String modelConfigVersion) {

    public static ReleaseCombo from(JsonNode node) {
        return new ReleaseCombo(
                node.path("promptVersion").asText(),
                node.path("datasetVersion").asText(),
                node.path("embeddingVersion").asText(),
                node.path("schemaVersion").asText(),
                node.path("modelConfigVersion").asText());
    }

    public boolean sameAs(JsonNode node) {
        return equals(from(node));
    }

    public ObjectNode writeTo(ObjectNode node) {
        node.put("promptVersion", promptVersion);
        node.put("datasetVersion", datasetVersion);
        node.put("embeddingVersion", embeddingVersion);
        node.put("schemaVersion", schemaVersion);
        node.put("modelConfigVersion", modelConfigVersion);
        return node;
    }
}
