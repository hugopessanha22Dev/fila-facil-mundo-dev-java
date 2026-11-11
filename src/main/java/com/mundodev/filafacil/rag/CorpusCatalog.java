package com.mundodev.filafacil.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CorpusCatalog {

    public static final Set<String> DOMINIOS = Set.of(
            "coletas", "entregas", "ocorrencias", "atendimento", "comprovantes", "atraso", "parceiros");

    private static final List<String> FILES = List.of(
            "/rag/corpus/politica-coletas.json",
            "/rag/corpus/politica-entregas.json",
            "/rag/corpus/manual-ocorrencias.json",
            "/rag/corpus/faq-atendimento.json",
            "/rag/corpus/regras-comprovantes.json",
            "/rag/corpus/procedimento-atraso.json",
            "/rag/corpus/instrucoes-parceiros.json");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<SourceDocument> load() {
        List<SourceDocument> docs = new ArrayList<>();
        for (String path : FILES) {
            try (InputStream in = CorpusCatalog.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("Corpus ausente: " + path);
                }
                docs.add(parse(MAPPER.readTree(in)));
            } catch (IOException ex) {
                throw new IllegalStateException("Falha ao ler " + path, ex);
            }
        }
        return List.copyOf(docs);
    }

    public SourceDocument parse(String json) {
        try {
            return parse(MAPPER.readTree(json));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new IllegalArgumentException("documento invalido", ex);
        }
    }

    public SourceDocument find(String id) {
        return load().stream()
                .filter(doc -> doc.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("documento ausente: " + id));
    }

    private SourceDocument parse(JsonNode node) {
        String dominio = node.path("dominio").asText();
        if (!DOMINIOS.contains(dominio)) {
            throw new IllegalArgumentException("familia desconhecida: " + dominio);
        }
        List<SourceDocument.Section> secoes = new ArrayList<>();
        for (JsonNode secao : node.path("secoes")) {
            secoes.add(new SourceDocument.Section(secao.path("nome").asText(), secao.path("texto").asText()));
        }
        return new SourceDocument(
                node.path("id").asText(),
                node.path("titulo").asText(),
                node.path("tipo").asText(),
                dominio,
                node.path("versao").asText(),
                node.path("dataAtualizacao").asText(),
                node.path("owner").asText(),
                node.path("acesso").asText(),
                List.copyOf(secoes));
    }
}
