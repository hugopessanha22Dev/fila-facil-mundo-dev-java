package com.mundodev.filafacil.api.model;

import java.time.LocalDateTime;

public record Agendamento(
        Long id,
        String transportadora,
        String placa,
        String doca,
        LocalDateTime dataHora,
        String status,
        String anexoRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
