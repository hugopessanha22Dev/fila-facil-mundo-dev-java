package com.mundodev.filafacil.excecoes.adapters;

import com.mundodev.filafacil.excecoes.application.CicloExcecao;

public final class ExcecoesSessao {

    private final RepositorioMemoria repositorio = new RepositorioMemoria();
    private final PublicadorMemoria publicador = new PublicadorMemoria();
    private final CicloExcecao ciclo;
    private int sequencia;

    public ExcecoesSessao() {
        this.ciclo = new CicloExcecao(repositorio, publicador, new GatewayContextosMemoria());
    }

    public CicloExcecao ciclo() {
        return ciclo;
    }

    public RepositorioMemoria repositorio() {
        return repositorio;
    }

    public PublicadorMemoria publicador() {
        return publicador;
    }

    public String proximoId() {
        sequencia++;
        return "EX-" + sequencia;
    }
}
