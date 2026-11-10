package com.mundodev.filafacil.manifest;

import com.mundodev.filafacil.error.ArquivoException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ManifestValidationService {

    private final ManifestReader reader;
    private final ManifestValidator validator;

    public ManifestValidationService() {
        this(new ManifestReader(), new ManifestValidator());
    }

    public ManifestValidationService(ManifestReader reader, ManifestValidator validator) {
        this.reader = reader;
        this.validator = validator;
    }

    public ValidationReport validar(Path arquivo) {
        List<ManifestRecord> records = reader.read(arquivo);
        return validator.validate(records);
    }

    public void salvarRelatorio(ValidationReport report, Path destino) {
        try {
            Path parent = destino.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(destino, report.render(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ArquivoException("Falha ao gravar relatorio: " + destino, ex);
        }
    }
}
