package com.mundodev.filafacil.inference;

import java.util.List;

public record TriageResult(
        String tipoSolicitacao,
        String codigoColeta,
        String cliente,
        String sentimento,
        String prioridadeSugerida,
        String resumoOperacional,
        String proximaAcao,
        boolean intervencaoHumana,
        String justificativaIntervencencao,
        double nivelConfianca,
        List<String> camposAusentesOuErro,
        List<String> indicadoresFalha) {
}
