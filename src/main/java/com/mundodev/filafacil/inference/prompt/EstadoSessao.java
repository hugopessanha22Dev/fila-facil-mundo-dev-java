package com.mundodev.filafacil.inference.prompt;

import java.util.List;

public record EstadoSessao(
        String id,
        String intencao,
        List<String> pendentes,
        List<EvidenciaPrompt> evidencias,
        String historico) {
}
