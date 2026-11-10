package com.mundodev.filafacil.coletas.observer;

import com.mundodev.filafacil.coletas.Coleta;

public class LogOperacionalObserver implements ColetaObserver {

    @Override
    public void onColetaCriada(Coleta coleta) {
        System.out.println("[coleta-evento] criada id=" + coleta.getId()
                + " prioridade=" + coleta.getPrioridade()
                + " status=" + coleta.getStatus());
    }

    @Override
    public void onStatusAtualizado(Coleta coleta, String statusAnterior, String statusNovo) {
        System.out.println("[coleta-evento] status id=" + coleta.getId()
                + " " + statusAnterior + " -> " + statusNovo);
    }
}
