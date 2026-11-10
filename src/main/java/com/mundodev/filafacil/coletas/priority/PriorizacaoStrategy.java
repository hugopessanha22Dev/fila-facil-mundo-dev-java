package com.mundodev.filafacil.coletas.priority;

public interface PriorizacaoStrategy {

    /**
     * Decide a prioridade normalizada: baixa | media | alta.
     */
    String decidir(ContextoPriorizacao contexto);
}
