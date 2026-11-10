package com.mundodev.filafacil.api.storage;

import java.io.InputStream;

public interface AttachmentStorage {

    /**
     * Persiste o anexo e retorna a referencia logica (path ou s3 key).
     */
    String store(long agendamentoId, String filename, InputStream content, long sizeBytes);
}
