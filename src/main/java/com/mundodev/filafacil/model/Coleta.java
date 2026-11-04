package com.mundodev.filafacil.model;

public class Coleta {

    private Long id;
    private String nome;
    private String endereco;
    private Integer pacotes;
    private String prioridade;
    private String status;

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

    @Override
    public String toString() {
        return "Coleta{id=%d, nome='%s', endereco='%s', pacotes=%d, prioridade='%s', status='%s'}"
                .formatted(id, nome, endereco, pacotes, prioridade, status);
    }
}
