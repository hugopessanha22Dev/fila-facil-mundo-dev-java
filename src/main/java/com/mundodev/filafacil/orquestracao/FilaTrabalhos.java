package com.mundodev.filafacil.orquestracao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FilaTrabalhos {

    private final Path raiz;
    private final RelogioPasso relogio;
    private final int timeoutMs;
    private final int tentativasMaximas;
    private int falhasTemporarias;
    private final List<Trabalho> trabalhos = new ArrayList<>();

    public FilaTrabalhos(Path raiz, RelogioPasso relogio, int timeoutMs, int tentativasMaximas, int falhasTemporarias) {
        this.raiz = raiz;
        this.relogio = relogio;
        this.timeoutMs = timeoutMs;
        this.tentativasMaximas = tentativasMaximas;
        this.falhasTemporarias = falhasTemporarias;
    }

    public Trabalho aceitar(String id, String tipo) {
        Trabalho trabalho = new Trabalho(id, tipo, relogio.agoraMs());
        trabalho.trilha.add("aceito");
        trabalho.status = "aceito";
        trabalhos.add(trabalho);
        return trabalho;
    }

    public Trabalho obter(String id) {
        for (Trabalho trabalho : trabalhos) {
            if (trabalho.id.equals(id)) {
                return trabalho;
            }
        }
        return null;
    }

    public void drenar() throws IOException {
        for (Trabalho trabalho : trabalhos) {
            if (!"aceito".equals(trabalho.status)) {
                continue;
            }
            trabalho.trilha.add("processando");
            trabalho.status = "processando";
            if (relogio.agoraMs() - trabalho.inicioMs > timeoutMs) {
                trabalho.status = "falhou";
                trabalho.erros = "timeout";
                trabalho.trilha.add("falhou");
                continue;
            }
            while (trabalho.tentativas < tentativasMaximas) {
                trabalho.tentativas++;
                if (falhasTemporarias > 0) {
                    falhasTemporarias--;
                    trabalho.erros = "temporario";
                    trabalho.eventos.add("retry");
                    continue;
                }
                Files.createDirectories(raiz);
                Files.writeString(raiz.resolve(trabalho.id + ".txt"), trabalho.tipo);
                trabalho.status = "concluido";
                trabalho.erros = trabalho.eventos.isEmpty() ? "nenhum" : "retry";
                trabalho.trilha.add("concluido");
                break;
            }
            if ("processando".equals(trabalho.status)) {
                trabalho.status = "falhou";
                trabalho.trilha.add("falhou");
            }
        }
    }

    public static final class Trabalho {
        private final String id;
        private final String tipo;
        private final long inicioMs;
        private final List<String> trilha = new ArrayList<>();
        private final List<String> eventos = new ArrayList<>();
        private String status;
        private String erros = "nenhum";
        private int tentativas;

        private Trabalho(String id, String tipo, long inicioMs) {
            this.id = id;
            this.tipo = tipo;
            this.inicioMs = inicioMs;
        }

        public String status() {
            return status;
        }

        public int tentativas() {
            return tentativas;
        }

        public String erros() {
            return erros;
        }

        public List<String> trilha() {
            return List.copyOf(trilha);
        }

        public List<String> eventos() {
            return List.copyOf(eventos);
        }
    }
}
