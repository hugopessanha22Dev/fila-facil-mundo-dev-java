package com.mundodev.filafacil.orquestracao;

public final class RelogioPasso {

    private long agora;

    public long agoraMs() {
        return agora;
    }

    public void avancar(long ms) {
        agora += ms;
    }
}
