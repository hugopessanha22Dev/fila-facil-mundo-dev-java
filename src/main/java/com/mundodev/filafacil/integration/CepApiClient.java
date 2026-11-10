package com.mundodev.filafacil.integration;

import com.mundodev.filafacil.model.EnderecoCep;

import java.util.concurrent.CompletableFuture;

public interface CepApiClient {

    CompletableFuture<EnderecoCep> buscar(String cep);
}
