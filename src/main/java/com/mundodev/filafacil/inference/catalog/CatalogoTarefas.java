package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatalogoTarefas {

    private final Map<String, Tarefa> tarefas;

    private CatalogoTarefas(Map<String, Tarefa> tarefas) {
        this.tarefas = tarefas;
    }

    public static CatalogoTarefas carregar() {
        JsonNode raiz = Recursos.ler("/inference/catalog/tarefas.json");
        Map<String, Tarefa> tarefas = new LinkedHashMap<>();
        for (JsonNode node : raiz.path("tarefas")) {
            Tarefa tarefa = new Tarefa(
                    node.path("id").asText(),
                    node.path("qualidadeMinima").asDouble(),
                    node.path("tetoCusto").asDouble(),
                    node.path("tetoLatenciaMs").asInt(),
                    node.path("risco").asText(),
                    node.path("idioma").asText(),
                    node.path("janela").asText(),
                    node.path("toolUse").asBoolean(),
                    node.path("privacidade").asText(),
                    node.path("formato").asText(),
                    node.path("streaming").asBoolean(),
                    node.path("cachePermitido").asBoolean(),
                    node.path("groundednessObrigatoria").asBoolean());
            tarefas.put(tarefa.id(), tarefa);
        }
        return new CatalogoTarefas(Map.copyOf(tarefas));
    }

    public Tarefa porId(String id) {
        Tarefa tarefa = tarefas.get(id);
        if (tarefa == null) {
            throw new IllegalArgumentException("Tarefa ausente: " + id);
        }
        return tarefa;
    }

    public List<Tarefa> todas() {
        return List.copyOf(tarefas.values());
    }
}
