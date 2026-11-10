package com.mundodev.filafacil.pipeline.inter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SourceAdapters {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SourceAdapters() {
    }

    public static List<SourceRecord> readAll(Path inputDir) throws Exception {
        List<SourceRecord> all = new ArrayList<>();
        all.addAll(readColetasApi(inputDir.resolve("coletas_api.json")));
        all.addAll(readClientes(inputDir.resolve("clientes.csv")));
        all.addAll(readEntregas(inputDir.resolve("entregas.csv")));
        all.addAll(readMotoristas(inputDir.resolve("motoristas_rotas.json")));
        return all;
    }

    public static List<SourceRecord> readSource(SourceId source, Path inputDir) throws Exception {
        return switch (source) {
            case API_COLETAS -> readColetasApi(inputDir.resolve("coletas_api.json"));
            case CLIENTES -> readClientes(inputDir.resolve("clientes.csv"));
            case ENTREGAS -> readEntregas(inputDir.resolve("entregas.csv"));
            case MOTORISTAS -> readMotoristas(inputDir.resolve("motoristas_rotas.json"));
        };
    }

    static List<SourceRecord> readColetasApi(Path file) throws Exception {
        List<Map<String, Object>> rows = MAPPER.readValue(file.toFile(), new TypeReference<>() {
        });
        List<SourceRecord> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            LocalDateTime ts = TimeParse.parse(str(row.get("updated_at")));
            String regiao = normalizeRegiao(str(row.get("regiao")));
            Map<String, Object> payload = new LinkedHashMap<>(row);
            out.add(new SourceRecord(
                    SourceId.API_COLETAS,
                    "coleta",
                    str(row.get("codigo_coleta")).toUpperCase(Locale.ROOT),
                    ts,
                    asInt(row.get("version")),
                    asBool(row.get("deleted")),
                    regiao,
                    TimeParse.dtOf(ts),
                    payload));
        }
        return out;
    }

    static List<SourceRecord> readClientes(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<SourceRecord> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split(",", -1);
            LocalDateTime ts = TimeParse.parse(get(p, 4));
            String codigo = get(p, 0).toUpperCase(Locale.ROOT);
            String regiao = normalizeRegiao(get(p, 3));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cliente_codigo", codigo);
            payload.put("cliente_nome", get(p, 1));
            payload.put("unidade", get(p, 2));
            payload.put("regiao", regiao);
            out.add(new SourceRecord(
                    SourceId.CLIENTES, "cliente", codigo, ts, parseInt(get(p, 5), 1),
                    false, regiao, TimeParse.dtOf(ts), payload));
        }
        return out;
    }

    static List<SourceRecord> readEntregas(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<SourceRecord> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] p = line.split(",", -1);
            LocalDateTime ts = TimeParse.parse(get(p, 6));
            String id = get(p, 0).toUpperCase(Locale.ROOT);
            String regiao = normalizeRegiao(get(p, 1));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id_entrega", id);
            payload.put("regiao", regiao);
            payload.put("status_final", get(p, 2));
            payload.put("atraso", get(p, 3));
            payload.put("cliente", get(p, 4));
            payload.put("unidade", get(p, 5));
            out.add(new SourceRecord(
                    SourceId.ENTREGAS, "entrega", id, ts, parseInt(get(p, 7), 1),
                    Boolean.parseBoolean(get(p, 8)), regiao, TimeParse.dtOf(ts), payload));
        }
        return out;
    }

    static List<SourceRecord> readMotoristas(Path file) throws Exception {
        List<Map<String, Object>> rows = MAPPER.readValue(file.toFile(), new TypeReference<>() {
        });
        List<SourceRecord> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            LocalDateTime ts = TimeParse.parse(str(row.get("updated_at")));
            String id = str(row.get("motorista_id")).toUpperCase(Locale.ROOT);
            String regiao = normalizeRegiao(str(row.get("regiao")));
            Map<String, Object> payload = new LinkedHashMap<>(row);
            out.add(new SourceRecord(
                    SourceId.MOTORISTAS, "motorista", id, ts, asInt(row.get("version")),
                    asBool(row.get("deleted")), regiao, TimeParse.dtOf(ts), payload));
        }
        return out;
    }

    public static List<SourceRecord> filterIncremental(List<SourceRecord> records, LocalDateTime afterExclusive) {
        List<SourceRecord> out = new ArrayList<>();
        for (SourceRecord r : records) {
            if (r.updatedAt().isAfter(afterExclusive)) {
                out.add(r);
            }
        }
        return out;
    }

    private static String normalizeRegiao(String regiao) {
        String r = regiao == null ? "" : regiao.trim().toUpperCase(Locale.ROOT).replace(' ', '-').replace('_', '-');
        return switch (r) {
            case "SE", "SUDESTE" -> "SUDESTE";
            case "S", "SUL" -> "SUL";
            case "CO", "CENTRO-OESTE", "CENTROOESTE" -> "CENTRO-OESTE";
            case "NE", "NORDESTE" -> "NORDESTE";
            case "N", "NORTE" -> "NORTE";
            default -> r.isEmpty() ? "DESCONHECIDA" : r;
        };
    }

    private static String get(String[] p, int i) {
        return i < p.length ? p[i].trim() : "";
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static int asInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        return parseInt(str(o), 1);
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return def;
        }
    }

    private static boolean asBool(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(str(o));
    }
}
