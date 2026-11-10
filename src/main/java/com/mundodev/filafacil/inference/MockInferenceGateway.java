package com.mundodev.filafacil.inference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.error.InferenceException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gateway sem rede: classifica heuristicamente e devolve JSON de triagem controlado.
 */
public class MockInferenceGateway implements InferenceGateway {

    private static final Pattern CODIGO = Pattern.compile("(?i)(?:coleta|pedido|rastreio)[#:\\s]*([A-Za-z0-9\\-]+)");
    private final ObjectMapper mapper;
    private final boolean forcePrimaryFailure;

    public MockInferenceGateway() {
        this(false);
    }

    public MockInferenceGateway(boolean forcePrimaryFailure) {
        this.mapper = new ObjectMapper();
        this.forcePrimaryFailure = forcePrimaryFailure;
    }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        long start = System.nanoTime();
        if (forcePrimaryFailure && request.profile() == ModelProfile.LIGHT) {
            throw new InferenceException("Falha simulada no modelo primario (leve).");
        }
        try {
            ObjectNode json = isTransformPrompt(request.resolvedSystemPrompt())
                    ? buildConversationConclusion(request.mensagem())
                    : buildTriage(request.mensagem());
            String raw = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            long latencyMs = (System.nanoTime() - start) / 1_000_000L;
            double cost = estimateCost(raw.length(), request.profile());
            System.out.println("[inference-mock] profile=" + request.profile()
                    + " model=" + request.modelId()
                    + " latencyMs=" + latencyMs
                    + " costUsd~" + cost);
            return new InferenceResponse(raw, request.profile(), request.modelId(), latencyMs, cost, false);
        } catch (InferenceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InferenceException("Falha no mock de inferencia: " + ex.getMessage(), ex);
        }
    }

    private static boolean isTransformPrompt(String systemPrompt) {
        if (systemPrompt == null) {
            return false;
        }
        String lower = systemPrompt.toLowerCase(Locale.ROOT);
        return lower.contains("transforme o texto em registro")
                || lower.contains("\"conclusaopronta\"");
    }

    private ObjectNode buildConversationConclusion(String mensagem) {
        ObjectNode triage = buildTriage(mensagem);
        ObjectNode node = mapper.createObjectNode();
        node.put("tipoSolicitacao", triage.path("tipoSolicitacao").asText("consulta_entrega"));
        copyNullable(triage, node, "codigoColeta", "codigoEntrega");
        if (triage.hasNonNull("cliente")) {
            node.put("cliente", triage.get("cliente").asText());
        } else {
            node.putNull("cliente");
        }
        node.putNull("endereco");
        node.putNull("prazo");
        String status = extractStatus(mensagem);
        if (status == null) {
            node.putNull("status");
        } else {
            node.put("status", status);
        }
        node.put("prioridadeSugerida", triage.path("prioridadeSugerida").asText("media"));
        node.put("proximaAcao", triage.path("proximaAcao").asText("Registrar caso"));
        node.put("intervencencaoHumana", triage.path("intervencencaoHumana").asBoolean(false));
        node.put("justificativaIntervencencao", triage.path("justificativaIntervencencao").asText(""));
        node.put("conclusaoPronta", true);

        ObjectNode campos = node.putObject("campos");
        putCampo(campos, "codigoEntrega", node.path("codigoEntrega"));
        putCampo(campos, "cliente", node.path("cliente"));
        putCampo(campos, "status", node.path("status"));
        return node;
    }

    private void putCampo(ObjectNode campos, String name, com.fasterxml.jackson.databind.JsonNode valueNode) {
        ObjectNode field = campos.putObject(name);
        if (valueNode == null || valueNode.isNull() || valueNode.asText("").isBlank()) {
            field.putNull("valor");
            field.put("origem", "ausente");
            field.putNull("evidencia");
        } else {
            field.put("valor", valueNode.asText());
            field.put("origem", "fato");
            field.put("evidencia", valueNode.asText());
        }
    }

    private void copyNullable(ObjectNode from, ObjectNode to, String fromField, String toField) {
        if (from.hasNonNull(fromField)) {
            to.put(toField, from.get(fromField).asText());
        } else {
            to.putNull(toField);
        }
    }

    private static String extractStatus(String mensagem) {
        if (mensagem == null) {
            return null;
        }
        String lower = mensagem.toLowerCase(Locale.ROOT);
        if (lower.contains("pendente")) {
            return "pendente";
        }
        if (lower.contains("em_rota") || lower.contains("em rota")) {
            return "em_rota";
        }
        if (lower.contains("conclu")) {
            return "concluida";
        }
        if (lower.contains("atras")) {
            return "atrasado";
        }
        return null;
    }

    private ObjectNode buildTriage(String mensagem) {
        String text = mensagem == null ? "" : mensagem;
        String lower = text.toLowerCase(Locale.ROOT);

        String tipo;
        String sentimento;
        String prioridade;
        boolean humano;
        String justificativa;
        double confianca;

        if (containsAny(lower, "reclam", "nao chegou", "não chegou", "nao conclu", "não conclu", "perda", "danificado")) {
            tipo = "reclamacao_cliente";
            sentimento = "insatisfeito";
            prioridade = "alta";
            humano = true;
            justificativa = "Reclamacao com pedido de resolucao imediata.";
            confianca = 0.82;
        } else if (containsAny(lower, "ocorrencia", "atraso", "endereco incorreto", "endereço incorreto", "motorista", "parceiro")) {
            tipo = "ocorrencia_operacional";
            sentimento = containsAny(lower, "urgente", "critico", "crítico") ? "urgente" : "ansioso";
            prioridade = "media";
            humano = containsAny(lower, "incompleto", "faltando", "urgente");
            justificativa = humano ? "Ocorrencia ambigua ou incompleta exige revisao." : "Ocorrencia operacional tipica.";
            confianca = 0.78;
        } else {
            tipo = "consulta_entrega";
            sentimento = containsAny(lower, "onde esta", "onde está", "ansioso", "urgente") ? "ansioso" : "neutro";
            prioridade = "baixa";
            humano = false;
            justificativa = "Consulta simples de status.";
            confianca = 0.88;
        }

        String codigo = extractCodigo(text);
        String cliente = extractCliente(text);

        ObjectNode node = mapper.createObjectNode();
        node.put("tipoSolicitacao", tipo);
        if (codigo == null) {
            node.putNull("codigoColeta");
        } else {
            node.put("codigoColeta", codigo);
        }
        if (cliente == null) {
            node.putNull("cliente");
        } else {
            node.put("cliente", cliente);
        }
        node.put("sentimento", sentimento);
        node.put("prioridadeSugerida", prioridade);
        node.put("resumoOperacional", "Triagem mock: " + tipo + " a partir da mensagem do operador.");
        node.put("proximaAcao", humano
                ? "Encaminhar para atendimento humano e validar dados da coleta."
                : "Consultar status da coleta e responder ao solicitante.");
        node.put("intervencaoHumana", humano);
        node.put("justificativaIntervencencao", justificativa);
        node.put("nivelConfianca", confianca);

        ArrayNode ausentes = node.putArray("camposAusentesOuErro");
        if (codigo == null) {
            ausentes.add("codigoColeta");
        }
        if (cliente == null) {
            ausentes.add("cliente");
        }

        ArrayNode falhas = node.putArray("indicadoresFalha");
        if (humano && confianca < 0.85) {
            falhas.add("baixa_confianca_com_intervencao");
        }
        return node;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String extractCodigo(String text) {
        Matcher matcher = CODIGO.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractCliente(String text) {
        Matcher matcher = Pattern.compile("(?i)cliente[:\\s]+([A-Za-zÀ-ú ]{2,40})").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static double estimateCost(int chars, ModelProfile profile) {
        double tokens = Math.max(1, chars / 4.0);
        double per1k = profile == ModelProfile.DETAILED ? 0.005 : 0.00015;
        return Math.round((tokens / 1000.0) * per1k * 1_000_000.0) / 1_000_000.0;
    }
}
