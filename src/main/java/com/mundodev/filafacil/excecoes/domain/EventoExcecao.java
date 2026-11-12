package com.mundodev.filafacil.excecoes.domain;

public record EventoExcecao(String excecaoId, StatusExcecao anterior, StatusExcecao novo) {
}
