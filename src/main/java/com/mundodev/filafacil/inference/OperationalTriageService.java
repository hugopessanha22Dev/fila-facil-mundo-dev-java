package com.mundodev.filafacil.inference;

import com.mundodev.filafacil.error.EntradaException;
import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.tools.OperationalTools;
import com.mundodev.filafacil.inference.tools.ToolError;
import com.mundodev.filafacil.state.AppState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Orquestra roteamento -> gateway (com fallback) -> validacao de schema -> enrich via tools mock.
 */
public class OperationalTriageService {

    private final ModelRouter router;
    private final InferenceGateway gateway;
    private final TriageSchemaValidator validator;
    private final OperationalTools tools;
    private final AppState state;

    public OperationalTriageService(
            ModelRouter router,
            InferenceGateway gateway,
            TriageSchemaValidator validator,
            OperationalTools tools,
            AppState state) {
        this.router = router;
        this.gateway = gateway;
        this.validator = validator;
        this.tools = tools;
        this.state = state;
    }

    public TriageOutcome triar(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            throw new EntradaException("Mensagem de triagem e obrigatoria.");
        }
        state.setLoading(true);
        state.limparErro();
        try {
            InferenceRequest request = router.route(mensagem);
            InferenceResponse response = gateway.complete(request);
            TriageResult result = validator.validateAndParse(response.rawJson());
            Map<String, Object> toolContext = enrichWithTools(result);
            state.limparErro();
            return new TriageOutcome(result, response, toolContext);
        } catch (InferenceException | EntradaException ex) {
            String origem = ex instanceof InferenceException ie ? ie.getOrigem() : "entrada";
            String msg = ex.getMessage();
            state.setUltimoErro(origem, msg);
            throw ex;
        } finally {
            state.setLoading(false);
        }
    }

    private Map<String, Object> enrichWithTools(TriageResult result) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        try {
            if (result.codigoColeta() != null && !result.codigoColeta().isBlank()) {
                ctx.put("statusEntrega", tools.consultarStatusEntrega(result.codigoColeta()));
            }
            String prioridadeCalc = tools.calcularPrioridadeOperacional(
                    result.tipoSolicitacao(), result.sentimento());
            ctx.put("prioridadeCalculada", prioridadeCalc);
            if (result.intervencaoHumana()) {
                ctx.put("encaminhamento", tools.encaminharAtendimentoHumano(
                        result.justificativaIntervencencao(),
                        result.codigoColeta()));
            }
        } catch (ToolError toolError) {
            ctx.put("toolError", Map.of("code", toolError.getCode(), "message", toolError.getMessage()));
        }
        return ctx;
    }

    public record TriageOutcome(
            TriageResult result,
            InferenceResponse response,
            Map<String, Object> toolContext) {
    }
}
