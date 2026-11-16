package com.mundodev.filafacil.inference.catalog;

import java.util.List;

public record ParametrosTarefa(
        double temperature,
        double topP,
        int maxTokens,
        List<String> stop,
        int seed,
        boolean streaming) {
}
