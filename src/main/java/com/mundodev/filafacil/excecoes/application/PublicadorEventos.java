package com.mundodev.filafacil.excecoes.application;

import com.mundodev.filafacil.excecoes.domain.EventoExcecao;

import java.util.List;

public interface PublicadorEventos {

    void publicar(EventoExcecao evento);

    List<EventoExcecao> publicados();
}
