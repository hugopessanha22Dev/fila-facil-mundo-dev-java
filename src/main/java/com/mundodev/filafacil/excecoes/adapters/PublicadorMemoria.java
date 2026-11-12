package com.mundodev.filafacil.excecoes.adapters;

import com.mundodev.filafacil.excecoes.application.PublicadorEventos;
import com.mundodev.filafacil.excecoes.domain.EventoExcecao;

import java.util.ArrayList;
import java.util.List;

public final class PublicadorMemoria implements PublicadorEventos {

    private final List<EventoExcecao> eventos = new ArrayList<>();

    @Override
    public void publicar(EventoExcecao evento) {
        eventos.add(evento);
    }

    @Override
    public List<EventoExcecao> publicados() {
        return List.copyOf(eventos);
    }
}
