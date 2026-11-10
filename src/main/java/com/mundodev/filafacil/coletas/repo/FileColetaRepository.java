package com.mundodev.filafacil.coletas.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mundodev.filafacil.coletas.Coleta;
import com.mundodev.filafacil.error.ArquivoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileColetaRepository implements ColetaRepository {

    private final Path arquivo;
    private final ObjectMapper mapper;

    public FileColetaRepository(Path arquivo) {
        this.arquivo = arquivo;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public List<Coleta> carregar() {
        if (!Files.exists(arquivo)) {
            return new ArrayList<>();
        }
        try {
            if (Files.size(arquivo) == 0) {
                return new ArrayList<>();
            }
            List<Coleta> coletas = mapper.readValue(arquivo.toFile(), new TypeReference<List<Coleta>>() {});
            return coletas == null ? new ArrayList<>() : new ArrayList<>(coletas);
        } catch (IOException ex) {
            throw new ArquivoException("Falha ao ler arquivo de coletas: " + arquivo, ex);
        }
    }

    @Override
    public void salvar(List<Coleta> coletas) {
        try {
            Path parent = arquivo.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writeValue(arquivo.toFile(), coletas);
        } catch (IOException ex) {
            throw new ArquivoException("Falha ao gravar arquivo de coletas: " + arquivo, ex);
        }
    }

    public Path getArquivo() {
        return arquivo;
    }
}
