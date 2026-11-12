package com.mundodev.filafacil.excecoes.adapters;

import com.mundodev.filafacil.excecoes.application.RepositorioExcecoes;
import com.mundodev.filafacil.excecoes.domain.ExcecaoEntrega;
import com.mundodev.filafacil.excecoes.domain.StatusExcecao;
import com.mundodev.filafacil.excecoes.domain.TipoExcecao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RepositorioMemoria implements RepositorioExcecoes {

    private final Map<String, ExcecaoEntrega> itens = new LinkedHashMap<>();

    @Override
    public void salvar(ExcecaoEntrega excecao) {
        itens.put(excecao.id(), excecao);
    }

    @Override
    public Optional<ExcecaoEntrega> porId(String id) {
        return Optional.ofNullable(itens.get(id));
    }

    @Override
    public boolean existeAberta(String codigoEntrega, TipoExcecao tipo) {
        return itens.values().stream().anyMatch(excecao ->
                excecao.status() == StatusExcecao.aberta
                        && excecao.codigoEntrega().equals(codigoEntrega)
                        && excecao.tipo() == tipo);
    }

    public int tamanho() {
        return itens.size();
    }
}
