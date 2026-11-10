package com.mundodev.filafacil.inference.tools;

import com.mundodev.filafacil.coletas.Coleta;

import java.util.Map;

/**
 * Contratos das tools de inferencia (entrada/saida/erro). Implementacao tipica: mock.
 */
public interface OperationalTools {

    Map<String, Object> consultarStatusEntrega(String codigoColeta);

    Coleta buscarColeta(long id);

    Map<String, Object> registrarOcorrencia(String codigoColeta, String descricao);

    String calcularPrioridadeOperacional(String tipoSolicitacao, String sentimento);

    Map<String, Object> encaminharAtendimentoHumano(String motivo, String codigoColeta);
}
