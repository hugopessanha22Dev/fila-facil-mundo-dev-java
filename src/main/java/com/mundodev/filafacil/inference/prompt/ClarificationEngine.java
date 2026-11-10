package com.mundodev.filafacil.inference.prompt;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClarificationEngine {

    private static final Map<String, String> QUESTIONS = Map.of(
            "codigoEntrega", "Qual o codigo da entrega ou coleta?",
            "cliente", "Qual o nome do cliente?",
            "status", "Qual o status atual informado (se conhecido)? Se nao souber, diga desconhecido.");

    private static final Pattern CODIGO = Pattern.compile(
            "(?i)(?:coleta|pedido|rastreio|codigo|código)[#:\\s]*([A-Za-z0-9\\-]+)");
    private static final Pattern CLIENTE = Pattern.compile(
            "(?i)cliente[:\\s]+([A-Za-zÀ-ú]+(?:\\s+[A-Za-zÀ-ú]+)?)(?=\\s*[,.;]|\\s+pergunt|\\s+sobre|\\s+pede|\\s+informa|$)");
    private static final Pattern STATUS = Pattern.compile(
            "(?i)\\b(pendente|em_rota|em rota|concluida|concluída|atrasado|extraviado)\\b");

    public void absorbMessage(ConversationSession session, String mensagem) {
        session.addMensagem(mensagem);
        String text = mensagem == null ? "" : mensagem;
        String lower = text.toLowerCase(Locale.ROOT);

        if (session.getIntencao() == null) {
            session.setIntencao(detectIntent(lower));
        }

        Matcher codigo = CODIGO.matcher(text);
        if (codigo.find()) {
            session.putCampo("codigoEntrega", FieldValue.fato(codigo.group(1), codigo.group(1)));
        }

        Matcher cliente = CLIENTE.matcher(text);
        if (cliente.find()) {
            session.putCampo("cliente", FieldValue.fato(cliente.group(1).trim(), cliente.group(1).trim()));
        }

        Matcher status = STATUS.matcher(text);
        if (status.find()) {
            String st = status.group(1).toLowerCase(Locale.ROOT).replace(' ', '_')
                    .replace("concluída", "concluida");
            session.putCampo("status", FieldValue.fato(st, status.group(1)));
        }

        if (lower.contains("desconhecido") || lower.contains("nao sei") || lower.contains("não sei")) {
            for (String missing : session.camposAusentesObrigatorios()) {
                if (session.perguntaJaFeita(missing)) {
                    session.marcarDesconhecido(missing, "informado pelo usuario");
                }
            }
        }
    }

    public String nextQuestion(ConversationSession session) {
        List<String> missing = session.camposAusentesObrigatorios();
        for (String field : missing) {
            if (!session.perguntaJaFeita(field)) {
                session.marcarPerguntaFeita(field);
                return QUESTIONS.getOrDefault(field, "Informe o campo: " + field);
            }
        }
        // todos ja perguntados mas ainda faltando: pedir desconhecido
        if (!missing.isEmpty()) {
            String field = missing.get(0);
            return "Ainda falta '" + field + "'. Informe o valor ou diga desconhecido.";
        }
        return null;
    }

    private static String detectIntent(String lower) {
        if (lower.contains("reclam") || lower.contains("procon") || lower.contains("nao chegou")
                || lower.contains("não chegou")) {
            return "reclamacao_cliente";
        }
        if (lower.contains("ocorrencia") || lower.contains("atraso") || lower.contains("motorista")
                || lower.contains("parceiro") || lower.contains("endereco") || lower.contains("endereço")) {
            return "ocorrencia_operacional";
        }
        return "consulta_entrega";
    }
}
