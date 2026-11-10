package com.mundodev.filafacil.inference;

public record InferenceParams(
        double temperature,
        double topP,
        int maxTokens,
        Long seed) {

    public static InferenceParams forLight() {
        return new InferenceParams(0.4, 0.9, 600, 42L);
    }

    public static InferenceParams forCritical() {
        return new InferenceParams(0.1, 0.8, 900, 42L);
    }
}
