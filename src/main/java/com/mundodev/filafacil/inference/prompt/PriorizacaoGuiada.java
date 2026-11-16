package com.mundodev.filafacil.inference.prompt;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PriorizacaoGuiada {

    public static final List<String> NOMES = List.of("intencao", "entidades", "evidencias", "resposta");
    private static final Pattern CODIGO = Pattern.compile("(?i)\\b([A-Z]-\\d+)\\b");

    public ResultadoPriorizacao executar(String mensagem, List<EvidenciaPrompt> evidencias) {
        String texto = mensagem == null ? "" : mensagem;
        String intencao = intencao(texto);
        String entidades = entidades(texto);
        List<EvidenciaPrompt> recebidas = evidencias == null ? List.of() : evidencias;
        boolean temBase = texto.toLowerCase(Locale.ROOT).contains("evidencia:")
                || recebidas.stream().anyMatch(FontesAutorizadas::documental);
        String prioridade = temBase ? "alta" : null;
        String resposta = temBase
                ? "prioridade sustentada pela evidencia"
                : "sem base suficiente";
        String detalheEvidencia = temBase ? "base localizada" : "nenhuma";
        List<EtapaPriorizacao> etapas = List.of(
                new EtapaPriorizacao("intencao", intencao),
                new EtapaPriorizacao("entidades", entidades),
                new EtapaPriorizacao("evidencias", detalheEvidencia),
                new EtapaPriorizacao("resposta", resposta));
        return new ResultadoPriorizacao(etapas, temBase, prioridade, !temBase);
    }

    private static String intencao(String texto) {
        String lower = texto.toLowerCase(Locale.ROOT);
        if (lower.contains("atras")) {
            return "atraso";
        }
        if (lower.contains("prioriz")) {
            return "priorizacao";
        }
        return "indefinida";
    }

    private static String entidades(String texto) {
        Matcher matcher = CODIGO.matcher(texto);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "nenhuma";
    }
}
