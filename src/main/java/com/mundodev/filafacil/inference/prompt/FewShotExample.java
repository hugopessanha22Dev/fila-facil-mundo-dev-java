package com.mundodev.filafacil.inference.prompt;

public record FewShotExample(
        String id,
        String type,
        String taskId,
        String input,
        String output) {
}
