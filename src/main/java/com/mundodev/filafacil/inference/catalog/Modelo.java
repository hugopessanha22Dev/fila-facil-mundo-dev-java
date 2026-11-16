package com.mundodev.filafacil.inference.catalog;

import java.util.List;

public record Modelo(
        String id,
        double qualidade,
        double custo,
        int latenciaMs,
        String janela,
        double disponibilidade,
        boolean multimodal,
        String privacidade,
        boolean toolUse,
        List<String> idiomas,
        String limitacao) {
}
