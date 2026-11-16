package com.mundodev.filafacil.inference.catalog;

public record Tarefa(
        String id,
        double qualidadeMinima,
        double tetoCusto,
        int tetoLatenciaMs,
        String risco,
        String idioma,
        String janela,
        boolean toolUse,
        String privacidade,
        String formato,
        boolean streaming,
        boolean cachePermitido,
        boolean groundednessObrigatoria) {
}
