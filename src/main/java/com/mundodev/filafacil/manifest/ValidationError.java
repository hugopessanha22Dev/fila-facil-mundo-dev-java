package com.mundodev.filafacil.manifest;

public record ValidationError(int linha, String pacote, String codigo, String mensagem) {
}
