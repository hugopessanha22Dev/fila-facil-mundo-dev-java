package com.mundodev.filafacil.agent;

public interface Relogio {

    long agoraMs();

    static Relogio sistema() {
        return System::currentTimeMillis;
    }
}
