package com.mundodev.filafacil.inference.catalog;

public record ExecucaoInferencia(
        String gateway,
        String tarefa,
        String texto,
        boolean aceita,
        boolean erro,
        boolean fallback,
        String tipoFalha,
        long latenciaMs,
        double custo,
        int tokens,
        String estadoCircuito,
        int tentativas) {
}
