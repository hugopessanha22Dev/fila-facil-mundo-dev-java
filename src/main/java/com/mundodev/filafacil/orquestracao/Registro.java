package com.mundodev.filafacil.orquestracao;

import java.util.List;

public record Registro(
        String id,
        String fluxo,
        String destino,
        String entrada,
        String saidaDeclarada,
        String risco,
        String resposta,
        List<String> etapas,
        String ferramenta,
        String intencao,
        String dados,
        String ferramentas,
        String contexto,
        String modelo,
        String versao,
        int tentativas,
        String erros,
        double custo,
        long latencia,
        String validacao,
        boolean fallback,
        String status,
        String feedback,
        List<String> trilha) {

    public Registro comFeedback(String novo) {
        return new Registro(
                id, fluxo, destino, entrada, saidaDeclarada, risco, resposta, etapas, ferramenta,
                intencao, dados, ferramentas, contexto, modelo, versao, tentativas, erros, custo, latencia,
                validacao, fallback, status, novo, trilha);
    }

    public Registro comTrabalho(String novoStatus, int novasTentativas, String novosErros, List<String> novaTrilha) {
        return new Registro(
                id, fluxo, destino, entrada, saidaDeclarada, risco, resposta, etapas, ferramenta,
                intencao, dados, ferramentas, contexto, modelo, versao, novasTentativas, novosErros, custo, latencia,
                validacao, fallback, novoStatus, feedback, novaTrilha);
    }
}
