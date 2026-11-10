package com.mundodev.filafacil.pipeline.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.pipeline.model.ClienteUnidade;
import com.mundodev.filafacil.pipeline.model.ColetaRaw;
import com.mundodev.filafacil.pipeline.model.EntregaRaw;
import com.mundodev.filafacil.pipeline.model.OcorrenciaRaw;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SourceReaders {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SourceReaders() {
    }

    public static void requireFile(Path file, String label) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Fonte obrigatoria ausente (" + label + "): " + file);
        }
    }

    public static List<ColetaRaw> readColetasCsv(Path file) throws Exception {
        requireFile(file, "CSV coletas");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalStateException("CSV coletas vazio: " + file);
        }
        String header = lines.get(0).toLowerCase();
        if (!header.contains("codigo_coleta") || !header.contains("cliente")) {
            throw new IllegalStateException("CSV coletas com cabecalho invalido: " + header);
        }
        List<ColetaRaw> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split(",", -1);
            out.add(new ColetaRaw(
                    get(p, 0), get(p, 1), get(p, 2), get(p, 3),
                    get(p, 4), get(p, 5), get(p, 6), get(p, 7)));
        }
        return out;
    }

    public static List<OcorrenciaRaw> readOcorrenciasJson(Path file) throws Exception {
        requireFile(file, "JSON ocorrencias");
        List<Map<String, Object>> rows = MAPPER.readValue(file.toFile(), new TypeReference<>() {
        });
        List<OcorrenciaRaw> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(new OcorrenciaRaw(
                    str(row.get("tipo")),
                    str(row.get("descricao")),
                    str(row.get("referencia")),
                    str(row.get("data")),
                    str(row.get("responsavel")),
                    str(row.get("status_resolucao"))));
        }
        return out;
    }

    public static List<EntregaRaw> readEntregasParquet(Path file) throws Exception {
        requireFile(file, "Parquet entregas");
        return ParquetIO.readEntregas(file);
    }

    public static Map<String, ClienteUnidade> readClientesCsv(Path file) throws Exception {
        requireFile(file, "CSV clientes/unidades (fallback)");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Map<String, ClienteUnidade> out = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split(",", -1);
            ClienteUnidade c = new ClienteUnidade(get(p, 0), get(p, 1), get(p, 2), get(p, 3));
            out.put(c.clienteCodigo(), c);
        }
        return out;
    }

    private static String get(String[] p, int i) {
        return i < p.length ? p[i].trim() : "";
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
