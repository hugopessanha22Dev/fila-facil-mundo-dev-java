package com.mundodev.filafacil.coletas.observer;

import com.mundodev.filafacil.coletas.Coleta;

import java.util.concurrent.atomic.AtomicInteger;

public class ContadorMemoriaObserver implements ColetaObserver {

    private final AtomicInteger criadas = new AtomicInteger();
    private final AtomicInteger statusAtualizados = new AtomicInteger();

    @Override
    public void onColetaCriada(Coleta coleta) {
        criadas.incrementAndGet();
    }

    @Override
    public void onStatusAtualizado(Coleta coleta, String statusAnterior, String statusNovo) {
        statusAtualizados.incrementAndGet();
    }

    public int getCriadas() {
        return criadas.get();
    }

    public int getStatusAtualizados() {
        return statusAtualizados.get();
    }

    public void reset() {
        criadas.set(0);
        statusAtualizados.set(0);
    }
}
