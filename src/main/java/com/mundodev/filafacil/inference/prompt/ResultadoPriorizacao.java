package com.mundodev.filafacil.inference.prompt;

import java.util.List;

public record ResultadoPriorizacao(
        List<EtapaPriorizacao> etapas,
        boolean prioridadeComoFato,
        String prioridade,
        boolean calibrada) {
}
