package com.mundodev.filafacil.inference.prompt;

import java.util.Set;

public final class FontesAutorizadas {

    public static final Set<String> FERRAMENTAS = Set.of(
            "consultarStatusEntrega",
            "buscarColeta",
            "registrarOcorrencia",
            "calcularPrioridadeOperacional",
            "encaminharAtendimentoHumano");

    private FontesAutorizadas() {
    }

    public static boolean documental(EvidenciaPrompt evidencia) {
        if (evidencia == null || evidencia.texto() == null || evidencia.texto().isBlank()) {
            return false;
        }
        if ("memoria".equals(evidencia.origem())) {
            return false;
        }
        if ("ferramenta".equals(evidencia.origem())) {
            return evidencia.ferramenta() != null && FERRAMENTAS.contains(evidencia.ferramenta());
        }
        return "documento".equals(evidencia.origem());
    }
}
