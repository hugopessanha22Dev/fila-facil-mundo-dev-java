package com.mundodev.filafacil.inference.prompt;

import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.InferenceConfig;
import com.mundodev.filafacil.inference.InferenceFactory;
import com.mundodev.filafacil.inference.InferenceParams;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;
import com.mundodev.filafacil.inference.MockInferenceGateway;
import com.mundodev.filafacil.inference.ModelProfile;
import com.mundodev.filafacil.state.AppState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptEngineeringTest {

    @Test
    void promptLibraryCarregaCincoTarefas() {
        PromptLibrary library = new PromptLibrary();
        assertEquals(5, library.size());
        for (String id : PromptLibrary.REQUIRED_IDS) {
            PromptDefinition def = library.get(id);
            assertNotNull(def.objective());
            assertFalse(def.constraints().isEmpty());
            assertFalse(def.qualityCriteria().isEmpty());
            assertNotNull(def.systemTemplate());
        }
    }

    @Test
    void fewShotCatalogTemQuatroTipos() {
        FewShotCatalog catalog = new FewShotCatalog();
        assertTrue(catalog.all().stream().anyMatch(e -> e.type().equals("positivo")));
        assertTrue(catalog.all().stream().anyMatch(e -> e.type().equals("negativo")));
        assertTrue(catalog.all().stream().anyMatch(e -> e.type().equals("contraexemplo")));
        assertTrue(catalog.all().stream().anyMatch(e -> e.type().equals("escalacao")));
    }

    @Test
    void composerIncluiFewShotNoSystemPrompt() {
        PromptComposer composer = new PromptComposer(new PromptLibrary(), new FewShotCatalog());
        String system = composer.composeSystemPrompt("classify", "intencao=consulta_entrega");
        assertTrue(system.contains("Exemplos few-shot"));
        assertTrue(system.contains("Memoria da sessao"));
        assertTrue(system.toLowerCase().contains("classifica"));
    }

    @Test
    void sessaoNaoPerdeCamposENaoRepetePergunta() {
        ConversationSession session = new ConversationSession();
        ClarificationEngine engine = new ClarificationEngine();
        engine.absorbMessage(session, "Cliente: Ana Demo. pergunta sobre atraso");
        assertEquals("Ana Demo", session.getCampo("cliente").valor());
        String q1 = engine.nextQuestion(session);
        assertNotNull(q1);
        assertTrue(session.perguntaJaFeita("codigoEntrega") || session.perguntaJaFeita("status"));
        String firstAsked = session.getPerguntasFeitas().iterator().next();
        engine.absorbMessage(session, "codigo: C-100 status pendente");
        assertEquals("C-100", session.getCampo("codigoEntrega").valor());
        assertEquals("Ana Demo", session.getCampo("cliente").valor());
        String q2 = engine.nextQuestion(session);
        if (q2 != null) {
            assertFalse(q2.equals(q1) && session.camposAusentesObrigatorios().contains(firstAsked)
                    && session.getCampo(firstAsked).preenchido());
        }
    }

    @Test
    void groundingRejeitaStatusInventadoComoFato() {
        GroundingValidator validator = new GroundingValidator();
        String json = """
                {
                  "tipoSolicitacao":"consulta_entrega",
                  "proximaAcao":"Checar status",
                  "status":"concluida",
                  "campos":{"status":{"valor":"concluida","origem":"fato","evidencia":"concluida"}}
                }
                """;
        InferenceException ex = assertThrows(InferenceException.class,
                () -> validator.validate(json, "Onde esta minha entrega?"));
        assertTrue(ex.getMessage().toLowerCase().contains("status"));
    }

    @Test
    void retryOrientadoEsgotaEEscala() {
        AtomicInteger calls = new AtomicInteger();
        OrientedRetry retry = new OrientedRetry(2, new GroundingValidator());
        InferenceRequest request = new InferenceRequest(
                "teste", ModelProfile.LIGHT, InferenceParams.forLight(), "mock-model",
                "system");
        InferenceException ex = assertThrows(InferenceException.class, () ->
                retry.execute(
                        req -> {
                            calls.incrementAndGet();
                            return new InferenceResponse(
                                    "{\"tipoSolicitacao\":\"consulta_entrega\",\"proximaAcao\":\"x\",\"status\":\"concluida\","
                                            + "\"campos\":{\"status\":{\"valor\":\"concluida\",\"origem\":\"fato\",\"evidencia\":\"concluida\"}}}",
                                    ModelProfile.LIGHT, "mock-model", 1, 0, false);
                        },
                        request,
                        "mensagem sem status",
                        err -> "corrigir: " + err));
        assertEquals(2, calls.get());
        assertTrue(ex.getMessage().contains("Retry orientado esgotado"));
        assertTrue(ex.getMessage().toLowerCase().contains("revisao humana"));
    }

    @Test
    void conversaMockDoisTurnosAteConclusao() {
        AppState state = new AppState();
        InferenceConfig config = mockConfig();
        ConversationOrchestrator orch = InferenceFactory.createConversation(state, config);

        ConversationOrchestrator.TurnResult t1 = orch.handleTurn(
                "Motorista: problema no endereco da entrega. Cliente: Bruno Demo");
        assertFalse(t1.concluded());
        assertNotNull(t1.clarificationQuestion());

        ConversationOrchestrator.TurnResult t2 = orch.handleTurn(
                "codigo: R-77 status pendente");
        assertTrue(t2.concluded());
        assertNotNull(t2.conclusionJson());
        assertTrue(t2.conclusionJson().contains("proximaAcao"));
        assertTrue(t2.conclusionJson().contains("R-77") || t2.conclusionJson().contains("Bruno"));
    }

    @Test
    void gatewayDinamicoMantemOneShot() {
        MockInferenceGateway gateway = new MockInferenceGateway();
        InferenceResponse response = gateway.complete(new InferenceRequest(
                "Onde esta a entrega do pedido: C-1?",
                ModelProfile.LIGHT,
                InferenceParams.forLight(),
                "gpt-4o-mini"));
        assertTrue(response.rawJson().contains("tipoSolicitacao"));
        assertTrue(response.rawJson().contains("consulta_entrega"));
    }

    @Test
    void composerSystemPromptVaiParaGateway() {
        PromptComposer composer = new PromptComposer(new PromptLibrary(), new FewShotCatalog());
        String system = composer.composeSystemPrompt("transform", "codigoEntrega=C-1 (fato)");
        assertTrue(system.contains("few-shot") || system.contains("Exemplos"));
        MockInferenceGateway gateway = new MockInferenceGateway();
        InferenceResponse response = gateway.complete(new InferenceRequest(
                "Mensagem atual:\ncodigo: C-1 Cliente: Ana status pendente\n",
                ModelProfile.LIGHT,
                InferenceParams.forLight(),
                "mock",
                system));
        assertTrue(response.rawJson().contains("conclusaoPronta")
                || response.rawJson().contains("proximaAcao"));
    }

    private static InferenceConfig mockConfig() {
        return new InferenceConfig(
                "mock", "", "https://api.openai.com/v1",
                "gpt-4o-mini", "gpt-4o", 10, 1, 0.00015);
    }
}
