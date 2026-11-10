package com.mundodev.filafacil.pipeline.inter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CheckpointStore {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public CheckpointStore(Path checkpointsDir) throws Exception {
        Files.createDirectories(checkpointsDir);
        this.file = checkpointsDir.resolve("checkpoints.json");
        if (!Files.exists(file)) {
            mapper.writeValue(file.toFile(), Map.of());
        }
    }

    public Optional<Checkpoint> get(String sourceId) throws Exception {
        Map<String, Checkpoint> all = load();
        return Optional.ofNullable(all.get(sourceId));
    }

    public LocalDateTime lastTsOrMin(String sourceId) throws Exception {
        return get(sourceId).map(Checkpoint::lastTs).orElse(LocalDateTime.MIN);
    }

    public synchronized void save(Checkpoint checkpoint) throws Exception {
        Map<String, Checkpoint> all = load();
        all.put(checkpoint.sourceId(), checkpoint);
        mapper.writeValue(file.toFile(), all);
    }

    public Map<String, Checkpoint> load() throws Exception {
        if (!Files.isRegularFile(file) || Files.size(file) == 0) {
            return new LinkedHashMap<>();
        }
        Map<String, Checkpoint> map = mapper.readValue(file.toFile(), new TypeReference<>() {
        });
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }
}
