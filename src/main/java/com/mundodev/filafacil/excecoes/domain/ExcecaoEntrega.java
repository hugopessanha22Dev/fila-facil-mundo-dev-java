package com.mundodev.filafacil.excecoes.domain;

import java.time.Instant;

public final class ExcecaoEntrega {

    private final String id;
    private final String codigoEntrega;
    private final TipoExcecao tipo;
    private final OrigemExcecao origem;
    private final Severidade severidade;
    private final String descricao;
    private final Instant abertura;
    private final Instant fimJanela;
    private StatusExcecao status;

    private ExcecaoEntrega(
            String id,
            String codigoEntrega,
            TipoExcecao tipo,
            OrigemExcecao origem,
            Severidade severidade,
            String descricao,
            Instant abertura,
            Instant fimJanela) {
        this.id = id;
        this.codigoEntrega = codigoEntrega;
        this.tipo = tipo;
        this.origem = origem;
        this.severidade = severidade;
        this.descricao = descricao;
        this.abertura = abertura;
        this.fimJanela = fimJanela;
        this.status = StatusExcecao.aberta;
    }

    public static ExcecaoEntrega abrir(
            String id,
            String codigoEntrega,
            TipoExcecao tipo,
            OrigemExcecao origem,
            Severidade severidade,
            String descricao,
            Instant abertura,
            Instant fimJanela) {
        if (id == null || id.isBlank()) {
            throw new TransicaoRecusada("identificador e obrigatorio");
        }
        if (codigoEntrega == null || codigoEntrega.isBlank()) {
            throw new TransicaoRecusada("codigo da entrega e obrigatorio");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new TransicaoRecusada("descricao e obrigatoria");
        }
        if (tipo == null || origem == null || severidade == null || abertura == null || fimJanela == null) {
            throw new TransicaoRecusada("tipo, origem, severidade, abertura e janela sao obrigatorios");
        }
        return new ExcecaoEntrega(id, codigoEntrega.trim(), tipo, origem, severidade, descricao.trim(), abertura, fimJanela);
    }

    public EventoExcecao analisar() {
        if (status != StatusExcecao.aberta) {
            throw new TransicaoRecusada("analise sai de aberta");
        }
        return transitar(StatusExcecao.em_analise);
    }

    public EventoExcecao escalar() {
        if (status != StatusExcecao.em_analise) {
            throw new TransicaoRecusada("escala sai de em analise");
        }
        if (!severidade.exigeEscala()) {
            throw new TransicaoRecusada("escala exige severidade alta ou critica");
        }
        return transitar(StatusExcecao.escalada);
    }

    public EventoExcecao resolver(Instant agora) {
        if (status == StatusExcecao.aberta) {
            throw new TransicaoRecusada("resolucao exige analise");
        }
        if (severidade.exigeEscala() && status != StatusExcecao.escalada) {
            throw new TransicaoRecusada("resolucao exige escala");
        }
        if (status != StatusExcecao.em_analise && status != StatusExcecao.escalada) {
            throw new TransicaoRecusada("resolucao sai de em analise ou escalada");
        }
        if (agora == null || agora.isAfter(fimJanela)) {
            throw new TransicaoRecusada("janela vencida");
        }
        return transitar(StatusExcecao.resolvida);
    }

    public EventoExcecao cancelar() {
        if (status != StatusExcecao.aberta && status != StatusExcecao.em_analise) {
            throw new TransicaoRecusada("cancelamento sai de aberta ou em analise");
        }
        return transitar(StatusExcecao.cancelada);
    }

    private EventoExcecao transitar(StatusExcecao novo) {
        StatusExcecao anterior = status;
        status = novo;
        return new EventoExcecao(id, anterior, novo);
    }

    public String id() {
        return id;
    }

    public String codigoEntrega() {
        return codigoEntrega;
    }

    public TipoExcecao tipo() {
        return tipo;
    }

    public OrigemExcecao origem() {
        return origem;
    }

    public Severidade severidade() {
        return severidade;
    }

    public String descricao() {
        return descricao;
    }

    public Instant abertura() {
        return abertura;
    }

    public Instant fimJanela() {
        return fimJanela;
    }

    public StatusExcecao status() {
        return status;
    }
}
