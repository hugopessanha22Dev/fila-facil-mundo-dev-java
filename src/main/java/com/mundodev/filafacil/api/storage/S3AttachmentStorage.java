package com.mundodev.filafacil.api.storage;

import com.mundodev.filafacil.api.error.ApiValidationException;
import com.mundodev.filafacil.api.error.PersistenceFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;

public class S3AttachmentStorage implements AttachmentStorage {

    private static final Logger log = LoggerFactory.getLogger(S3AttachmentStorage.class);

    private final S3Client s3;
    private final String bucket;
    private final long maxBytes;

    public S3AttachmentStorage(S3Client s3, String bucket, long maxBytes) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("FILAFACIL_S3_BUCKET e obrigatorio em modo prod.");
        }
        this.s3 = s3;
        this.bucket = bucket;
        this.maxBytes = maxBytes;
    }

    @Override
    public String store(long agendamentoId, String filename, InputStream content, long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new ApiValidationException("Anexo vazio.");
        }
        if (sizeBytes > maxBytes) {
            throw new ApiValidationException("Anexo excede o limite de " + maxBytes + " bytes.");
        }
        String safeName = sanitize(filename);
        String key = "agendamentos/" + agendamentoId + "/" + safeName;
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentLength(sizeBytes)
                    .build();
            s3.putObject(req, RequestBody.fromInputStream(content, sizeBytes));
            String ref = "s3://" + bucket + "/" + key;
            log.info("Anexo S3 salvo agendamentoId={} bytes={}", agendamentoId, sizeBytes);
            return ref;
        } catch (ApiValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao enviar anexo ao storage cloud.");
            throw new PersistenceFailureException(ex);
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
