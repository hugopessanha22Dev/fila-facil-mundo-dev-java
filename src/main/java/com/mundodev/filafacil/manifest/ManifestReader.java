package com.mundodev.filafacil.manifest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.ArquivoException;
import com.mundodev.filafacil.error.EntradaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManifestReader {

    private final ObjectMapper mapper;

    public ManifestReader() {
        this(new ObjectMapper());
    }

    public ManifestReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<ManifestRecord> read(Path arquivo) {
        if (arquivo == null) {
            throw new EntradaException("Caminho do manifesto e obrigatorio.");
        }
        if (!Files.exists(arquivo)) {
            throw new ArquivoException("Manifesto nao encontrado: " + arquivo);
        }
        String name = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".json")) {
                return readJson(arquivo);
            }
            if (name.endsWith(".csv")) {
                return readCsv(arquivo);
            }
            throw new EntradaException("Formato nao suportado. Use .json ou .csv: " + arquivo);
        } catch (ArquivoException | EntradaException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ArquivoException("Falha ao ler manifesto: " + arquivo, ex);
        }
    }

    private List<ManifestRecord> readJson(Path arquivo) throws IOException {
        List<ManifestRecord> records = mapper.readValue(
                arquivo.toFile(), new TypeReference<List<ManifestRecord>>() {});
        return records == null ? List.of() : List.copyOf(records);
    }

    private List<ManifestRecord> readCsv(Path arquivo) throws IOException {
        List<String> lines = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return List.of();
        }
        String header = lines.get(0).trim().toLowerCase(Locale.ROOT);
        if (!header.contains("pacote") || !header.contains("rota")) {
            throw new EntradaException("CSV deve ter cabecalho: pacote,rota,veiculo,responsavel,status");
        }
        List<ManifestRecord> records = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 5) {
                throw new EntradaException("Linha CSV incompleta (" + (i + 1) + "): " + line);
            }
            records.add(new ManifestRecord(
                    blankToNull(parts[0]),
                    blankToNull(parts[1]),
                    blankToNull(parts[2]),
                    blankToNull(parts[3]),
                    blankToNull(parts[4])));
        }
        return List.copyOf(records);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
