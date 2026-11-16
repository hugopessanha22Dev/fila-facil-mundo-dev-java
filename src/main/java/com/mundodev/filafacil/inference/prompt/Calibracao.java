package com.mundodev.filafacil.inference.prompt;

import java.util.Map;

public record Calibracao(String texto, boolean incerteza, Map<String, String> fatos) {
}
