package com.mundodev.filafacil.pipeline.model;

public record MetricaDiaria(
        String dataRef,
        String regiao,
        String status,
        String clienteCodigo,
        long totalColetas,
        long totalEntregas,
        long totalAtrasos,
        long totalOcorrencias) {
}
