package com.mundodev.filafacil.error;

public class EntradaException extends AppException {

    public EntradaException(String message) {
        super("entrada", message);
    }
}
