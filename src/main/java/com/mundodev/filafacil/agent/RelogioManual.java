package com.mundodev.filafacil.agent;

public final class RelogioManual implements Relogio {

    private long agora;
    private final long salto;

    public RelogioManual(long inicio, long salto) {
        this.agora = inicio;
        this.salto = salto;
    }

    @Override
    public long agoraMs() {
        long atual = agora;
        agora += salto;
        return atual;
    }
}
