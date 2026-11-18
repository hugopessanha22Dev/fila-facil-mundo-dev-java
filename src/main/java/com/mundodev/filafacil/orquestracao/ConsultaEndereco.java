package com.mundodev.filafacil.orquestracao;

public final class ConsultaEndereco {

    private final int limite;
    private int usadas;

    public ConsultaEndereco(int limite) {
        this.limite = limite;
    }

    public Resultado consultar(String codigo) {
        if (usadas >= limite) {
            return new Resultado(false, "", "limite");
        }
        usadas++;
        if (codigo == null || codigo.isBlank()) {
            return new Resultado(false, "", "argumento");
        }
        return new Resultado(true, "endereco simulado " + codigo, "");
    }

    public record Resultado(boolean ok, String pagina, String erro) {
    }
}
