package com.mundodev.filafacil.inference;

public final class DefaultPrompts {

    public static final String TRIAGE_ONE_SHOT = """
            Voce e um assistente de triagem operacional da FilaFacil Logistica.
            Responda APENAS com um unico objeto JSON valido (sem markdown) com os campos:
            tipoSolicitacao (consulta_entrega|ocorrencia_operacional|reclamacao_cliente),
            codigoColeta (string|null), cliente (string|null), sentimento (neutro|ansioso|insatisfeito|urgente),
            prioridadeSugerida (baixa|media|alta), resumoOperacional, proximaAcao,
            intervencaoHumana (boolean), justificativaIntervencencao, nivelConfianca (0..1),
            camposAusentesOuErro (array de string), indicadoresFalha (array de string).
            """;

    private DefaultPrompts() {
    }
}
