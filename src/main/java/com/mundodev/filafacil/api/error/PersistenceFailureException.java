package com.mundodev.filafacil.api.error;

/**
 * Erro de persistencia com mensagem publica segura (sem credenciais).
 */
public class PersistenceFailureException extends RuntimeException {

    public static final String PUBLIC_MESSAGE = "Falha de persistencia. Tente novamente mais tarde.";

    public PersistenceFailureException(Throwable cause) {
        super(PUBLIC_MESSAGE, cause);
    }

    public PersistenceFailureException() {
        super(PUBLIC_MESSAGE);
    }
}
