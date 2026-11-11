package com.mundodev.filafacil.cloudai.vector;

import java.util.Locale;

public final class TokenEmbedder {

    public static final int DIMENSIONS = 64;

    private TokenEmbedder() {
    }

    public static double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        if (text == null) {
            return vector;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.isBlank()) {
                continue;
            }
            vector[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1.0;
        }
        return vector;
    }

    public static double cosine(double[] left, double[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
