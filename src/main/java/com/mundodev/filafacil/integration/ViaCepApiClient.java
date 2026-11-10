package com.mundodev.filafacil.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.ApiException;
import com.mundodev.filafacil.model.EnderecoCep;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ViaCepApiClient implements CepApiClient {

    private static final String BASE_URL = "https://viacep.com.br/ws/";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ViaCepApiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), new ObjectMapper());
    }

    public ViaCepApiClient(HttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public CompletableFuture<EnderecoCep> buscar(String cep) {
        String cepLimpo = cep == null ? "" : cep.replaceAll("\\D", "");
        if (cepLimpo.length() != 8) {
            return CompletableFuture.failedFuture(
                    new ApiException("CEP invalido. Informe 8 digitos (ex.: 01001000)."));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + cepLimpo + "/json/"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new ApiException("Falha na API ViaCEP (HTTP " + response.statusCode() + ").");
                    }
                    try {
                        EnderecoCep endereco = mapper.readValue(response.body(), EnderecoCep.class);
                        if (endereco == null || endereco.isErro()) {
                            throw new ApiException("CEP nao encontrado na ViaCEP: " + cepLimpo);
                        }
                        endereco.setCep(cepLimpo);
                        return endereco;
                    } catch (ApiException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new ApiException("Resposta invalida da ViaCEP.", ex);
                    }
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ApiException apiEx) {
                        throw apiEx;
                    }
                    throw new ApiException("Falha de conexao com a ViaCEP: " + cause.getMessage(), cause);
                });
    }
}
