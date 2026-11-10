package com.mundodev.filafacil.coletas;

public class Coleta {

    private Long id;
    private String nome;
    private String endereco;
    private Integer pacotes;
    private String prioridade;
    private String status;
    private String cep;
    private String bairro;
    private String cidade;
    private String uf;

    public Coleta() {
    }

    public Coleta(Long id, String nome, String endereco, Integer pacotes, String prioridade, String status) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.pacotes = pacotes;
        this.prioridade = prioridade;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Integer getPacotes() {
        return pacotes;
    }

    public void setPacotes(Integer pacotes) {
        this.pacotes = pacotes;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    @Override
    public String toString() {
        return "Coleta{id=%d, nome='%s', endereco='%s', pacotes=%d, prioridade='%s', status='%s', cep='%s', bairro='%s', cidade='%s', uf='%s'}"
                .formatted(id, nome, endereco, pacotes, prioridade, status, cep, bairro, cidade, uf);
    }
}
