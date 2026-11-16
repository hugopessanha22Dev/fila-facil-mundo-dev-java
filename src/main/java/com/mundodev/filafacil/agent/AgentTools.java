package com.mundodev.filafacil.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.inference.tools.OperationalTools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AgentTools {

    public static final String AUSENTE = "ausente";
    public static final Set<String> PERMITIDAS = Set.of(
            "consultar_coleta",
            "consultar_entrega",
            "buscar_cliente",
            "registrar_ocorrencia",
            "consultar_regras",
            "notificar",
            "publicar_fila",
            "consultar_historico",
            "consultar_portal",
            "calcular_prioridade",
            "escalar_humano");

    private static final Map<String, Set<String>> PERMISSOES = carregarPermissoes();

    private final OpsWorld world;
    private final RagRules rules;
    private final OperationalTools triage;

    public AgentTools(OpsWorld world, RagRules rules, OperationalTools triage) {
        this.world = world;
        this.rules = rules;
        this.triage = triage;
    }

    public OpsWorld world() {
        return world;
    }

    public ToolOutcome call(String name, Map<String, Object> args) {
        return call(name, args, "operador");
    }

    public ToolOutcome call(String name, Map<String, Object> args, String papel) {
        if (!PERMITIDAS.contains(name)) {
            return ToolOutcome.blocked("fora_escopo");
        }
        if (!PERMISSOES.getOrDefault(papel, Set.of()).contains(name)) {
            return ToolOutcome.blocked("sem_permissao");
        }
        ToolOutcome outcome = switch (name) {
            case "consultar_coleta", "consultar_entrega" -> consultar(text(args, "codigo"));
            case "buscar_cliente" -> buscarCliente(text(args, "id"));
            case "registrar_ocorrencia" -> registrar(text(args, "codigo"), text(args, "tipo"), text(args, "descricao"));
            case "consultar_regras" -> regras(text(args, "pergunta"));
            case "notificar" -> notificar(text(args, "codigo"), text(args, "mensagem"));
            case "publicar_fila" -> publicarFila(text(args, "codigo"), text(args, "mensagem"));
            case "consultar_historico" -> historico(text(args, "codigo"));
            case "consultar_portal" -> portal(text(args, "codigo"));
            case "calcular_prioridade" -> prioridade(text(args, "tipo"), text(args, "sentimento"));
            case "escalar_humano" -> escalar(text(args, "motivo"), text(args, "codigo"), text(args, "estado"), text(args, "dados"));
            default -> ToolOutcome.blocked("fora_escopo");
        };
        return outcome.comPermissao();
    }

    private ToolOutcome consultar(String codigo) {
        if (codigo.isBlank()) {
            return ToolOutcome.invalid();
        }
        if (world.consultaIndisponivel()) {
            Map<String, Object> data = base();
            data.put("encontrada", false);
            data.put("status", AUSENTE);
            data.put("cliente", AUSENTE);
            return ToolOutcome.failed("indisponivel", data);
        }
        if (world.consumirFalhaTemporaria()) {
            Map<String, Object> data = base();
            data.put("encontrada", false);
            data.put("status", AUSENTE);
            data.put("cliente", AUSENTE);
            return ToolOutcome.failed("temporario", data);
        }
        if (world.erroPermanente()) {
            Map<String, Object> data = base();
            data.put("encontrada", false);
            data.put("status", AUSENTE);
            data.put("cliente", AUSENTE);
            return ToolOutcome.failed("permanente", data);
        }
        OpsWorld.Delivery delivery = world.delivery(codigo);
        if (delivery == null && codigo.chars().allMatch(Character::isDigit)) {
            Map<String, Object> mock = triage.consultarStatusEntrega(codigo);
            boolean found = Boolean.TRUE.equals(mock.get("encontrada"));
            Map<String, Object> data = base();
            data.put("encontrada", found);
            data.put("status", found ? String.valueOf(mock.get("status")) : AUSENTE);
            data.put("cliente", AUSENTE);
            data.put("clienteId", AUSENTE);
            return ToolOutcome.ok(data);
        }
        Map<String, Object> data = base();
        if (delivery == null) {
            data.put("encontrada", false);
            data.put("status", AUSENTE);
            data.put("cliente", AUSENTE);
            data.put("clienteId", AUSENTE);
            return ToolOutcome.ok(data);
        }
        data.put("encontrada", true);
        data.put("status", delivery.status());
        data.put("cliente", delivery.clienteNome());
        data.put("clienteId", delivery.clienteId());
        data.put("codigo", delivery.codigo());
        return ToolOutcome.ok(data);
    }

    private ToolOutcome buscarCliente(String id) {
        if (id.isBlank()) {
            return ToolOutcome.invalid();
        }
        OpsWorld.Client client = world.client(id);
        Map<String, Object> data = base();
        if (client == null) {
            data.put("encontrada", false);
            data.put("cliente", AUSENTE);
            return ToolOutcome.ok(data);
        }
        data.put("encontrada", true);
        data.put("cliente", client.nome());
        data.put("clienteId", client.id());
        return ToolOutcome.ok(data);
    }

    private ToolOutcome registrar(String codigo, String tipo, String descricao) {
        if (codigo.isBlank() || tipo.isBlank() || descricao.isBlank()) {
            return ToolOutcome.invalid();
        }
        if (world.erroEscrita()) {
            return new ToolOutcome(false, true, "erro_escrita", base(), List.of("confirmado", "erro_escrita"));
        }
        world.addOccurrence(new OpsWorld.Occurrence(codigo, tipo, descricao));
        triage.registrarOcorrencia(codigo, descricao);
        Map<String, Object> data = base();
        data.put("ocorrencia", "registrada");
        data.put("codigo", codigo);
        return new ToolOutcome(true, true, "", data, List.of("confirmado", "gravado"));
    }

    private ToolOutcome regras(String pergunta) {
        if (pergunta.isBlank()) {
            return ToolOutcome.invalid();
        }
        RagRules.Hit hit = rules.lookup(pergunta);
        Map<String, Object> data = base();
        data.put("encontrada", hit.encontrada());
        data.put("trecho", hit.encontrada() ? hit.trecho() : AUSENTE);
        data.put("citacao", hit.encontrada() ? hit.citacao() : AUSENTE);
        return ToolOutcome.ok(data);
    }

    private ToolOutcome notificar(String codigo, String mensagem) {
        if (codigo.isBlank() || mensagem.isBlank()) {
            return ToolOutcome.invalid();
        }
        String payload = codigo + " | " + mensagem;
        world.addNotice(new OpsWorld.Notice(payload, System.currentTimeMillis(), "sucesso"));
        Map<String, Object> data = base();
        data.put("notificacao", "enfileirada");
        data.put("payload", payload);
        return new ToolOutcome(true, true, "", data, List.of("confirmado", "enfileirado"));
    }

    private ToolOutcome publicarFila(String codigo, String mensagem) {
        if (codigo.isBlank() || mensagem.isBlank()) {
            return ToolOutcome.invalid();
        }
        String payload = codigo + " | " + mensagem;
        world.addNotice(new OpsWorld.Notice(payload, System.currentTimeMillis(), "fila"));
        Map<String, Object> data = base();
        data.put("publicacao", "publicada");
        data.put("payload", payload);
        return ToolOutcome.ok(data);
    }

    private ToolOutcome historico(String codigo) {
        if (codigo.isBlank()) {
            return ToolOutcome.invalid();
        }
        List<String> eventos = world.occurrences().stream()
                .filter(item -> codigo.equals(item.codigo()))
                .map(item -> item.tipo() + ":" + item.descricao())
                .toList();
        Map<String, Object> data = base();
        data.put("eventos", eventos);
        data.put("encontrada", !eventos.isEmpty());
        return ToolOutcome.ok(data);
    }

    private ToolOutcome portal(String codigo) {
        if (codigo.isBlank()) {
            return ToolOutcome.invalid();
        }
        OpsWorld.Delivery delivery = world.delivery(codigo);
        Map<String, Object> data = base();
        if (delivery == null) {
            data.put("encontrada", false);
            data.put("pagina", AUSENTE);
            return ToolOutcome.ok(data);
        }
        data.put("encontrada", true);
        data.put("pagina", "portal interno " + codigo);
        data.put("status", delivery.status());
        return ToolOutcome.ok(data);
    }

    private ToolOutcome prioridade(String tipo, String sentimento) {
        String value = triage.calcularPrioridadeOperacional(tipo, sentimento);
        Map<String, Object> data = base();
        data.put("prioridade", value);
        return ToolOutcome.ok(data);
    }

    private ToolOutcome escalar(String motivo, String codigo, String estado, String dados) {
        if (motivo.isBlank()) {
            return ToolOutcome.invalid();
        }
        String estadoValor = estado.isBlank() ? AUSENTE : estado;
        String dadosValor = dados.isBlank() ? AUSENTE : dados;
        triage.encaminharAtendimentoHumano(motivo, codigo);
        world.addEscalation(new OpsWorld.Escalation(motivo, estadoValor, dadosValor, codigo));
        Map<String, Object> data = base();
        data.put("motivo", motivo);
        data.put("estado", estadoValor);
        data.put("dados", dadosValor);
        data.put("codigo", codigo.isBlank() ? AUSENTE : codigo);
        return ToolOutcome.ok(data);
    }

    private static Map<String, Object> base() {
        return new LinkedHashMap<>();
    }

    private static String text(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public record ToolOutcome(
            boolean ok,
            boolean executada,
            String erro,
            Map<String, Object> dados,
            List<String> eventos) {

        public static ToolOutcome ok(Map<String, Object> dados) {
            return new ToolOutcome(true, true, "", dados, List.of());
        }

        public static ToolOutcome failed(String erro, Map<String, Object> dados) {
            return new ToolOutcome(false, true, erro, dados, List.of(erro));
        }

        public static ToolOutcome invalid() {
            return new ToolOutcome(false, false, "argumento_invalido", Map.of(), List.of("argumento_invalido"));
        }

        public static ToolOutcome blocked(String erro) {
            return new ToolOutcome(false, false, erro, Map.of(), List.of(erro));
        }

        public ToolOutcome comPermissao() {
            List<String> conferida = new ArrayList<>();
            conferida.add("permissao_conferida");
            conferida.addAll(eventos);
            return new ToolOutcome(ok, executada, erro, dados, List.copyOf(conferida));
        }
    }

    private static Map<String, Set<String>> carregarPermissoes() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = AgentTools.class.getResourceAsStream("/agent/permissoes.json")) {
            if (in == null) {
                throw new IllegalStateException("permissoes.json ausente");
            }
            Map<String, Set<String>> papeis = new LinkedHashMap<>();
            JsonNode root = mapper.readTree(in);
            root.fieldNames().forEachRemaining(papel -> {
                Set<String> nomes = new java.util.LinkedHashSet<>();
                root.path(papel).forEach(item -> nomes.add(item.asText()));
                papeis.put(papel, Set.copyOf(nomes));
            });
            return Map.copyOf(papeis);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler permissoes.json", ex);
        }
    }
}
