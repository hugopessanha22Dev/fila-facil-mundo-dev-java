package com.mundodev.filafacil.inference;

import com.mundodev.filafacil.inference.prompt.ConversationOrchestrator;
import com.mundodev.filafacil.inference.tools.MockOperationalTools;
import com.mundodev.filafacil.state.AppState;

public final class InferenceFactory {

    private InferenceFactory() {
    }

    public static OperationalTriageService create(AppState state) {
        return create(state, InferenceConfig.fromEnvironment());
    }

    public static OperationalTriageService create(AppState state, InferenceConfig config) {
        return new OperationalTriageService(
                new ModelRouter(config),
                createGateway(config),
                new TriageSchemaValidator(),
                new MockOperationalTools(state),
                state);
    }

    public static ConversationOrchestrator createConversation(AppState state, InferenceConfig config) {
        return ConversationOrchestrator.createDefault(state, config, createGateway(config));
    }

    public static InferenceGateway createGateway(InferenceConfig config) {
        InferenceGateway inner = config.isMock()
                ? new MockInferenceGateway()
                : new HttpOpenAiCompatibleGateway(config);
        return new FallbackInferenceGateway(inner, config);
    }
}
