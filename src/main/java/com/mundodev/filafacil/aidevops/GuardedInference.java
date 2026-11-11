package com.mundodev.filafacil.aidevops;

import com.mundodev.filafacil.cloudai.Redactor;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceParams;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;
import com.mundodev.filafacil.inference.ModelProfile;

public final class GuardedInference {

    public CaseOutcome run(
            GoldenCase caso,
            ModelSettings settings,
            InferenceGateway gateway,
            String prompt,
            String promptVersion,
            String configVersion,
            int remainingBudget) {
        if (caso.message() == null || caso.message().isBlank()) {
            return empty(caso, promptVersion, configVersion, true, "entrada vazia");
        }
        int budget = remainingBudget;
        int modelCalls = 0;
        int primaryDone = 0;
        long latency = 0;
        double cost = 0;
        int tokens = 0;
        String primaryError = "";
        InferenceResponse last = null;
        OutputSchema.Parsed parsed = OutputSchema.Parsed.invalid();
        int attempts = Math.max(1, settings.maxRetries());

        for (int i = 0; i < attempts; i++) {
            if (modelCalls >= budget) {
                primaryError = "rate_limit";
                break;
            }
            modelCalls++;
            primaryDone++;
            try {
                last = gateway.complete(request(caso, settings.primaryModel(), ModelProfile.LIGHT, prompt));
                latency += last.latencyMs();
                cost += last.estimatedCostUsd();
                tokens += tokensOf(last.rawJson());
                parsed = OutputSchema.parse(last.rawJson());
                if (parsed.schemaOk()) {
                    primaryError = "";
                    break;
                }
                primaryError = "schema invalido";
            } catch (RuntimeException ex) {
                primaryError = ex.getMessage() == null ? "falha primaria" : ex.getMessage();
                parsed = OutputSchema.Parsed.invalid();
            }
        }

        boolean fallback = !parsed.schemaOk();
        boolean human = false;
        String model = last == null ? settings.primaryModel() : last.modelId();
        if (fallback) {
            if (modelCalls >= budget) {
                human = true;
                if (primaryError.isBlank()) {
                    primaryError = "rate_limit";
                }
            } else {
                modelCalls++;
                try {
                    last = gateway.complete(request(
                            caso, settings.fallbackModel(), ModelProfile.DETAILED, prompt));
                    model = last.modelId();
                    latency += last.latencyMs();
                    cost += last.estimatedCostUsd();
                    tokens += tokensOf(last.rawJson());
                    parsed = OutputSchema.parse(last.rawJson());
                    human = !parsed.schemaOk();
                } catch (RuntimeException ex) {
                    human = true;
                    model = settings.fallbackModel();
                    parsed = OutputSchema.Parsed.invalid();
                }
            }
        }

        boolean accepted = parsed.schemaOk() && !human;
        String raw = last == null ? "" : last.rawJson();
        return new CaseOutcome(
                caso.id(),
                caso.category(),
                false,
                parsed.jsonValid(),
                parsed.schemaOk(),
                !parsed.resumo().isBlank(),
                accepted && caso.expectedTipo().equals(parsed.tipo()),
                accepted && caso.expectedPrioridade().equals(parsed.prioridade()),
                fallback,
                human,
                Math.max(0, primaryDone - 1),
                modelCalls,
                latency,
                cost,
                tokens,
                model,
                primaryError,
                Redactor.mask(raw),
                promptVersion,
                configVersion);
    }

    private static CaseOutcome empty(
            GoldenCase caso, String promptVersion, String configVersion, boolean rejected, String error) {
        return new CaseOutcome(
                caso.id(),
                caso.category(),
                rejected,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                "",
                error,
                "",
                promptVersion,
                configVersion);
    }

    private static InferenceRequest request(
            GoldenCase caso, String modelId, ModelProfile profile, String prompt) {
        InferenceParams params = profile == ModelProfile.DETAILED
                ? InferenceParams.forCritical()
                : InferenceParams.forLight();
        return new InferenceRequest(caso.message(), profile, params, modelId, prompt);
    }

    private static int tokensOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        return Math.max(1, raw.length() / 4);
    }
}
