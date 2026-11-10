package com.mundodev.filafacil.auditoria.model;

import java.time.LocalDate;

public record EntregaRow(
        long id,
        String status,
        LocalDate dataEntrega,
        int valorCentavos,
        long clienteId,
        String clienteNome,
        long motoristaId,
        String motoristaNome,
        String ocorrenciaTipo,
        String reembolsoStatus) {
}
