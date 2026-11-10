package com.mundodev.filafacil.inference.prompt;

import java.util.List;

public record PromptDefinition(
        String id,
        String objective,
        String context,
        String inputExpected,
        List<String> constraints,
        List<String> qualityCriteria,
        String outputFormat,
        String systemTemplate) {
}
