package com.mundodev.filafacil.coletas.priority;

import com.mundodev.filafacil.error.EntradaException;

/**
 * Strategy alternativa: prioridade pela quantidade de pacotes.
 * >= 10 alta, >= 5 media, senao baixa.
 */
public class PrioridadePorPacotesStrategy implements PriorizacaoStrategy {

    @Override
    public String decidir(ContextoPriorizacao contexto) {
        if (contexto == null || contexto.pacotes() == null) {
            throw new EntradaException("Quantidade de pacotes e obrigatoria.");
        }
        int pacotes = contexto.pacotes();
        if (pacotes <= 0) {
            throw new EntradaException("Quantidade de pacotes deve ser maior que zero.");
        }
        if (pacotes >= 10) {
            return "alta";
        }
        if (pacotes >= 5) {
            return "media";
        }
        return "baixa";
    }
}
