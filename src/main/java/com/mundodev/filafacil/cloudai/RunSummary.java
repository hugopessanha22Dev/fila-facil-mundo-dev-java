package com.mundodev.filafacil.cloudai;

public record RunSummary(
        int tokens,
        int chamadas,
        long latenciaMs,
        int erros,
        long storageBytes,
        int fila,
        double custo,
        boolean alerta) {

    public String render() {
        String line = "tokens=" + tokens
                + " chamadas=" + chamadas
                + " latenciaMs=" + latenciaMs
                + " erros=" + erros
                + " storageBytes=" + storageBytes
                + " fila=" + fila
                + " custo=" + custo;
        if (alerta) {
            return line + "\nALERTA tokens acima do limiar do ambiente";
        }
        return line;
    }
}
