package com.mundodev.filafacil.coletas.observer;

import com.mundodev.filafacil.coletas.Coleta;

import java.util.ArrayList;
import java.util.List;

public class ColetaEventPublisher {

    private final List<ColetaObserver> observers;

    public ColetaEventPublisher(List<ColetaObserver> observers) {
        this.observers = observers == null ? List.of() : List.copyOf(observers);
    }

    public void notifyCriada(Coleta coleta) {
        for (ColetaObserver observer : observers) {
            observer.onColetaCriada(coleta);
        }
    }

    public void notifyStatusAtualizado(Coleta coleta, String statusAnterior, String statusNovo) {
        for (ColetaObserver observer : observers) {
            observer.onStatusAtualizado(coleta, statusAnterior, statusNovo);
        }
    }

    public List<ColetaObserver> observers() {
        return new ArrayList<>(observers);
    }
}
