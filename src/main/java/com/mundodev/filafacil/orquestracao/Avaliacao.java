package com.mundodev.filafacil.orquestracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Avaliacao {

    private Avaliacao() {
    }

    public static Relatorio avaliar(Path raiz) throws IOException {
        Orquestrador orquestrador = Orquestrador.criar(raiz);
        List<Item> itens = new ArrayList<>();
        int inventadas = 0;
        boolean classificadorSeguiu = false;
        for (Caso caso : casos()) {
            Registro registro = orquestrador.executar(caso.texto(), caso.vetorIndisponivel(), caso.falhasModelo());
            boolean bateu = caso.fluxo().equals(registro.fluxo());
            boolean inventada = caso.ausencia() && !Orquestrador.AUSENCIA.equals(registro.resposta());
            if (inventada) {
                inventadas++;
            }
            if ("classificador-vetor".equals(caso.id())) {
                classificadorSeguiu = "concluido".equals(registro.status()) && "classificador".equals(registro.fluxo());
            }
            itens.add(new Item(caso.id(), caso.fluxo(), registro.fluxo(), registro.status(), registro.fallback(), inventada, bateu));
        }
        boolean fluxos = itens.stream().allMatch(Item::bateu);
        return new Relatorio(itens, inventadas, fluxos && inventadas == 0 && classificadorSeguiu);
    }

    public static void escrever(Path destino, Relatorio relatorio) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode raiz = mapper.createObjectNode();
        raiz.put("inventadas", relatorio.inventadas());
        raiz.put("aprovado", relatorio.aprovado());
        ArrayNode casos = raiz.putArray("casos");
        for (Item item : relatorio.itens()) {
            ObjectNode node = casos.addObject();
            node.put("id", item.id());
            node.put("esperado", item.esperado());
            node.put("obtido", item.obtido());
            node.put("status", item.status());
            node.put("fallback", item.fallback());
            node.put("inventada", item.inventada());
        }
        Files.createDirectories(destino.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(destino.toFile(), raiz);
    }

    private static List<Caso> casos() throws IOException {
        try (InputStream input = Avaliacao.class.getResourceAsStream("/orquestracao/casos.json")) {
            if (input == null) {
                throw new IllegalStateException("Dataset da orquestracao ausente.");
            }
            JsonNode raiz = new ObjectMapper().readTree(input);
            List<Caso> casos = new ArrayList<>();
            for (JsonNode node : raiz) {
                casos.add(new Caso(
                        node.path("id").asText(),
                        node.path("texto").asText(),
                        node.path("fluxo").asText(),
                        node.path("vetorIndisponivel").asBoolean(false),
                        node.path("falhasModelo").asInt(0),
                        node.path("ausencia").asBoolean(false)));
            }
            return casos;
        }
    }

    public record Caso(String id, String texto, String fluxo, boolean vetorIndisponivel, int falhasModelo, boolean ausencia) {
    }

    public record Item(
            String id,
            String esperado,
            String obtido,
            String status,
            boolean fallback,
            boolean inventada,
            boolean bateu) {
    }

    public record Relatorio(List<Item> itens, int inventadas, boolean aprovado) {
    }
}
