package com.mundodev.filafacil.error;

public class ApiException extends AppException {

    public ApiException(String message) {
        super("api", message);
    }

    public ApiException(String message, Throwable cause) {
        super("api", message, cause);
    }
}
