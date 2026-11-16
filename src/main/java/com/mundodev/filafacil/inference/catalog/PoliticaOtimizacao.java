package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class PoliticaOtimizacao {

    private final List<Estrategia> estrategias;

    private PoliticaOtimizacao(List<Estrategia> estrategias) {
        this.estrategias = estrategias;
    }

    public static PoliticaOtimizacao carregar() {
        JsonNode raiz = Recursos.ler("/inference/catalog/otimizacao.json");
        List<Estrategia> estrategias = new ArrayList<>();
        for (JsonNode node : raiz.path("estrategias")) {
            estrategias.add(new Estrategia(
                    node.path("id").asText(),
                    node.path("uso").asText(),
                    node.path("proibicao").asText(),
                    node.path("risco").asText()));
        }
        return new PoliticaOtimizacao(List.copyOf(estrategias));
    }

    public List<Estrategia> estrategias() {
        return estrategias;
    }

    public Estrategia porId(String id) {
        return estrategias.stream()
                .filter(estrategia -> id.equals(estrategia.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estrategia ausente: " + id));
    }

    public record Estrategia(String id, String uso, String proibicao, String risco) {
    }
}
