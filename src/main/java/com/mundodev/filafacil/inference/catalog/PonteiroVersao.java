package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class PonteiroVersao {

    private final Map<String, ConfigVersao> versoes;
    private String ativa;
    private String anterior;

    public PonteiroVersao(Map<String, ConfigVersao> versoes, String ativa, String anterior) {
        this.versoes = Map.copyOf(versoes);
        this.ativa = ativa;
        this.anterior = anterior;
    }

    public static PonteiroVersao carregar() {
        return new PonteiroVersao(
                Map.of("v1", ConfigVersao.carregar("v1"), "v2", ConfigVersao.carregar("v2")),
                "v1",
                null);
    }

    public String ativa() {
        return ativa;
    }

    public String anterior() {
        return anterior;
    }

    public ConfigVersao configAtiva() {
        return versoes.get(ativa);
    }

    public boolean registrada(String versao) {
        return versoes.containsKey(versao);
    }

    public void promover(String versao) {
        if (!versoes.containsKey(versao)) {
            throw new IllegalArgumentException("Versao ausente: " + versao);
        }
        if (versao.equals(ativa)) {
            return;
        }
        anterior = ativa;
        ativa = versao;
    }

    public void rollback() {
        if (anterior == null) {
            return;
        }
        String deixada = ativa;
        ativa = anterior;
        anterior = deixada;
    }

    public void gravar(Path arquivo) {
        try {
            Files.createDirectories(arquivo.getParent());
            ObjectNode node = Recursos.JSON.createObjectNode();
            node.put("ativa", ativa);
            if (anterior == null) {
                node.putNull("anterior");
            } else {
                node.put("anterior", anterior);
            }
            Recursos.JSON.writerWithDefaultPrettyPrinter().writeValue(arquivo.toFile(), node);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gravar ponteiro", ex);
        }
    }

    public static PonteiroVersao carregar(Path arquivo) {
        PonteiroVersao ponteiro = carregar();
        if (arquivo == null || !Files.exists(arquivo)) {
            return ponteiro;
        }
        try {
            JsonNode node = Recursos.JSON.readTree(arquivo.toFile());
            String ativa = node.path("ativa").asText("v1");
            String anterior = node.path("anterior").isNull() ? null : node.path("anterior").asText(null);
            if (ponteiro.registrada(ativa)) {
                ponteiro.ativa = ativa;
                ponteiro.anterior = anterior;
            }
            return ponteiro;
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler ponteiro", ex);
        }
    }
}
