package com.mundodev.filafacil.orquestracao;

public enum Fluxo {
    EMBEDDING("embedding", "documento", "vetor", "lote no caminho sincrono", "fila", true, "embedding"),
    REPROCESSAMENTO("reprocessamento", "documento", "indice", "lote no caminho sincrono", "fila", true, "reprocessamento"),
    LOTE("lote", "conjunto", "analise", "lote no caminho sincrono", "fila", true, "lote"),
    RESUMO("resumo", "historico", "resumo", "lote no caminho sincrono", "fila", true, "resumo"),
    FORA("fora_escopo", "mensagem", "recusa", "pedido fora da plataforma", "escopo", false, "fora_escopo"),
    AGENTE("agente", "mensagem operacional", "apoio com ferramentas", "acao sem controle", "permissao", false, "agente"),
    TRIAGEM("triagem", "descricao", "categoria e prioridade", "classificacao incorreta", "precisao", false, "classificador de ocorrencia"),
    CLASSIFICADOR("classificador", "entrega", "prioridade", "modelo indisponivel", "fallback", false, "classificador"),
    BUSCA("busca", "pergunta", "trecho e fonte", "resposta inventada", "citacao", false, "busca"),
    ATENDIMENTO("atendimento", "mensagem", "resposta com evidencia", "resposta sem evidencia", "evidencia", false, "atendimento");

    private final String id;
    private final String entrada;
    private final String saida;
    private final String risco;
    private final String validacao;
    private final boolean assincrono;
    private final String destino;

    Fluxo(String id, String entrada, String saida, String risco, String validacao, boolean assincrono, String destino) {
        this.id = id;
        this.entrada = entrada;
        this.saida = saida;
        this.risco = risco;
        this.validacao = validacao;
        this.assincrono = assincrono;
        this.destino = destino;
    }

    public String id() {
        return id;
    }

    public String entrada() {
        return entrada;
    }

    public String saida() {
        return saida;
    }

    public String risco() {
        return risco;
    }

    public String validacaoExigida() {
        return validacao;
    }

    public boolean assincrono() {
        return assincrono;
    }

    public String destino() {
        return destino;
    }
}
