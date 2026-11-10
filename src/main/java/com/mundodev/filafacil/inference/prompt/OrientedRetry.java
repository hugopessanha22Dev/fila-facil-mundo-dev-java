package com.mundodev.filafacil.inference.prompt;

import com.mundodev.filafacil.error.InferenceException;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;

import java.util.function.Function;

public class OrientedRetry {

    private final int maxAttempts;
    private final GroundingValidator groundingValidator;

    public OrientedRetry(int maxAttempts, GroundingValidator groundingValidator) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.groundingValidator = groundingValidator;
    }

    public InferenceResponse execute(
            InferenceGateway gateway,
            InferenceRequest baseRequest,
            String sourceText,
            Function<String, String> correctionAppender) {
        InferenceException last = null;
        InferenceRequest current = baseRequest;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                InferenceResponse response = gateway.complete(current);
                groundingValidator.validate(response.rawJson(), sourceText);
                return response;
            } catch (InferenceException ex) {
                last = ex;
                if (attempt >= maxAttempts) {
                    break;
                }
                String correctedPrompt = correctionAppender.apply(ex.getMessage());
                current = new InferenceRequest(
                        current.mensagem(),
                        current.profile(),
                        current.params(),
                        current.modelId(),
                        correctedPrompt);
            }
        }
        throw new InferenceException(
                "Retry orientado esgotado (" + maxAttempts + "). Revisao humana necessaria. Ultimo erro: "
                        + (last != null ? last.getMessage() : "desconhecido"),
                last);
    }
}
