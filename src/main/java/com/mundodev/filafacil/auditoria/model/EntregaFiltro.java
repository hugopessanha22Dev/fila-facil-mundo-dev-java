package com.mundodev.filafacil.auditoria.model;

import java.time.LocalDate;

public record EntregaFiltro(
        String status,
        LocalDate dataInicio,
        LocalDate dataFim,
        Long clienteId,
        Long motoristaId,
        Boolean temReembolso) {

    public static EntregaFiltro vazio() {
        return new EntregaFiltro(null, null, null, null, null, null);
    }
}
