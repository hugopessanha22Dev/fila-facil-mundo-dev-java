package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class AvaliacaoAdversarial {

    private static final List<String> ARQUIVOS = List.of(
            "ambiguo.json",
            "incompleto.json",
            "longo.json",
            "fora-de-escopo.json");

    public RelatorioAdversarial avaliar() {
        List<String> casos = new ArrayList<>();
        boolean fora = false;
        for (String arquivo : ARQUIVOS) {
            JsonNode node = Recursos.ler("/inference/adversarial/" + arquivo);
            String id = node.path("id").asText();
            String esperado = node.path("esperado").asText();
            casos.add(id + ":" + esperado);
            if ("fora_de_escopo".equals(esperado)) {
                fora = true;
            }
        }
        return new RelatorioAdversarial(List.copyOf(casos), fora);
    }
}
