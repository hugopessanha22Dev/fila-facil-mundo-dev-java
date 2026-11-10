package com.mundodev.filafacil.inference;

import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.tools.MockOperationalTools;
import com.mundodev.filafacil.inference.tools.ToolError;
import com.mundodev.filafacil.coletas.Coleta;
import com.mundodev.filafacil.state.AppState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InferenceLayerTest {

    @Test
    void mockGatewayRetornaJsonValidavel() {
        InferenceConfig config = mockConfig();
        MockInferenceGateway gateway = new MockInferenceGateway();
        TriageSchemaValidator validator = new TriageSchemaValidator();
        InferenceRequest request = new ModelRouter(config).route("Onde esta a entrega do pedido: C-1?");
        InferenceResponse response = gateway.complete(request);
        TriageResult result = validator.validateAndParse(response.rawJson());
        assertEquals("consulta_entrega", result.tipoSolicitacao());
        assertFalse(response.usedFallback());
    }

    @Test
    void httpSemChaveLancaErroTipado() {
        InferenceConfig config = new InferenceConfig(
                "http", "", "https://api.openai.com/v1",
                "gpt-4o-mini", "gpt-4o", 5, 1, 0.00015);
        HttpOpenAiCompatibleGateway gateway = new HttpOpenAiCompatibleGateway(config);
        InferenceRequest request = new InferenceRequest(
                "teste", ModelProfile.LIGHT, InferenceParams.forLight(), config.modelLight());
        InferenceException ex = assertThrows(InferenceException.class, () -> gateway.complete(request));
        assertEquals("inference", ex.getOrigem());
        assertTrue(ex.getMessage().contains("FILAFACIL_LLM_API_KEY"));
    }

    @Test
    void fallbackQuandoPrimarioFalha() {
        InferenceConfig config = mockConfig();
        InferenceGateway gateway = new FallbackInferenceGateway(new MockInferenceGateway(true), config);
        InferenceRequest light = new InferenceRequest(
                "consulta simples", ModelProfile.LIGHT, InferenceParams.forLight(), config.modelLight());
        InferenceResponse response = gateway.complete(light);
        assertTrue(response.usedFallback());
        assertEquals(ModelProfile.DETAILED, response.profileUsed());
        new TriageSchemaValidator().validateAndParse(response.rawJson());
    }

    @Test
    void validatorRejeitaJsonInvalido() {
        TriageSchemaValidator validator = new TriageSchemaValidator();
        assertThrows(InferenceException.class, () -> validator.validateAndParse("{"));
        assertThrows(InferenceException.class, () -> validator.validateAndParse("{\"tipoSolicitacao\":\"x\"}"));
    }

    @Test
    void routerEscolheLeveEDetalhado() {
        ModelRouter router = new ModelRouter(mockConfig());
        assertEquals(ModelProfile.LIGHT, router.route("Onde esta minha entrega?").profile());
        assertEquals(ModelProfile.DETAILED,
                router.route("Reclamacao urgente: a entrega nao chegou, quero resolucao imediata").profile());
    }

    @Test
    void toolsBuscarColetaSucessoENaoEncontrada() {
        AppState state = new AppState();
        state.setColetas(java.util.List.of(
                new Coleta(10L, "Ana", "Rua A", 1, "alta", "pendente")));
        MockOperationalTools tools = new MockOperationalTools(state);
        assertEquals("Ana", tools.buscarColeta(10L).getNome());
        ToolError err = assertThrows(ToolError.class, () -> tools.buscarColeta(999L));
        assertEquals("NOT_FOUND", err.getCode());
    }

    @Test
    void triageServiceExercitaToolDePrioridade() {
        AppState state = new AppState();
        OperationalTriageService service = InferenceFactory.create(state, mockConfig());
        OperationalTriageService.TriageOutcome outcome = service.triar(
                "Consulta de status da entrega do pedido: 10");
        assertTrue(outcome.toolContext().containsKey("prioridadeCalculada"));
        assertEquals("baixa", outcome.toolContext().get("prioridadeCalculada"));
    }

    private static InferenceConfig mockConfig() {
        return new InferenceConfig(
                "mock", "", "https://api.openai.com/v1",
                "gpt-4o-mini", "gpt-4o", 10, 1, 0.00015);
    }
}
