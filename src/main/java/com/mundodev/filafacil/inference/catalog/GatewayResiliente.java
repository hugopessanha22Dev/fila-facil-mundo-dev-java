package com.mundodev.filafacil.inference.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GatewayResiliente {

    public static final String NOME = "gateway-central";

    private final ProvedorInferencia provedor;
    private final LimitesInferencia limites;
    private final Relogio relogio;
    private final Map<String, Integer> chamadasPorTarefa = new HashMap<>();
    private final List<ExecucaoInferencia> historico = new ArrayList<>();
    private final List<Falha> falhas = new ArrayList<>();
    private int chamadasProvedor;
    private int falhasSeguidas;
    private Estado estado = Estado.FECHADO;
    private long abertoEm;

    public GatewayResiliente(ProvedorInferencia provedor, LimitesInferencia limites, Relogio relogio) {
        this.provedor = provedor;
        this.limites = limites;
        this.relogio = relogio;
    }

    public static GatewayResiliente mockPadrao() {
        return new GatewayResiliente(new MockCatalogo(), LimitesInferencia.carregar(), new Relogio.Sistema());
    }

    public ExecucaoInferencia executar(String tarefa, String texto) {
        if (circuitoBloqueia()) {
            return registrar(recusa(tarefa, "circuito_aberto", 0, 0, 0, 0));
        }
        int usadas = chamadasPorTarefa.getOrDefault(tarefa, 0);
        if (usadas >= limites.rateLimitPorTarefa() || chamadasProvedor >= limites.rateLimitPorProvedor()) {
            return registrar(recusa(tarefa, "rate_limit", 0, 0, 0, 0));
        }
        chamadasPorTarefa.merge(tarefa, 1, Integer::sum);

        int tentativas = 0;
        long espera = limites.backoffInicialMs();
        long latencia = 0;
        double custo = 0;
        int tokens = 0;
        boolean houveFalha = false;
        String tipo = "provedor";
        while (tentativas < limites.maxTentativas()) {
            if (tentativas > 0) {
                relogio.esperar(espera);
                espera = espera * 2L;
            }
            if (circuitoBloqueia()) {
                tipo = "circuito_aberto";
                break;
            }
            if (chamadasProvedor >= limites.rateLimitPorProvedor()) {
                tipo = "rate_limit";
                break;
            }
            chamadasProvedor++;
            tentativas++;
            RespostaProvedor resposta = provedor.completar(tarefa, texto);
            latencia += resposta.latenciaMs();
            custo += resposta.custo();
            tokens += resposta.tokens();
            tipo = classificar(tarefa, resposta);
            if ("nenhuma".equals(tipo)) {
                if (estado == Estado.MEIO_ABERTO) {
                    estado = Estado.FECHADO;
                }
                falhasSeguidas = 0;
                return registrar(new ExecucaoInferencia(
                        NOME, tarefa, resposta.texto(), true, false, houveFalha, "nenhuma",
                        latencia, custo, tokens, estado.name(), tentativas));
            }
            houveFalha = true;
            falhas.add(new Falha(tarefa, tipo));
            falhasSeguidas++;
            if (falhasSeguidas >= limites.limiarCircuito()) {
                estado = Estado.ABERTO;
                abertoEm = relogio.agoraMs();
            }
        }
        return registrar(new ExecucaoInferencia(
                NOME, tarefa, null, false, true, false, tipo,
                latencia, custo, tokens, estado.name(), tentativas));
    }

    public List<ExecucaoInferencia> porTarefa(String tarefa) {
        return historico.stream().filter(execucao -> tarefa.equals(execucao.tarefa())).toList();
    }

    public ResumoMetricas resumo(String tarefa) {
        long latencia = 0;
        double custo = 0;
        int tokens = 0;
        int erros = 0;
        int fallbacks = 0;
        for (ExecucaoInferencia execucao : porTarefa(tarefa)) {
            latencia += execucao.latenciaMs();
            custo += execucao.custo();
            tokens += execucao.tokens();
            if (execucao.erro()) {
                erros++;
            }
            if (execucao.fallback()) {
                fallbacks++;
            }
        }
        Map<String, Integer> porTipo = new HashMap<>();
        for (Falha falha : falhas) {
            if (tarefa.equals(falha.tarefa())) {
                porTipo.merge(falha.tipo(), 1, Integer::sum);
            }
        }
        return new ResumoMetricas(tarefa, latencia, custo, tokens, erros, fallbacks, Map.copyOf(porTipo));
    }

    public String estado() {
        return estado.name();
    }

    private ExecucaoInferencia registrar(ExecucaoInferencia execucao) {
        historico.add(execucao);
        return execucao;
    }

    private ExecucaoInferencia recusa(String tarefa, String tipo, long latencia, double custo, int tokens, int tentativas) {
        falhas.add(new Falha(tarefa, tipo));
        return new ExecucaoInferencia(
                NOME, tarefa, null, false, true, false, tipo,
                latencia, custo, tokens, estado.name(), tentativas);
    }

    private boolean circuitoBloqueia() {
        if (estado == Estado.FECHADO) {
            return false;
        }
        if (estado == Estado.ABERTO && relogio.agoraMs() >= abertoEm + limites.cooldownMs()) {
            estado = Estado.MEIO_ABERTO;
            return false;
        }
        return estado == Estado.ABERTO;
    }

    private String classificar(String tarefa, RespostaProvedor resposta) {
        if (resposta.falha()) {
            return resposta.tipoFalha() == null || resposta.tipoFalha().isBlank()
                    ? "provedor"
                    : resposta.tipoFalha();
        }
        if (resposta.latenciaMs() > limites.timeoutMs()) {
            return "timeout";
        }
        if (exigeJson(tarefa) && !Recursos.jsonValido(resposta.texto())) {
            return "json_invalido";
        }
        return "nenhuma";
    }

    private static boolean exigeJson(String tarefa) {
        return "extracao".equals(tarefa) || "resposta_assistida".equals(tarefa);
    }

    private enum Estado {
        FECHADO,
        ABERTO,
        MEIO_ABERTO
    }

    private record Falha(String tarefa, String tipo) {
    }
}
