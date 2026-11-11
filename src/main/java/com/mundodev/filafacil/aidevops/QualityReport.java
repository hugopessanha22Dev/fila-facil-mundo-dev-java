package com.mundodev.filafacil.aidevops;

import java.util.List;
import java.util.Map;

public record QualityReport(
        int total,
        double accuracy,
        double extraction,
        double validJson,
        double schemaAdherence,
        double summary,
        double costUsd,
        double latencyMs,
        boolean regression,
        Map<String, Integer> failuresByCategory,
        boolean approved,
        List<Stage> stages) {

    public record Stage(String name, boolean approved) {
    }

    public String render() {
        StringBuilder out = new StringBuilder();
        for (Stage stage : stages) {
            out.append("etapa=").append(stage.name())
                    .append(" status=").append(stage.approved() ? "aprovado" : "reprovado")
                    .append('\n');
        }
        out.append("acuracia=").append(accuracy)
                .append(" extracao=").append(extraction)
                .append(" jsonValido=").append(validJson)
                .append(" schema=").append(schemaAdherence)
                .append(" resumo=").append(summary)
                .append(" custo=").append(costUsd)
                .append(" latencia=").append(latencyMs)
                .append(" regressao=").append(regression)
                .append(" falhasPorCategoria=").append(failuresByCategory)
                .append(" aprovado=").append(approved);
        return out.toString();
    }
}
