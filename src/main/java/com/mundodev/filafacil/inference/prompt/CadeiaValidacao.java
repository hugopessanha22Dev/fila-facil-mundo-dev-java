package com.mundodev.filafacil.inference.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;

public final class CadeiaValidacao {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GroundingValidator grounding = new GroundingValidator();

    public ResultadoCadeia avaliar(String bruto) {
        String corrigido = autocorrecao(bruto);
        try {
            JsonNode node = mapper.readTree(corrigido);
            if (!node.isObject()) {
                return ResultadoCadeia.recusada("parser: JSON deve ser objeto");
            }
            if (!texto(node, "tipoSolicitacao") || !texto(node, "proximaAcao")) {
                return ResultadoCadeia.recusada("campo obrigatorio ausente");
            }
            if (node.has("nivelConfianca") && !node.get("nivelConfianca").isNumber()) {
                return ResultadoCadeia.recusada("tipo invalido");
            }
            JsonNode status = node.path("campos").path("status");
            if ("ausente".equals(status.path("origem").asText()) && status.hasNonNull("valor")
                    && !status.get("valor").asText("").isBlank()) {
                return ResultadoCadeia.recusada("consistencia: ausente com valor");
            }
            grounding.validate(corrigido, corrigido);
            return ResultadoCadeia.aceita(corrigido);
        } catch (InferenceException ex) {
            return ResultadoCadeia.recusada(ex.getMessage());
        } catch (Exception ex) {
            return ResultadoCadeia.recusada("parser: " + ex.getMessage());
        }
    }

    public ResultadoCadeia executar(
            InferenceGateway gateway,
            InferenceRequest pedido,
            String fonte,
            int maxTentativas) {
        OrientedRetry retry = new OrientedRetry(maxTentativas, grounding);
        try {
            InferenceResponse resposta = retry.execute(
                    gateway,
                    pedido,
                    fonte,
                    erro -> pedido.resolvedSystemPrompt() + "\nCorrecao: " + erro);
            return avaliar(resposta.rawJson());
        } catch (InferenceException ex) {
            return ResultadoCadeia.bloqueada("revisao humana");
        }
    }

    static String autocorrecao(String bruto) {
        if (bruto == null) {
            return "";
        }
        String texto = bruto.trim();
        if (!texto.startsWith("```")) {
            return texto;
        }
        int quebra = texto.indexOf('\n');
        int fim = texto.lastIndexOf("```");
        if (quebra < 0 || fim <= quebra) {
            return texto;
        }
        return texto.substring(quebra + 1, fim).trim();
    }

    private static boolean texto(JsonNode node, String campo) {
        return node.has(campo) && node.get(campo).isTextual() && !node.get(campo).asText().isBlank();
    }
}
