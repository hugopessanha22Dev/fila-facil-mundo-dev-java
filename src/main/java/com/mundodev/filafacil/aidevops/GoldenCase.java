package com.mundodev.filafacil.aidevops;

public record GoldenCase(
        String id,
        String category,
        String message,
        String expectedTipo,
        String expectedPrioridade) {
}
