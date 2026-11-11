package com.mundodev.filafacil.agent;

import com.mundodev.filafacil.inference.tools.MockOperationalTools;
import com.mundodev.filafacil.inference.tools.OperationalTools;
import com.mundodev.filafacil.state.AppState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgentLoop {

    private static final Pattern CODIGO = Pattern.compile("(?i)\\bcodigo\\s+(\\S+)");
    private static final Pattern TIPO = Pattern.compile("(?i)\\btipo\\s+(\\S+)");
    private static final Pattern DESCRICAO = Pattern.compile("(?i)\\bdescricao\\s+(.+)$");

    private final AgentTools tools;
    private final AgentLimits limits;

    public AgentLoop(AgentTools tools, AgentLimits limits) {
        this.tools = tools;
        this.limits = limits;
    }

    public static AgentLoop create(RagRules rules, AgentLimits limits, OperationalTools triage) {
        return new AgentLoop(new AgentTools(OpsWorld.demo(), rules, triage), limits);
    }

    public static AgentLoop create(RagRules rules, AgentLimits limits) {
        return create(rules, limits, new MockOperationalTools(new AppState()));
    }

    public Run run(String texto) {
        return execute(texto);
    }

    public Run run(String texto, boolean consultaIndisponivel, boolean erroEscrita) {
        tools.world().setConsultaIndisponivel(consultaIndisponivel);
        tools.world().setErroEscrita(erroEscrita);
        return execute(texto);
    }

    private Run execute(String texto) {
        long start = System.nanoTime();
        String message = texto == null ? "" : texto.trim();
        Intent intent = classify(message);
        State state = new State(field(CODIGO, message), field(TIPO, message), field(DESCRICAO, message));
        List<Step> trace = new ArrayList<>();
        if (intent == Intent.FORA) {
            return finish("fora_dominio", state, trace, start, "Recusar fora do dominio logistico.");
        }
        if (needsCode(intent) && state.codigo.isBlank()) {
            return finish("dado_ausente", state, trace, start, "Pedir o codigo da coleta ou entrega.");
        }
        int calls = 0;
        while (true) {
            if (trace.size() >= limits.maxSteps() || calls >= limits.maxCalls()) {
                return finish("limite", state, trace, start, "Encerrar por limite de execucao.");
            }
            Planned planned = next(intent, state);
            if (planned == null) {
                if (state.escalou) {
                    return finish("escalado", state, trace, start, "Aguardar atendimento humano.");
                }
                return finish("resolvido", state, trace, start, proximaResolvida(intent));
            }
            if (!AgentTools.PERMITIDAS.contains(planned.tool())) {
                return finish("fora_escopo", state, trace, start, "Bloquear ferramenta fora da lista.");
            }
            if (repeats(trace, planned)) {
                Step last = trace.get(trace.size() - 1);
                trace.set(trace.size() - 1, last.withDecisao("parar:loop"));
                return finish("loop", state, trace, start, "Encerrar por repeticao do mesmo passo.");
            }
            AgentTools.ToolOutcome outcome = tools.call(planned.tool(), planned.args());
            apply(state, planned.tool(), outcome);
            String stop = stopReason(planned.tool(), outcome, state);
            String decisao = stop == null ? preview(intent, state) : "parar:" + stop;
            String resultado = outcome.ok() ? "ok" : outcome.erro();
            Object citacao = outcome.dados().get("citacao");
            if (citacao != null && !AgentTools.AUSENTE.equals(citacao)) {
                resultado = resultado + " " + citacao;
            }
            trace.add(new Step(
                    trace.size() + 1,
                    intent.name().toLowerCase(Locale.ROOT),
                    planned.tool(),
                    planned.args(),
                    resultado,
                    decisao,
                    outcome.executada(),
                    outcome.ok(),
                    outcome.eventos()));
            if (outcome.executada()) {
                calls++;
            }
            if (stop != null) {
                return finish(stop, state, trace, start, proximaParada(stop));
            }
        }
    }

    private String stopReason(String tool, AgentTools.ToolOutcome outcome, State state) {
        if ("argumento_invalido".equals(outcome.erro())) {
            return "argumento_invalido";
        }
        if (!outcome.ok() && outcome.executada()) {
            int attempts = state.failures.getOrDefault(tool, 0);
            if (attempts >= limits.maxAttemptsPerTool()) {
                return "indisponivel".equals(outcome.erro()) ? "ferramenta_indisponivel" : "falha_ferramenta";
            }
            return null;
        }
        if ("consultar_coleta".equals(tool) && outcome.ok() && !state.encontrada) {
            return "nao_encontrado";
        }
        return null;
    }

    private void apply(State state, String tool, AgentTools.ToolOutcome outcome) {
        if (!outcome.ok() && outcome.executada()) {
            state.failures.merge(tool, 1, Integer::sum);
            return;
        }
        if (!outcome.ok()) {
            if ("registrar_ocorrencia".equals(tool)) {
                state.recusada = true;
            }
            return;
        }
        Map<String, Object> data = outcome.dados();
        switch (tool) {
            case "consultar_coleta" -> {
                state.consultada = true;
                state.encontrada = Boolean.TRUE.equals(data.get("encontrada"));
                if (state.encontrada) {
                    state.status = String.valueOf(data.get("status"));
                    state.cliente = String.valueOf(data.get("cliente"));
                    state.clienteId = String.valueOf(data.getOrDefault("clienteId", ""));
                }
            }
            case "buscar_cliente" -> {
                state.clienteBuscado = true;
                if (Boolean.TRUE.equals(data.get("encontrada"))) {
                    state.cliente = String.valueOf(data.get("cliente"));
                }
            }
            case "registrar_ocorrencia" -> state.registrada = true;
            case "consultar_regras" -> {
                state.regras = true;
                if (Boolean.TRUE.equals(data.get("encontrada"))) {
                    state.regra = String.valueOf(data.get("citacao"));
                }
            }
            case "calcular_prioridade" -> {
                state.prioridadeFeita = true;
                state.prioridade = String.valueOf(data.get("prioridade"));
            }
            case "notificar" -> state.notificou = true;
            case "escalar_humano" -> state.escalou = true;
            default -> {
            }
        }
    }

    private Planned next(Intent intent, State state) {
        if (intent == Intent.PROIBIDA) {
            return new Planned("apagar_banco", Map.of());
        }
        if (intent == Intent.CRITICO) {
            if (!state.codigo.isBlank() && !state.consultada) {
                return consulta(state);
            }
            if (!state.escalou) {
                return escala(state, "Caso ambiguo ou critico");
            }
            return null;
        }
        if (!state.consultada) {
            return consulta(state);
        }
        if (intent == Intent.LOOP) {
            return consulta(state);
        }
        if (intent == Intent.ATRASO) {
            if (!state.regras) {
                return new Planned("consultar_regras", Map.of("pergunta", "atraso aviso"));
            }
            if (!state.prioridadeFeita) {
                return new Planned("calcular_prioridade", Map.of("tipo", "atraso", "sentimento", ""));
            }
            return null;
        }
        if (!state.clienteBuscado && !state.clienteId.isBlank()) {
            return new Planned("buscar_cliente", Map.of("id", state.clienteId));
        }
        if (!state.registrada && !state.recusada) {
            return new Planned("registrar_ocorrencia", Map.of(
                    "codigo", state.codigo,
                    "tipo", state.tipo.isBlank() ? intent.name().toLowerCase(Locale.ROOT) : state.tipo,
                    "descricao", state.descricao));
        }
        if (state.recusada || !state.registrada) {
            return null;
        }
        if (!state.prioridadeFeita) {
            return new Planned("calcular_prioridade", Map.of("tipo", intent.name().toLowerCase(Locale.ROOT), "sentimento", ""));
        }
        if (!state.notificou) {
            return new Planned("notificar", Map.of(
                    "codigo", state.codigo,
                    "mensagem", "Ocorrencia registrada para " + state.codigo));
        }
        return null;
    }

    private String preview(Intent intent, State state) {
        if (state.escalou) {
            return "parar:escalado";
        }
        Planned planned = next(intent, state);
        return planned == null ? "parar:resolvido" : "proxima:" + planned.tool();
    }

    private static Planned consulta(State state) {
        return new Planned("consultar_coleta", Map.of("codigo", state.codigo));
    }

    private static Planned escala(State state, String motivo) {
        String dados = "status=" + blank(state.status) + "; cliente=" + blank(state.cliente);
        return new Planned("escalar_humano", Map.of(
                "motivo", motivo,
                "codigo", state.codigo,
                "estado", "critico",
                "dados", dados));
    }

    private static boolean repeats(List<Step> trace, Planned planned) {
        if (trace.isEmpty()) {
            return false;
        }
        Step last = trace.get(trace.size() - 1);
        return last.executada() && last.ok() && last.ferramenta().equals(planned.tool())
                && last.argumentos().equals(planned.args());
    }

    private static boolean needsCode(Intent intent) {
        return intent == Intent.ATRASO || intent == Intent.ENDERECO || intent == Intent.COLETA
                || intent == Intent.OCORRENCIA || intent == Intent.LOOP;
    }

    private static Intent classify(String texto) {
        String lower = texto.toLowerCase(Locale.ROOT);
        if (lower.contains("apagar_banco")) {
            return Intent.PROIBIDA;
        }
        if (lower.contains("repetir")) {
            return Intent.LOOP;
        }
        if (containsAny(lower, "critico", "critica", "risco", "ambigua", "ambiguo", "incompleta")) {
            return Intent.CRITICO;
        }
        if (!containsAny(lower, "entrega", "atras", "endereco", "coleta", "ocorrencia", "cliente", "motorista", "pacote")) {
            return Intent.FORA;
        }
        if (lower.contains("endereco")) {
            return Intent.ENDERECO;
        }
        if (lower.contains("coleta") || lower.contains("motorista")) {
            return Intent.COLETA;
        }
        if (lower.contains("ocorrencia")) {
            return Intent.OCORRENCIA;
        }
        if (lower.contains("atras") || lower.contains("entrega")) {
            return Intent.ATRASO;
        }
        return Intent.FORA;
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String field(Pattern pattern, String texto) {
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private Run finish(String parada, State state, List<Step> trace, long start, String proxima) {
        long latencia = Math.max(0, (System.nanoTime() - start) / 1_000_000);
        List<String> acoes = new ArrayList<>();
        int falhas = 0;
        int escalonamentos = 0;
        boolean aderente = true;
        for (Step step : trace) {
            if (step.executada()) {
                acoes.add(step.ferramenta());
            }
            if (step.executada() && !step.ok()) {
                falhas++;
            }
            if (step.executada() && step.ok() && "escalar_humano".equals(step.ferramenta())) {
                escalonamentos++;
            }
            int gravado = step.eventos().indexOf("gravado");
            int confirmado = step.eventos().indexOf("confirmado");
            if (gravado >= 0 && confirmado < 0) {
                aderente = false;
            }
            if (gravado >= 0 && confirmado > gravado) {
                aderente = false;
            }
        }
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("status", blank(state.status));
        dados.put("cliente", blank(state.cliente));
        dados.put("regra", blank(state.regra));
        dados.put("prioridade", blank(state.prioridade));
        dados.put("ocorrencia", state.registrada ? "registrada" : AgentTools.AUSENTE);
        dados.put("notificacao", state.notificou ? "enfileirada" : AgentTools.AUSENTE);
        return new Run(
                parada,
                parada,
                List.copyOf(acoes),
                dados,
                proxima,
                List.copyOf(trace),
                "resolvido".equals(parada),
                falhas,
                escalonamentos,
                aderente,
                latencia,
                acoes.size());
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? AgentTools.AUSENTE : value;
    }

    private static String proximaResolvida(Intent intent) {
        if (intent == Intent.ATRASO) {
            return "Informar o cliente sobre o status e a regra citada.";
        }
        return "Acompanhar a ocorrencia registrada e a notificacao enviada.";
    }

    private static String proximaParada(String parada) {
        return switch (parada) {
            case "nao_encontrado" -> "Conferir o codigo com o cliente.";
            case "escalado" -> "Aguardar atendimento humano.";
            case "ferramenta_indisponivel" -> "Tentar novamente quando a consulta voltar.";
            case "falha_ferramenta" -> "Nao registrar de novo e revisar a falha de escrita.";
            case "argumento_invalido" -> "Pedir os dados minimos antes de registrar.";
            case "limite" -> "Encerrar por limite de execucao.";
            default -> "Encerrar o fluxo.";
        };
    }

    private enum Intent {
        FORA, PROIBIDA, LOOP, CRITICO, ATRASO, ENDERECO, COLETA, OCORRENCIA
    }

    private static final class State {
        private final String codigo;
        private final String tipo;
        private final String descricao;
        private final Map<String, Integer> failures = new LinkedHashMap<>();
        private boolean consultada;
        private boolean encontrada;
        private boolean clienteBuscado;
        private boolean registrada;
        private boolean recusada;
        private boolean regras;
        private boolean prioridadeFeita;
        private boolean notificou;
        private boolean escalou;
        private String clienteId = "";
        private String status = "";
        private String cliente = "";
        private String regra = "";
        private String prioridade = "";

        private State(String codigo, String tipo, String descricao) {
            this.codigo = codigo;
            this.tipo = tipo;
            this.descricao = descricao;
        }
    }

    private record Planned(String tool, Map<String, Object> args) {
    }

    public record Step(
            int indice,
            String interpretacao,
            String ferramenta,
            Map<String, Object> argumentos,
            String resultado,
            String decisao,
            boolean executada,
            boolean ok,
            List<String> eventos) {

        public Step withDecisao(String nova) {
            return new Step(indice, interpretacao, ferramenta, argumentos, resultado, nova, executada, ok, eventos);
        }
    }

    public record Run(
            String status,
            String parada,
            List<String> acoes,
            Map<String, String> dados,
            String proximaEtapa,
            List<Step> trace,
            boolean sucesso,
            int falhas,
            int escalonamentos,
            boolean aderente,
            long latenciaMs,
            int custoEstimado) {

        public String render() {
            StringBuilder out = new StringBuilder();
            out.append("status: ").append(status).append('\n');
            out.append("parada: ").append(parada).append('\n');
            out.append("acoes: ").append(String.join(", ", acoes)).append('\n');
            dados.forEach((key, value) -> out.append("dados.").append(key).append(": ").append(value).append('\n'));
            out.append("proximaEtapa: ").append(proximaEtapa).append('\n');
            out.append("custoEstimado: ").append(custoEstimado).append('\n');
            out.append("latenciaMs: ").append(latenciaMs).append('\n');
            out.append("passos: ").append(trace.size()).append('\n');
            out.append("falhas: ").append(falhas).append('\n');
            out.append("escalonamentos: ").append(escalonamentos).append('\n');
            out.append("aderencia: ").append(aderente).append('\n');
            for (Step step : trace) {
                out.append("trace: ").append(step.indice()).append(' ').append(step.ferramenta())
                        .append(' ').append(step.resultado()).append(' ').append(step.decisao()).append('\n');
            }
            return out.toString();
        }
    }
}
