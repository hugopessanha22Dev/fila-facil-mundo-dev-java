package com.mundodev.filafacil.inference.catalog;

public interface Relogio {

    long agoraMs();

    void esperar(long ms);

    final class Manual implements Relogio {

        private long agora;

        public void avancar(long ms) {
            agora += ms;
        }

        @Override
        public long agoraMs() {
            return agora;
        }

        @Override
        public void esperar(long ms) {
            // O teste usa espera zero. A producao dobra o intervalo no Relogio.Sistema.
        }
    }

    final class Sistema implements Relogio {

        @Override
        public long agoraMs() {
            return System.currentTimeMillis();
        }

        @Override
        public void esperar(long ms) {
            if (ms <= 0) {
                return;
            }
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
