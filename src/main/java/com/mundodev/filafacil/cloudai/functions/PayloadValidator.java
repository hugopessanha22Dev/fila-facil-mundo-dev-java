package com.mundodev.filafacil.cloudai.functions;

public final class PayloadValidator {

    public record Result(boolean valid, String reason) {
    }

    private PayloadValidator() {
    }

    public static Result validate(String text, String origin) {
        if (text == null || text.isBlank()) {
            return new Result(false, "texto de solicitacao ausente");
        }
        if (origin == null || origin.isBlank()) {
            return new Result(false, "origem simulada ausente");
        }
        return new Result(true, "");
    }
}
