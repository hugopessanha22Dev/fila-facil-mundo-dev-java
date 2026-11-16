package com.mundodev.filafacil.inference.prompt;

public record ResultadoCadeia(boolean aceita, boolean bloqueada, String textoAceito, String motivo) {

    public static ResultadoCadeia aceita(String texto) {
        return new ResultadoCadeia(true, false, texto, "aceita");
    }

    public static ResultadoCadeia recusada(String motivo) {
        return new ResultadoCadeia(false, false, null, motivo);
    }

    public static ResultadoCadeia bloqueada(String motivo) {
        return new ResultadoCadeia(false, true, null, motivo);
    }
}
