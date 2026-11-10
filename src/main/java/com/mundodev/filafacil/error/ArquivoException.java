package com.mundodev.filafacil.error;

public class ArquivoException extends AppException {

    public ArquivoException(String message, Throwable cause) {
        super("arquivo", message, cause);
    }

    public ArquivoException(String message) {
        super("arquivo", message);
    }
}
