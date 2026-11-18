package com.mundodev.filafacil.aiops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class GateRelease {

    private GateRelease() {
    }

    public record Medidas(double groundedness, double formato, double ferramenta, double seguranca, double custo, double relevancia) {
    }

    public record Decisao(String status, boolean promovido, boolean alerta, List<String> falhos, String versao) {
    }

    public static Decisao avaliar(String cenario, String decisaoHumana) {
        List<Caso> casos = carregar().stream().filter(caso -> caso.cenario().equals(cenario)).toList();
        List<String> falhos = new ArrayList<>();
        boolean alerta = false;
        String versao = casos.isEmpty() ? "" : casos.get(0).versao();
        boolean ambiguo = false;
        for (Caso caso : casos) {
            ambiguo = ambiguo || caso.ambiguo();
            if (pioraBloqueante(caso)) {
                falhos.add(caso.id());
            } else if (caso.candidata().relevancia() < caso.atual().relevancia()) {
                alerta = true;
            }
        }
        if (!falhos.isEmpty()) {
            return new Decisao("bloqueado", false, false, List.copyOf(falhos), versao);
        }
        if (ambiguo && (decisaoHumana == null || decisaoHumana.isBlank())) {
            return new Decisao("revisao", false, false, List.of(), versao);
        }
        return new Decisao("aprovado", true, alerta, List.of(), versao);
    }

    private static boolean pioraBloqueante(Caso caso) {
        Medidas atual = caso.atual();
        Medidas candidata = caso.candidata();
        double tetoCusto = CatalogoFluxos.carregar().obter("classificador").metrica("custo").limite();
        return candidata.groundedness() < atual.groundedness()
                || candidata.formato() < atual.formato()
                || candidata.ferramenta() < atual.ferramenta()
                || candidata.seguranca() < atual.seguranca()
                || candidata.custo() > tetoCusto;
    }

    private static List<Caso> carregar() {
        try (InputStream input = GateRelease.class.getResourceAsStream("/aiops/golden.json")) {
            if (input == null) {
                throw new IllegalStateException("Golden de AI Ops ausente.");
            }
            JsonNode raiz = new ObjectMapper().readTree(input);
            List<Caso> casos = new ArrayList<>();
            for (JsonNode node : raiz) {
                casos.add(new Caso(
                        node.path("id").asText(),
                        node.path("cenario").asText(),
                        node.path("versao").asText(),
                        node.path("ambiguo").asBoolean(false),
                        medidas(node.path("atual")),
                        medidas(node.path("candidata"))));
            }
            return casos;
        } catch (IOException ex) {
            throw new IllegalStateException("Golden de AI Ops invalido.", ex);
        }
    }

    private static Medidas medidas(JsonNode node) {
        return new Medidas(
                node.path("groundedness").asDouble(),
                node.path("formato").asDouble(),
                node.path("ferramenta").asDouble(),
                node.path("seguranca").asDouble(),
                node.path("custo").asDouble(),
                node.path("relevancia").asDouble());
    }

    private record Caso(String id, String cenario, String versao, boolean ambiguo, Medidas atual, Medidas candidata) {
    }
}
