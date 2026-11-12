package com.mundodev.filafacil.excecoes.application;

import com.mundodev.filafacil.excecoes.domain.ExcecaoEntrega;
import com.mundodev.filafacil.excecoes.domain.TipoExcecao;

import java.util.Optional;

public interface RepositorioExcecoes {

    void salvar(ExcecaoEntrega excecao);

    Optional<ExcecaoEntrega> porId(String id);

    boolean existeAberta(String codigoEntrega, TipoExcecao tipo);
}
