package com.mundodev.filafacil.orquestracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public record Contratos(List<Integracao> integracoes) {

    public record Integracao(
            String nome,
            String entrada,
            String saida,
            String erro,
            int timeoutMs,
            int tentativas,
            String acesso,
            int limiteUso) {
    }

    public static Contratos carregar() {
        try (InputStream input = Contratos.class.getResourceAsStream("/orquestracao/contratos.json")) {
            if (input == null) {
                throw new IllegalStateException("Contratos da orquestracao ausentes.");
            }
            JsonNode raiz = new ObjectMapper().readTree(input);
            List<Integracao> itens = new ArrayList<>();
            for (JsonNode node : raiz.path("integracoes")) {
                itens.add(new Integracao(
                        node.path("nome").asText(),
                        node.path("entrada").asText(),
                        node.path("saida").asText(),
                        node.path("erro").asText(),
                        node.path("timeoutMs").asInt(),
                        node.path("tentativas").asInt(),
                        node.path("acesso").asText(),
                        node.path("limiteUso").asInt()));
            }
            return new Contratos(List.copyOf(itens));
        } catch (IOException ex) {
            throw new IllegalStateException("Contratos da orquestracao invalidos.", ex);
        }
    }

    public Integracao obter(String nome) {
        for (Integracao item : integracoes) {
            if (item.nome().equals(nome)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Integracao ausente: " + nome);
    }
}
