package com.mundodev.filafacil.pipeline.inter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConsolidatedStore {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public ConsolidatedStore(Path consolidadaDir) throws Exception {
        Files.createDirectories(consolidadaDir);
        this.file = consolidadaDir.resolve("state.json");
        if (!Files.exists(file)) {
            mapper.writeValue(file.toFile(), List.of());
        }
    }

    public Map<String, SourceRecord> load() throws Exception {
        if (!Files.isRegularFile(file) || Files.size(file) == 0) {
            return new LinkedHashMap<>();
        }
        List<Map<String, Object>> rows = mapper.readValue(file.toFile(), new TypeReference<>() {
        });
        Map<String, SourceRecord> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            SourceRecord r = fromMap(row);
            out.put(CdcClassifier.key(r), r);
        }
        return out;
    }

    public void save(List<SourceRecord> records) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SourceRecord r : records) {
            rows.add(r.toMap());
        }
        mapper.writeValue(file.toFile(), rows);
    }

    public Path file() {
        return file;
    }

    private static SourceRecord fromMap(Map<String, Object> row) {
        String sourceId = String.valueOf(row.get("source"));
        SourceId sid = switch (sourceId) {
            case "api_coletas" -> SourceId.API_COLETAS;
            case "clientes" -> SourceId.CLIENTES;
            case "entregas" -> SourceId.ENTREGAS;
            case "motoristas" -> SourceId.MOTORISTAS;
            default -> {
                try {
                    yield SourceId.valueOf(sourceId.toUpperCase());
                } catch (Exception ex) {
                    yield SourceId.API_COLETAS;
                }
            }
        };
        LocalDateTime ts = TimeParse.parse(String.valueOf(row.get("updated_at")));
        boolean deleted = Boolean.parseBoolean(String.valueOf(row.getOrDefault("deleted", false)));
        int version = row.get("version") instanceof Number n ? n.intValue() : 1;
        return new SourceRecord(
                sid,
                String.valueOf(row.getOrDefault("entity", "")),
                String.valueOf(row.getOrDefault("business_key", "")),
                ts,
                version,
                deleted,
                String.valueOf(row.getOrDefault("regiao", "DESCONHECIDA")),
                String.valueOf(row.getOrDefault("dt", TimeParse.dtOf(ts))),
                new LinkedHashMap<>(row));
    }
}
