package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatalogoModelos {

    private final Map<String, Modelo> modelos;

    private CatalogoModelos(Map<String, Modelo> modelos) {
        this.modelos = modelos;
    }

    public static CatalogoModelos carregar() {
        JsonNode raiz = Recursos.ler("/inference/catalog/modelos.json");
        Map<String, Modelo> modelos = new LinkedHashMap<>();
        for (JsonNode node : raiz.path("modelos")) {
            List<String> idiomas = new ArrayList<>();
            node.path("idiomas").forEach(item -> idiomas.add(item.asText()));
            Modelo modelo = new Modelo(
                    node.path("id").asText(),
                    node.path("qualidade").asDouble(),
                    node.path("custo").asDouble(),
                    node.path("latenciaMs").asInt(),
                    node.path("janela").asText(),
                    node.path("disponibilidade").asDouble(),
                    node.path("multimodal").asBoolean(),
                    node.path("privacidade").asText(),
                    node.path("toolUse").asBoolean(),
                    List.copyOf(idiomas),
                    node.path("limitacao").asText());
            modelos.put(modelo.id(), modelo);
        }
        return new CatalogoModelos(Map.copyOf(modelos));
    }

    public Modelo porId(String id) {
        Modelo modelo = modelos.get(id);
        if (modelo == null) {
            throw new IllegalArgumentException("Modelo ausente: " + id);
        }
        return modelo;
    }

    public List<Modelo> todos() {
        return List.copyOf(modelos.values());
    }
}
