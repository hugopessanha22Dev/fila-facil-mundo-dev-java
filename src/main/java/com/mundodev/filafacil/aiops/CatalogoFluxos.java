package com.mundodev.filafacil.aiops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CatalogoFluxos(List<Fluxo> fluxos) {

    public record Metrica(String nome, String regra, double limite, String dono, String consequencia) {
    }

    public record Fluxo(
            String id,
            String dono,
            String risco,
            String modelo,
            String prompt,
            String entrada,
            String saida,
            List<String> ferramentas,
            Map<String, List<String>> argumentos,
            String fonte,
            List<Metrica> metricas) {

        public Metrica metrica(String nome) {
            for (Metrica metrica : metricas) {
                if (metrica.nome().equals(nome)) {
                    return metrica;
                }
            }
            return null;
        }
    }

    public static CatalogoFluxos carregar() {
        try (InputStream input = CatalogoFluxos.class.getResourceAsStream("/aiops/fluxos.json")) {
            if (input == null) {
                throw new IllegalStateException("Catalogo de fluxos ausente.");
            }
            JsonNode raiz = new ObjectMapper().readTree(input);
            List<Fluxo> fluxos = new ArrayList<>();
            for (JsonNode node : raiz.path("fluxos")) {
                Map<String, List<String>> argumentos = new LinkedHashMap<>();
                node.path("argumentos").fields().forEachRemaining(entry -> {
                    List<String> campos = new ArrayList<>();
                    entry.getValue().forEach(campo -> campos.add(campo.asText()));
                    argumentos.put(entry.getKey(), List.copyOf(campos));
                });
                List<String> ferramentas = new ArrayList<>();
                node.path("ferramentas").forEach(item -> ferramentas.add(item.asText()));
                List<Metrica> metricas = new ArrayList<>();
                for (JsonNode metrica : node.path("metricas")) {
                    metricas.add(new Metrica(
                            metrica.path("nome").asText(),
                            metrica.path("regra").asText(),
                            metrica.path("limite").asDouble(),
                            metrica.path("dono").asText(),
                            metrica.path("consequencia").asText()));
                }
                fluxos.add(new Fluxo(
                        node.path("id").asText(),
                        node.path("dono").asText(),
                        node.path("risco").asText(),
                        node.path("modelo").asText(),
                        node.path("prompt").asText(),
                        node.path("entrada").asText(),
                        node.path("saida").asText(),
                        List.copyOf(ferramentas),
                        Map.copyOf(argumentos),
                        node.path("fonte").asText(),
                        List.copyOf(metricas)));
            }
            return new CatalogoFluxos(List.copyOf(fluxos));
        } catch (IOException ex) {
            throw new IllegalStateException("Catalogo de fluxos invalido.", ex);
        }
    }

    public Fluxo obter(String id) {
        for (Fluxo fluxo : fluxos) {
            if (fluxo.id().equals(id)) {
                return fluxo;
            }
        }
        throw new IllegalArgumentException("Fluxo ausente: " + id);
    }

    public String consequencia(String fluxo, String metrica, double valor) {
        Metrica item = obter(fluxo).metrica(metrica);
        if (item == null) {
            return "ausente";
        }
        return valor < item.limite() ? item.consequencia() : "ok";
    }
}
