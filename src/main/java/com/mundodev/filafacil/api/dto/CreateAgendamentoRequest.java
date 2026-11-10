package com.mundodev.filafacil.api.dto;

public record CreateAgendamentoRequest(
        String transportadora,
        String placa,
        String doca,
        String dataHora) {
}
