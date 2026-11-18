package com.mundodev.filafacil.orquestracao;

import java.util.Locale;

public final class Roteador {

    private Roteador() {
    }

    public static Fluxo classificar(String texto) {
        String normal = normalizar(texto);
        if (normal.contains("embedding")) {
            return Fluxo.EMBEDDING;
        }
        if (normal.contains("reprocessar")) {
            return Fluxo.REPROCESSAMENTO;
        }
        if (normal.contains("lote")) {
            return Fluxo.LOTE;
        }
        if (normal.contains("resumir")) {
            return Fluxo.RESUMO;
        }
        if (normal.contains("previsao")) {
            return Fluxo.FORA;
        }
        if (normal.contains("atraso")
                || normal.contains("atrasad")
                || normal.contains("registrar ocorrencia")
                || normal.contains("plano inseguro")
                || normal.contains("impacto alto")) {
            return Fluxo.AGENTE;
        }
        if (normal.contains("triagem")) {
            return Fluxo.TRIAGEM;
        }
        if (normal.contains("classificar") || normal.contains("prioridade")) {
            return Fluxo.CLASSIFICADOR;
        }
        if (normal.contains("operador") || normal.contains("politica") || normal.contains("busca")) {
            return Fluxo.BUSCA;
        }
        if (normal.contains("cliente") || normal.contains("regra")) {
            return Fluxo.ATENDIMENTO;
        }
        return Fluxo.FORA;
    }

    static String normalizar(String texto) {
        String base = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        return base
                .replace("ç", "c")
                .replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                .replace("ú", "u");
    }
}
