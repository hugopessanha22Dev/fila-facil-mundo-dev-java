package com.mundodev.filafacil.inference.tools;

import com.mundodev.filafacil.coletas.Coleta;
import com.mundodev.filafacil.state.AppState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MockOperationalTools implements OperationalTools {

    private final AppState state;

    public MockOperationalTools(AppState state) {
        this.state = state;
    }

    @Override
    public Map<String, Object> consultarStatusEntrega(String codigoColeta) {
        if (codigoColeta == null || codigoColeta.isBlank()) {
            throw new ToolError("INVALID_INPUT", "codigoColeta e obrigatorio.");
        }
        Coleta coleta = findByCodigoOrId(codigoColeta);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("codigoColeta", codigoColeta);
        out.put("status", coleta != null ? coleta.getStatus() : "desconhecido");
        out.put("encontrada", coleta != null);
        return out;
    }

    @Override
    public Coleta buscarColeta(long id) {
        if (id <= 0) {
            throw new ToolError("INVALID_INPUT", "Id invalido.");
        }
        return state.getColetas().stream()
                .filter(c -> id == c.getId())
                .findFirst()
                .orElseThrow(() -> new ToolError("NOT_FOUND", "Coleta nao encontrada para id " + id));
    }

    @Override
    public Map<String, Object> registrarOcorrencia(String codigoColeta, String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new ToolError("INVALID_INPUT", "descricao e obrigatoria.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("mock", true);
        out.put("codigoColeta", codigoColeta);
        out.put("descricao", descricao);
        System.out.println("[tool-mock] registrarOcorrencia codigo=" + codigoColeta);
        return out;
    }

    @Override
    public String calcularPrioridadeOperacional(String tipoSolicitacao, String sentimento) {
        String tipo = tipoSolicitacao == null ? "" : tipoSolicitacao.toLowerCase(Locale.ROOT);
        String sent = sentimento == null ? "" : sentimento.toLowerCase(Locale.ROOT);
        if (tipo.contains("reclam") || sent.contains("urgente") || sent.contains("insatisfeito")) {
            return "alta";
        }
        if (tipo.contains("ocorrencia") || sent.contains("ansioso")) {
            return "media";
        }
        return "baixa";
    }

    @Override
    public Map<String, Object> encaminharAtendimentoHumano(String motivo, String codigoColeta) {
        if (motivo == null || motivo.isBlank()) {
            throw new ToolError("INVALID_INPUT", "motivo e obrigatorio.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("mock", true);
        out.put("fila", "atendimento_humano");
        out.put("motivo", motivo);
        out.put("codigoColeta", codigoColeta);
        System.out.println("[tool-mock] encaminharHumano motivo=" + motivo);
        return out;
    }

    private Coleta findByCodigoOrId(String codigoColeta) {
        List<Coleta> coletas = state.getColetas();
        for (Coleta coleta : coletas) {
            if (codigoColeta.equalsIgnoreCase(String.valueOf(coleta.getId()))) {
                return coleta;
            }
        }
        return null;
    }
}
