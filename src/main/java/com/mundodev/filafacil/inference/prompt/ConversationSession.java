package com.mundodev.filafacil.inference.prompt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConversationSession {

    public static final List<String> REQUIRED_FIELDS = List.of(
            "codigoEntrega", "cliente", "status");

    private String intencao;
    private final Map<String, FieldValue> campos = new LinkedHashMap<>();
    private final Set<String> perguntasFeitas = new LinkedHashSet<>();
    private final List<String> historicoMensagens = new ArrayList<>();
    private boolean concluida;

    public ConversationSession() {
        for (String field : List.of("codigoEntrega", "cliente", "endereco", "prazo", "status")) {
            campos.put(field, FieldValue.ausente());
        }
    }

    public void addMensagem(String mensagem) {
        if (mensagem != null && !mensagem.isBlank()) {
            historicoMensagens.add(mensagem);
        }
    }

    public void setIntencao(String intencao) {
        this.intencao = intencao;
    }

    public String getIntencao() {
        return intencao;
    }

    public void putCampo(String nome, FieldValue value) {
        if (value != null) {
            campos.put(nome, value);
        }
    }

    public FieldValue getCampo(String nome) {
        return campos.getOrDefault(nome, FieldValue.ausente());
    }

    public Map<String, FieldValue> getCampos() {
        return Map.copyOf(campos);
    }

    public void marcarPerguntaFeita(String campo) {
        perguntasFeitas.add(campo);
    }

    public boolean perguntaJaFeita(String campo) {
        return perguntasFeitas.contains(campo);
    }

    public Set<String> getPerguntasFeitas() {
        return Set.copyOf(perguntasFeitas);
    }

    public List<String> getHistoricoMensagens() {
        return List.copyOf(historicoMensagens);
    }

    public String historicoTexto() {
        return String.join("\n", historicoMensagens);
    }

    public List<String> camposAusentesObrigatorios() {
        List<String> missing = new ArrayList<>();
        for (String field : REQUIRED_FIELDS) {
            FieldValue value = getCampo(field);
            boolean markedUnknown = "ausente".equals(value.origem()) && value.evidencia() != null
                    && value.evidencia().toLowerCase().contains("desconhecido");
            if (!value.preenchido() && !markedUnknown) {
                missing.add(field);
            }
        }
        return missing;
    }

    public boolean prontaParaConclusao() {
        return camposAusentesObrigatorios().isEmpty();
    }

    public void marcarDesconhecido(String campo, String justificativa) {
        campos.put(campo, FieldValue.desconhecido("desconhecido: " + justificativa));
        perguntasFeitas.add(campo);
    }

    public boolean isConcluida() {
        return concluida;
    }

    public void setConcluida(boolean concluida) {
        this.concluida = concluida;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("intencao=").append(intencao).append('\n');
        for (Map.Entry<String, FieldValue> e : campos.entrySet()) {
            sb.append(e.getKey()).append("=")
                    .append(e.getValue().valor())
                    .append(" (").append(e.getValue().origem()).append(")\n");
        }
        sb.append("perguntasFeitas=").append(perguntasFeitas).append('\n');
        return sb.toString();
    }

    public void clear() {
        intencao = null;
        campos.replaceAll((k, v) -> FieldValue.ausente());
        perguntasFeitas.clear();
        historicoMensagens.clear();
        concluida = false;
    }
}
