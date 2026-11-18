package com.mundodev.filafacil.aiops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MesaIncidentes {

    private final Path raiz;
    private final List<Alerta> alertas = new ArrayList<>();
    private final List<Incidente> incidentes = new ArrayList<>();
    private final Set<String> bloqueados = new LinkedHashSet<>();

    public MesaIncidentes(Path raiz) {
        this.raiz = raiz;
    }

    public Alerta sinal(String fluxo, String tipo, String causa, double valor) {
        CatalogoFluxos.Metrica metrica = CatalogoFluxos.carregar().obter(fluxo).metrica(tipo);
        boolean acima = metrica != null && valor > metrica.limite();
        Alerta alerta = new Alerta(fluxo, tipo, causa, valor, acima);
        if (acima) {
            alertas.add(alerta);
            gravar();
        }
        return alerta;
    }

    public Incidente abrir(String fluxo, String severidade, String dono, String mitigacao, boolean rollback, String causa, String preventiva) {
        Incidente incidente = new Incidente(fluxo, severidade, dono, mitigacao, rollback, causa, preventiva);
        incidentes.add(incidente);
        bloqueados.add(fluxo);
        gravar();
        return incidente;
    }

    public boolean bloqueado(String fluxo) {
        return bloqueados.contains(fluxo);
    }

    public String executar(String fluxo) {
        return bloqueados.contains(fluxo) ? "recusado" : "aceito";
    }

    public List<Alerta> alertas() {
        return List.copyOf(alertas);
    }

    private void gravar() {
        try {
            Files.createDirectories(raiz);
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode raizJson = mapper.createObjectNode();
            ArrayNode alertasJson = raizJson.putArray("alertas");
            for (Alerta alerta : alertas) {
                ObjectNode node = alertasJson.addObject();
                node.put("fluxo", alerta.fluxo());
                node.put("tipo", alerta.tipo());
                node.put("causa", alerta.causa());
                node.put("valor", alerta.valor());
            }
            ArrayNode incidentesJson = raizJson.putArray("incidentes");
            for (Incidente incidente : incidentes) {
                ObjectNode node = incidentesJson.addObject();
                node.put("fluxo", incidente.fluxo());
                node.put("severidade", incidente.severidade());
                node.put("dono", incidente.dono());
                node.put("mitigacao", incidente.mitigacao());
                node.put("rollback", incidente.rollback());
                node.put("causa", incidente.causa());
                node.put("preventiva", incidente.preventiva());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(raiz.resolve("incidentes.json").toFile(), raizJson);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gravar incidentes.", ex);
        }
    }

    public record Alerta(String fluxo, String tipo, String causa, double valor, boolean disparado) {
    }

    public record Incidente(
            String fluxo,
            String severidade,
            String dono,
            String mitigacao,
            boolean rollback,
            String causa,
            String preventiva) {
    }
}
