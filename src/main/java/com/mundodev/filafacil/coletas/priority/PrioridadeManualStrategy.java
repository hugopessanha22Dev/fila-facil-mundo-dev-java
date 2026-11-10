package com.mundodev.filafacil.coletas.priority;

import com.mundodev.filafacil.error.EntradaException;

import java.util.Locale;
import java.util.Set;

/**
 * Strategy padrao: usa a prioridade informada pelo usuario.
 */
public class PrioridadeManualStrategy implements PriorizacaoStrategy {

    private static final Set<String> PRIORIDADES = Set.of("baixa", "media", "alta");

    @Override
    public String decidir(ContextoPriorizacao contexto) {
        if (contexto == null || contexto.prioridadeInformada() == null || contexto.prioridadeInformada().isBlank()) {
            throw new EntradaException("Prioridade e obrigatoria.");
        }
        String norm = contexto.prioridadeInformada().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!PRIORIDADES.contains(norm)) {
            throw new EntradaException("Prioridade invalida. Use: baixa, media, alta");
        }
        return norm;
    }
}
