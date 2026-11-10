package com.mundodev.filafacil.inference.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.error.EntradaException;
import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.InferenceConfig;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;
import com.mundodev.filafacil.inference.ModelRouter;
import com.mundodev.filafacil.state.AppState;

public class ConversationOrchestrator {

    private final ConversationSession session;
    private final ClarificationEngine clarification;
    private final PromptComposer composer;
    private final InferenceGateway gateway;
    private final ModelRouter router;
    private final OrientedRetry orientedRetry;
    private final AppState state;
    private final ObjectMapper mapper;
    private final GroundingValidator groundingValidator;

    public ConversationOrchestrator(
            ConversationSession session,
            ClarificationEngine clarification,
            PromptComposer composer,
            InferenceGateway gateway,
            ModelRouter router,
            OrientedRetry orientedRetry,
            AppState state) {
        this.session = session;
        this.clarification = clarification;
        this.composer = composer;
        this.gateway = gateway;
        this.router = router;
        this.orientedRetry = orientedRetry;
        this.state = state;
        this.mapper = new ObjectMapper();
        this.groundingValidator = new GroundingValidator(mapper);
    }

    public ConversationSession session() {
        return session;
    }

    public TurnResult handleTurn(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            throw new EntradaException("Mensagem do atendimento e obrigatoria.");
        }
        state.setLoading(true);
        state.limparErro();
        try {
            clarification.absorbMessage(session, mensagem);
            if (!session.prontaParaConclusao()) {
                String question = clarification.nextQuestion(session);
                return TurnResult.clarification(question, session);
            }

            String system = composer.composeSystemPrompt("transform", session.summary());
            InferenceRequest base = router.route(buildUserPayload(mensagem));
            InferenceRequest withPrompt = new InferenceRequest(
                    base.mensagem(), base.profile(), base.params(), base.modelId(), system);

            InferenceResponse response = orientedRetry.execute(
                    gateway,
                    withPrompt,
                    session.historicoTexto(),
                    error -> system + "\nCorrija o erro de validacao: " + error
                            + "\nNao invente campos. Marque ausentes corretamente.");

            JsonNode node = mapper.readTree(response.rawJson());
            enrichFromSession(node);
            groundingValidator.validate(node.toString(), session.historicoTexto());
            session.setConcluida(true);
            state.limparErro();
            return TurnResult.conclusion(node.toPrettyString(), response, session);
        } catch (InferenceException | EntradaException ex) {
            String origem = ex instanceof InferenceException ie ? ie.getOrigem() : "entrada";
            state.setUltimoErro(origem, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            InferenceException wrapped = new InferenceException(
                    "Falha no atendimento conversacional: " + ex.getMessage(), ex);
            state.setUltimoErro(wrapped.getOrigem(), wrapped.getMessage());
            throw wrapped;
        } finally {
            state.setLoading(false);
        }
    }

    public void reset() {
        session.clear();
    }

    private String buildUserPayload(String mensagem) {
        return "Mensagem atual:\n" + mensagem + "\n\nSessao:\n" + session.summary();
    }

    private void enrichFromSession(JsonNode node) {
        if (!(node instanceof ObjectNode obj)) {
            return;
        }
        if (session.getIntencao() != null && (!obj.has("tipoSolicitacao") || obj.get("tipoSolicitacao").asText().isBlank())) {
            obj.put("tipoSolicitacao", session.getIntencao());
        }
        putIfMissing(obj, "codigoEntrega", session.getCampo("codigoEntrega").valor());
        putIfMissing(obj, "cliente", session.getCampo("cliente").valor());
        putIfMissing(obj, "status", session.getCampo("status").valor());
        if (!obj.has("intervencencaoHumana")) {
            obj.put("intervencencaoHumana", "reclamacao_cliente".equals(session.getIntencao()));
        }
        if (!obj.has("prioridadeSugerida") || obj.get("prioridadeSugerida").asText("").isBlank()) {
            obj.put("prioridadeSugerida",
                    "reclamacao_cliente".equals(session.getIntencao()) ? "alta" : "media");
        }
        if (!obj.has("proximaAcao") || obj.get("proximaAcao").asText("").isBlank()) {
            obj.put("proximaAcao", "Registrar caso e seguir procedimento operacional");
        }
    }

    private static void putIfMissing(ObjectNode obj, String field, String value) {
        if (value == null) {
            return;
        }
        if (!obj.has(field) || obj.get(field).isNull() || obj.get(field).asText("").isBlank()) {
            obj.put(field, value);
        }
    }

    public static ConversationOrchestrator createDefault(AppState state, InferenceConfig config, InferenceGateway gateway) {
        PromptComposer composer = new PromptComposer(new PromptLibrary(), new FewShotCatalog());
        OrientedRetry retry = new OrientedRetry(2, new GroundingValidator());
        return new ConversationOrchestrator(
                new ConversationSession(),
                new ClarificationEngine(),
                composer,
                gateway,
                new ModelRouter(config),
                retry,
                state);
    }

    public record TurnResult(
            boolean concluded,
            String clarificationQuestion,
            String conclusionJson,
            InferenceResponse response,
            ConversationSession session) {

        public static TurnResult clarification(String question, ConversationSession session) {
            return new TurnResult(false, question, null, null, session);
        }

        public static TurnResult conclusion(String json, InferenceResponse response, ConversationSession session) {
            return new TurnResult(true, null, json, response, session);
        }
    }
}
