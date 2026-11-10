package com.mundodev.filafacil.pipeline.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LayerWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private LayerWriter() {
    }

    public static Path writeJson(Path dir, String fileName, Object payload) throws Exception {
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        MAPPER.writeValue(target.toFile(), payload);
        return target;
    }

    public static Path writeLines(Path dir, String fileName, List<String> lines) throws Exception {
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        Files.write(target, lines, StandardCharsets.UTF_8);
        return target;
    }
}
