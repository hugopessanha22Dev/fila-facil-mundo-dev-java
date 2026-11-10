package com.mundodev.filafacil.api.storage;

import com.mundodev.filafacil.api.error.ApiValidationException;
import com.mundodev.filafacil.api.error.PersistenceFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class LocalFileAttachmentStorage implements AttachmentStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileAttachmentStorage.class);

    private final Path baseDir;
    private final long maxBytes;

    public LocalFileAttachmentStorage(Path baseDir, long maxBytes) {
        this.baseDir = baseDir;
        this.maxBytes = maxBytes;
    }

    @Override
    public String store(long agendamentoId, String filename, InputStream content, long sizeBytes) {
        validateSize(sizeBytes);
        String safeName = sanitize(filename);
        Path dir = baseDir.resolve("agendamentos").resolve(String.valueOf(agendamentoId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(safeName);
            Files.copy(content, target);
            String ref = "file://" + target.toAbsolutePath().normalize();
            log.info("Anexo local salvo agendamentoId={} bytes={}", agendamentoId, sizeBytes);
            return ref;
        } catch (IOException ex) {
            log.error("Falha ao gravar anexo local.");
            throw new PersistenceFailureException(ex);
        }
    }

    private void validateSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new ApiValidationException("Anexo vazio.");
        }
        if (sizeBytes > maxBytes) {
            throw new ApiValidationException("Anexo excede o limite de " + maxBytes + " bytes.");
        }
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "anexo.bin";
        }
        String name = Path.of(filename).getFileName().toString().toLowerCase(Locale.ROOT);
        name = name.replaceAll("[^a-z0-9._-]", "_");
        return name.isBlank() ? "anexo.bin" : name;
    }
}
