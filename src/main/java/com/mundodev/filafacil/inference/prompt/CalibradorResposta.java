package com.mundodev.filafacil.inference.prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CalibradorResposta {

    public static final List<String> CAMPOS = List.of("prazo", "status", "responsavel", "politica", "codigo");

    public Calibracao calibrar(
            String mensagem,
            Map<String, FieldValue> sessao,
            List<EvidenciaPrompt> evidencias,
            Map<String, String> afirmacoes) {
        String corpus = corpus(mensagem, sessao, evidencias);
        Map<String, String> fatos = new LinkedHashMap<>();
        boolean incerteza = false;
        for (String campo : CAMPOS) {
            String valor = afirmacoes.get(campo);
            if (valor == null || valor.isBlank()) {
                continue;
            }
            if (corpus.contains(valor.toLowerCase(Locale.ROOT))) {
                fatos.put(campo, valor);
            } else {
                incerteza = true;
            }
        }
        String texto = incerteza ? "incerteza: sem base suficiente" : "fatos confirmados";
        return new Calibracao(texto, incerteza, Map.copyOf(fatos));
    }

    private static String corpus(String mensagem, Map<String, FieldValue> sessao, List<EvidenciaPrompt> evidencias) {
        StringBuilder sb = new StringBuilder();
        if (mensagem != null) {
            sb.append(mensagem.toLowerCase(Locale.ROOT)).append('\n');
        }
        if (sessao != null) {
            sessao.forEach((nome, valor) -> {
                if (valor != null && "fato".equals(valor.origem()) && valor.valor() != null) {
                    sb.append(valor.valor().toLowerCase(Locale.ROOT)).append('\n');
                }
            });
        }
        if (evidencias != null) {
            for (EvidenciaPrompt evidencia : evidencias) {
                if (FontesAutorizadas.documental(evidencia)) {
                    sb.append(evidencia.texto().toLowerCase(Locale.ROOT)).append('\n');
                }
            }
        }
        return sb.toString();
    }
}
