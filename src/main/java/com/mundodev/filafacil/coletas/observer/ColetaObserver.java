package com.mundodev.filafacil.coletas.observer;

import com.mundodev.filafacil.coletas.Coleta;

public interface ColetaObserver {

    void onColetaCriada(Coleta coleta);

    void onStatusAtualizado(Coleta coleta, String statusAnterior, String statusNovo);
}
