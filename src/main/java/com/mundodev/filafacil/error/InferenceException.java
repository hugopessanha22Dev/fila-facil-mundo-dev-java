package com.mundodev.filafacil.error;

public class InferenceException extends AppException {

    public InferenceException(String message) {
        super("inference", message);
    }

    public InferenceException(String message, Throwable cause) {
        super("inference", message, cause);
    }
}
