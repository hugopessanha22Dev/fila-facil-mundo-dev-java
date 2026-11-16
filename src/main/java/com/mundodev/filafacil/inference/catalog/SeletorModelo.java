package com.mundodev.filafacil.inference.catalog;

public final class SeletorModelo {

    private final CatalogoModelos modelos;

    public SeletorModelo(CatalogoModelos modelos) {
        this.modelos = modelos;
    }

    public static SeletorModelo carregar() {
        return new SeletorModelo(CatalogoModelos.carregar());
    }

    public Selecao escolher(Tarefa tarefa) {
        boolean robusto = tarefa.toolUse()
                || "alto".equals(tarefa.risco())
                || "longa".equals(tarefa.janela());
        return Selecao.aceita(modelos.porId(robusto ? "robusto" : "barato"));
    }

    public Selecao fallback(Tarefa tarefa, String modeloQueFalhou) {
        String reservaId = "barato".equals(modeloQueFalhou) ? "robusto" : "barato";
        Modelo reserva = modelos.porId(reservaId);
        if (rank(reserva.privacidade()) < rank(tarefa.privacidade())) {
            return Selecao.recusada("privacidade insuficiente");
        }
        return Selecao.aceita(reserva);
    }

    static int rank(String privacidade) {
        return switch (privacidade) {
            case "baixa" -> 1;
            case "media" -> 2;
            case "alta" -> 3;
            default -> 0;
        };
    }
}
