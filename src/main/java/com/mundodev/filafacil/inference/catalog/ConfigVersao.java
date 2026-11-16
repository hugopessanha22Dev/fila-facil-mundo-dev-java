package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ConfigVersao(String versao, Map<String, ParametrosTarefa> tarefas) {

    public static ConfigVersao carregar(String versao) {
        JsonNode raiz = Recursos.ler("/inference/catalog/config-" + versao + ".json");
        Map<String, ParametrosTarefa> tarefas = new LinkedHashMap<>();
        raiz.path("tarefas").fields().forEachRemaining(entrada -> {
            JsonNode node = entrada.getValue();
            List<String> stop = new ArrayList<>();
            node.path("stop").forEach(item -> stop.add(item.asText()));
            tarefas.put(entrada.getKey(), new ParametrosTarefa(
                    node.path("temperature").asDouble(),
                    node.path("topP").asDouble(),
                    node.path("maxTokens").asInt(),
                    List.copyOf(stop),
                    node.path("seed").asInt(),
                    node.path("streaming").asBoolean()));
        });
        return new ConfigVersao(raiz.path("versao").asText(versao), Map.copyOf(tarefas));
    }

    public ParametrosTarefa de(String tarefa) {
        ParametrosTarefa parametros = tarefas.get(tarefa);
        if (parametros == null) {
            throw new IllegalArgumentException("Parametros ausentes: " + tarefa);
        }
        return parametros;
    }
}
