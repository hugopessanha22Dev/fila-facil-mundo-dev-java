package com.mundodev.filafacil.excecoes.application;

import com.mundodev.filafacil.excecoes.domain.EventoExcecao;
import com.mundodev.filafacil.excecoes.domain.ExcecaoEntrega;
import com.mundodev.filafacil.excecoes.domain.OrigemExcecao;
import com.mundodev.filafacil.excecoes.domain.Severidade;
import com.mundodev.filafacil.excecoes.domain.StatusExcecao;
import com.mundodev.filafacil.excecoes.domain.TipoExcecao;
import com.mundodev.filafacil.excecoes.domain.TransicaoRecusada;

import java.time.Instant;

public final class CicloExcecao {

    private final RepositorioExcecoes repositorio;
    private final PublicadorEventos publicador;
    private final GatewayContextos gateway;

    public CicloExcecao(RepositorioExcecoes repositorio, PublicadorEventos publicador, GatewayContextos gateway) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.gateway = gateway;
    }

    public ExcecaoEntrega abrir(
            String id,
            String codigoEntrega,
            TipoExcecao tipo,
            OrigemExcecao origem,
            Severidade severidade,
            String descricao,
            Instant abertura,
            Instant fimJanela) {
        if (repositorio.existeAberta(codigoEntrega, tipo)) {
            throw new TransicaoRecusada("ja existe excecao aberta para a entrega e o tipo");
        }
        ExcecaoEntrega excecao = ExcecaoEntrega.abrir(
                id, codigoEntrega, tipo, origem, severidade, descricao, abertura, fimJanela);
        repositorio.salvar(excecao);
        publicador.publicar(new EventoExcecao(excecao.id(), null, StatusExcecao.aberta));
        return excecao;
    }

    public ExcecaoEntrega abrirPorTermo(
            String id,
            String termo,
            Severidade severidade,
            String descricao,
            String codigoEntrega,
            Instant abertura,
            Instant fimJanela) {
        GatewayContextos.Traducao traducao = gateway.traduzir(termo)
                .orElseThrow(() -> new TransicaoRecusada("termo sem traducao"));
        return abrir(id, codigoEntrega, traducao.tipo(), traducao.origem(), severidade, descricao, abertura, fimJanela);
    }

    public ExcecaoEntrega analisar(String id) {
        ExcecaoEntrega excecao = carregar(id);
        EventoExcecao evento = excecao.analisar();
        repositorio.salvar(excecao);
        publicador.publicar(evento);
        return excecao;
    }

    public ExcecaoEntrega escalar(String id) {
        ExcecaoEntrega excecao = carregar(id);
        EventoExcecao evento = excecao.escalar();
        repositorio.salvar(excecao);
        publicador.publicar(evento);
        return excecao;
    }

    public ExcecaoEntrega resolver(String id, Instant agora) {
        ExcecaoEntrega excecao = carregar(id);
        EventoExcecao evento = excecao.resolver(agora);
        repositorio.salvar(excecao);
        publicador.publicar(evento);
        return excecao;
    }

    public ExcecaoEntrega cancelar(String id) {
        ExcecaoEntrega excecao = carregar(id);
        EventoExcecao evento = excecao.cancelar();
        repositorio.salvar(excecao);
        publicador.publicar(evento);
        return excecao;
    }

    public ExcecaoEntrega consultar(String id) {
        return carregar(id);
    }

    private ExcecaoEntrega carregar(String id) {
        return repositorio.porId(id).orElseThrow(() -> new TransicaoRecusada("excecao nao encontrada"));
    }
}
