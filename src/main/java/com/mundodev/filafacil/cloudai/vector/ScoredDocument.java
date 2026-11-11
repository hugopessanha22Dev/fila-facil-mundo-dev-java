package com.mundodev.filafacil.cloudai.vector;

public record ScoredDocument(OperationalDocument document, String version, double similarity) {
}
