package com.mundodev.filafacil.manifest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManifestValidator {

    public ValidationReport validate(List<ManifestRecord> records) {
        List<ManifestRecord> safe = records == null ? List.of() : records;
        List<ValidationError> erros = new ArrayList<>();
        Set<String> pacotesVistos = new HashSet<>();

        for (int i = 0; i < safe.size(); i++) {
            ManifestRecord record = safe.get(i);
            int linha = i + 1;
            String pacote = safeValue(record == null ? null : record.pacote());

            if (isBlank(record == null ? null : record.pacote())) {
                erros.add(new ValidationError(linha, pacote, "CAMPO_OBRIGATORIO", "Campo pacote ausente ou vazio."));
            } else if (!pacotesVistos.add(record.pacote().trim())) {
                erros.add(new ValidationError(linha, pacote, "PACOTE_DUPLICADO",
                        "Pacote duplicado: " + record.pacote().trim()));
            }

            if (isBlank(record == null ? null : record.rota())) {
                erros.add(new ValidationError(linha, pacote, "ROTA_VAZIA", "Campo rota ausente ou vazio."));
            }
            if (isBlank(record == null ? null : record.veiculo())) {
                erros.add(new ValidationError(linha, pacote, "SEM_VEICULO", "Campo veiculo ausente ou vazio."));
            }
            if (isBlank(record == null ? null : record.responsavel())) {
                erros.add(new ValidationError(linha, pacote, "SEM_RESPONSAVEL",
                        "Campo responsavel ausente ou vazio."));
            }
            if (isBlank(record == null ? null : record.status())) {
                erros.add(new ValidationError(linha, pacote, "CAMPO_OBRIGATORIO", "Campo status ausente ou vazio."));
            }
        }

        String status = erros.isEmpty() ? "aprovado" : "reprovado";
        return new ValidationReport(safe.size(), List.copyOf(erros), status);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
