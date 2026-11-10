package com.mundodev.filafacil.manifest;

import java.util.List;

public record ValidationReport(
        int totalRegistros,
        List<ValidationError> erros,
        String statusFinal) {

    public boolean aprovado() {
        return "aprovado".equalsIgnoreCase(statusFinal);
    }

    public int totalErros() {
        return erros == null ? 0 : erros.size();
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Relatorio de Validacao de Manifesto ===\n");
        sb.append("Total de registros: ").append(totalRegistros).append('\n');
        sb.append("Total de erros: ").append(totalErros()).append('\n');
        sb.append("Status final: ").append(statusFinal).append('\n');
        if (totalErros() > 0) {
            sb.append("Problemas:\n");
            for (ValidationError erro : erros) {
                sb.append(" - linha=").append(erro.linha())
                        .append(" pacote=").append(erro.pacote())
                        .append(" [").append(erro.codigo()).append("] ")
                        .append(erro.mensagem())
                        .append('\n');
            }
        }
        return sb.toString();
    }
}
