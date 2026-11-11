package com.mundodev.filafacil.cloudai.infra;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public final class InfraTagValidator {

    private static final List<String> REQUIRED = List.of("Environment", "Owner", "Purpose");

    private InfraTagValidator() {
    }

    public static void validate(JsonNode root) {
        JsonNode resources = root.path("resources");
        if (!resources.isArray() || resources.isEmpty()) {
            throw new IllegalArgumentException("resources ausente");
        }
        for (JsonNode resource : resources) {
            for (String tag : REQUIRED) {
                if (resource.path(tag).asText("").isBlank()) {
                    throw new IllegalArgumentException("Recurso sem tag " + tag + ": " + resource.path("type").asText(""));
                }
            }
        }
    }
}
