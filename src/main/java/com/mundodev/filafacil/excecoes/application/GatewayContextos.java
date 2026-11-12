package com.mundodev.filafacil.excecoes.application;

import com.mundodev.filafacil.excecoes.domain.OrigemExcecao;
import com.mundodev.filafacil.excecoes.domain.TipoExcecao;

import java.util.Optional;

public interface GatewayContextos {

    Optional<Traducao> traduzir(String termo);

    record Traducao(TipoExcecao tipo, OrigemExcecao origem) {
    }
}
