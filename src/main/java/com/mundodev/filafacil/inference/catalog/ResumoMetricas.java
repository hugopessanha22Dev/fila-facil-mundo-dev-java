package com.mundodev.filafacil.inference.catalog;

import java.util.Map;

public record ResumoMetricas(
        String tarefa,
        long latenciaMs,
        double custo,
        int tokens,
        int erros,
        int fallbacks,
        Map<String, Integer> porTipoFalha) {
}
