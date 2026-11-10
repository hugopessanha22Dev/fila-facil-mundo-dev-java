package com.mundodev.filafacil.coletas.repo;

import com.mundodev.filafacil.coletas.Coleta;

import java.util.List;

public interface ColetaRepository {

    List<Coleta> carregar();

    void salvar(List<Coleta> coletas);
}
