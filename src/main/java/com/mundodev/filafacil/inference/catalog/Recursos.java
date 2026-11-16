package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

final class Recursos {

    static final ObjectMapper JSON = new ObjectMapper();

    private Recursos() {
    }

    static JsonNode ler(String classpath) {
        try (InputStream in = Recursos.class.getResourceAsStream(classpath)) {
            if (in == null) {
                throw new IllegalStateException("Recurso ausente: " + classpath);
            }
            return JSON.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler " + classpath, ex);
        }
    }

    static boolean jsonValido(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        try {
            JsonNode node = JSON.readTree(texto);
            return node.isObject() || node.isArray();
        } catch (IOException ex) {
            return false;
        }
    }
}
