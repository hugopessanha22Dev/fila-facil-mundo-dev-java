package com.mundodev.filafacil.error;

public abstract class AppException extends RuntimeException {

    private final String origem;

    protected AppException(String origem, String message) {
        super(message);
        this.origem = origem;
    }

    protected AppException(String origem, String message, Throwable cause) {
        super(message, cause);
        this.origem = origem;
    }

    public String getOrigem() {
        return origem;
    }
}
