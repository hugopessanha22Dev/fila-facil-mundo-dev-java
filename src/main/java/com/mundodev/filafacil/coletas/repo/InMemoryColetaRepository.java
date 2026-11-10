package com.mundodev.filafacil.coletas.repo;

import com.mundodev.filafacil.coletas.Coleta;

import java.util.ArrayList;
import java.util.List;

public class InMemoryColetaRepository implements ColetaRepository {

    private List<Coleta> dados = new ArrayList<>();

    @Override
    public List<Coleta> carregar() {
        return new ArrayList<>(dados);
    }

    @Override
    public void salvar(List<Coleta> coletas) {
        dados = coletas == null ? new ArrayList<>() : new ArrayList<>(coletas);
    }
}
