package com.mundodev.filafacil.inference.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RetomadaSessao {

    private final ObjectMapper mapper = new ObjectMapper();

    public void gravar(Path diretorio, EstadoSessao estado) {
        try {
            Files.createDirectories(diretorio);
            mapper.writerWithDefaultPrettyPrinter().writeValue(diretorio.resolve(estado.id() + ".json").toFile(), estado);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gravar sessao de prompt", ex);
        }
    }

    public EstadoSessao carregar(Path diretorio, String id) {
        try {
            return mapper.readValue(diretorio.resolve(id + ".json").toFile(), EstadoSessao.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler sessao de prompt", ex);
        }
    }

    public boolean aceitaComoDocumento(EvidenciaPrompt evidencia) {
        return FontesAutorizadas.documental(evidencia);
    }

    public EstadoSessao semMemoriaComoFonte(EstadoSessao estado) {
        List<EvidenciaPrompt> recebidas = estado.evidencias() == null ? List.of() : estado.evidencias();
        List<EvidenciaPrompt> documentais = new ArrayList<>();
        for (EvidenciaPrompt evidencia : recebidas) {
            if (aceitaComoDocumento(evidencia)) {
                documentais.add(evidencia);
            }
        }
        return new EstadoSessao(estado.id(), estado.intencao(), estado.pendentes(), List.copyOf(documentais), estado.historico());
    }

    public String resumo(EstadoSessao estado) {
        EstadoSessao limpo = semMemoriaComoFonte(estado);
        StringBuilder sb = new StringBuilder();
        sb.append("intencao=").append(limpo.intencao()).append('\n');
        List<String> pendentes = limpo.pendentes() == null ? List.of() : limpo.pendentes();
        sb.append("pendentes=").append(String.join(",", pendentes)).append('\n');
        sb.append("evidencias=");
        for (EvidenciaPrompt evidencia : limpo.evidencias()) {
            sb.append(evidencia.texto()).append(' ');
        }
        return sb.toString().trim();
    }
}
