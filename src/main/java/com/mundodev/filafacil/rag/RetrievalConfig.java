package com.mundodev.filafacil.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public record RetrievalConfig(
        int chunkSize,
        int overlap,
        int topK,
        double hybridWeight,
        boolean rerank,
        int sessionTtlMinutes,
        double cutoff,
        int maxTopK) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static RetrievalConfig load() {
        try (InputStream in = RetrievalConfig.class.getResourceAsStream("/rag/retrieval.json")) {
            if (in == null) {
                throw new IllegalStateException("retrieval.json ausente");
            }
            JsonNode node = MAPPER.readTree(in);
            return new RetrievalConfig(
                    node.path("chunkSize").asInt(),
                    node.path("overlap").asInt(),
                    node.path("topK").asInt(),
                    node.path("hybridWeight").asDouble(),
                    node.path("rerank").asBoolean(),
                    node.path("sessionTtlMinutes").asInt(),
                    node.path("cutoff").asDouble(),
                    node.path("maxTopK").asInt(6));
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler retrieval.json", ex);
        }
    }

    public RetrievalConfig withChunkSize(int size) {
        return new RetrievalConfig(size, overlap, topK, hybridWeight, rerank, sessionTtlMinutes, cutoff, maxTopK);
    }

    public RetrievalConfig withOverlap(int value) {
        return new RetrievalConfig(chunkSize, value, topK, hybridWeight, rerank, sessionTtlMinutes, cutoff, maxTopK);
    }

    public RetrievalConfig withTopK(int value) {
        return new RetrievalConfig(chunkSize, overlap, value, hybridWeight, rerank, sessionTtlMinutes, cutoff, maxTopK);
    }

    public RetrievalConfig withHybridWeight(double value) {
        return new RetrievalConfig(chunkSize, overlap, topK, value, rerank, sessionTtlMinutes, cutoff, maxTopK);
    }

    public RetrievalConfig withRerank(boolean value) {
        return new RetrievalConfig(chunkSize, overlap, topK, hybridWeight, value, sessionTtlMinutes, cutoff, maxTopK);
    }

    public RetrievalConfig withCutoff(double value) {
        return new RetrievalConfig(chunkSize, overlap, topK, hybridWeight, rerank, sessionTtlMinutes, value, maxTopK);
    }

    public RetrievalConfig withMaxTopK(int value) {
        return new RetrievalConfig(chunkSize, overlap, topK, hybridWeight, rerank, sessionTtlMinutes, cutoff, value);
    }
}
