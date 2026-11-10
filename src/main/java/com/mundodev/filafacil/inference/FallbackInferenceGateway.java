package com.mundodev.filafacil.inference;

import com.mundodev.filafacil.error.InferenceException;

/**
 * Tenta o request no perfil/modelo primario; em falha recuperavel, reexecuta no perfil detalhado (fallback).
 */
public class FallbackInferenceGateway implements InferenceGateway {

    private final InferenceGateway delegate;
    private final InferenceConfig config;

    public FallbackInferenceGateway(InferenceGateway delegate, InferenceConfig config) {
        this.delegate = delegate;
        this.config = config;
    }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        try {
            return delegate.complete(request);
        } catch (InferenceException primaryEx) {
            if (request.profile() == ModelProfile.DETAILED) {
                throw primaryEx;
            }
            System.out.println("[inference-fallback] primario falhou (" + primaryEx.getMessage()
                    + "); tentando perfil DETAILED");
            InferenceRequest fallbackRequest = new InferenceRequest(
                    request.mensagem(),
                    ModelProfile.DETAILED,
                    InferenceParams.forCritical(),
                    config.modelFor(ModelProfile.DETAILED));
            try {
                InferenceResponse response = delegate.complete(fallbackRequest);
                return new InferenceResponse(
                        response.rawJson(),
                        response.profileUsed(),
                        response.modelId(),
                        response.latencyMs(),
                        response.estimatedCostUsd(),
                        true);
            } catch (InferenceException fallbackEx) {
                throw new InferenceException(
                        "Primario e fallback falharam. Primario: " + primaryEx.getMessage()
                                + " | Fallback: " + fallbackEx.getMessage(),
                        fallbackEx);
            }
        }
    }
}
