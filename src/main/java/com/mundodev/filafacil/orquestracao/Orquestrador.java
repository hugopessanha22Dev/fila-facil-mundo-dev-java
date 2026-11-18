package com.mundodev.filafacil.orquestracao;

import com.mundodev.filafacil.agent.AgentLimits;
import com.mundodev.filafacil.agent.AgentLoop;
import com.mundodev.filafacil.agent.RagRules;
import com.mundodev.filafacil.inference.catalog.ExecucaoInferencia;
import com.mundodev.filafacil.inference.catalog.GatewayResiliente;
import com.mundodev.filafacil.inference.catalog.LimitesInferencia;
import com.mundodev.filafacil.inference.catalog.MockCatalogo;
import com.mundodev.filafacil.inference.catalog.ProvedorInferencia;
import com.mundodev.filafacil.inference.catalog.Relogio;
import com.mundodev.filafacil.inference.catalog.RespostaProvedor;
import com.mundodev.filafacil.rag.ChunkIndex;
import com.mundodev.filafacil.rag.CorpusCatalog;
import com.mundodev.filafacil.rag.RagAnswer;
import com.mundodev.filafacil.rag.RagAssistant;
import com.mundodev.filafacil.rag.RetrievalConfig;
import com.mundodev.filafacil.rag.Retriever;
import com.mundodev.filafacil.rag.SourceDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class Orquestrador {

    public static final String VERSAO = "orquestracao-v1";
    public static final String AUSENCIA = "Nao encontrado nos documentos.";
    public static final List<String> ETAPAS = List.of(
            "intencao", "contexto", "modelo", "ferramenta", "validacao", "encerramento");

    private static final Pattern SEGREDO = Pattern.compile("(?i)(?:gsk_|sk-)[A-Za-z0-9_\\-]+|\\d{11,}");

    private final Path raiz;
    private final ChunkIndex index;
    private final RetrievalConfig config;
    private final FilaTrabalhos fila;
    private final ConsultaEndereco endereco;
    private final Map<String, Registro> registros = new LinkedHashMap<>();
    private int sequencia;

    private Orquestrador(Path raiz, ChunkIndex index, RetrievalConfig config, FilaTrabalhos fila, ConsultaEndereco endereco) {
        this.raiz = raiz;
        this.index = index;
        this.config = config;
        this.fila = fila;
        this.endereco = endereco;
    }

    public static Orquestrador criar(Path raiz) throws IOException {
        return criar(raiz, new RelogioPasso(), 0);
    }

    public static Orquestrador criar(Path raiz, RelogioPasso relogio, int falhasTemporarias) throws IOException {
        RetrievalConfig config = RetrievalConfig.load();
        ChunkIndex index = new ChunkIndex(raiz.resolve("indice"));
        index.load();
        if (index.chunks().isEmpty()) {
            for (SourceDocument doc : new CorpusCatalog().load()) {
                index.publish(doc, config);
            }
        }
        Contratos contratos = Contratos.carregar();
        Contratos.Integracao filaContrato = contratos.obter("fila");
        FilaTrabalhos fila = new FilaTrabalhos(
                raiz.resolve("trabalhos"),
                relogio,
                filaContrato.timeoutMs(),
                filaContrato.tentativas(),
                falhasTemporarias);
        return new Orquestrador(raiz, index, config, fila, new ConsultaEndereco(contratos.obter("ferramenta-externa").limiteUso()));
    }

    public Registro executar(String texto) {
        return executar(texto, false, 0);
    }

    public Registro executar(String texto, boolean vetorIndisponivel, int falhasModelo) {
        Fluxo fluxo = Roteador.classificar(texto);
        String id = "exec-" + (++sequencia);
        String entrada = ocultar(texto);
        if (SEGREDO.matcher(texto == null ? "" : texto).find()) {
            return guardar(base(id, fluxo, entrada, "nao_aplicada", "nenhum", 0, "dado_sensivel", 0, 0, "dado_sensivel", false, "bloqueado", "bloqueado", List.of()));
        }
        if (fluxo == Fluxo.FORA) {
            return guardar(base(id, fluxo, entrada, "nao_aplicada", "nenhum", 0, "nenhum", 0, 0, "escopo", false, "fora_escopo", "recusado", List.of()));
        }
        if (fluxo.assincrono()) {
            FilaTrabalhos.Trabalho trabalho = fila.aceitar(id, fluxo.id());
            return guardar(base(id, fluxo, entrada, "fila", "nenhum", 0, "nenhum", 0, 0, "fila", false, trabalho.status(), id, trabalho.trilha()));
        }
        if (vetorIndisponivel && (fluxo == Fluxo.BUSCA || fluxo == Fluxo.ATENDIMENTO)) {
            return guardar(base(id, fluxo, entrada, "nao_aplicada", "nenhum", 0, "vetor_indisponivel", 0, 0, "sem_evidencia", false, "concluido", AUSENCIA, List.of()));
        }
        if (fluxo == Fluxo.AGENTE) {
            return guardar(agente(id, fluxo, entrada, texto));
        }
        if (fluxo == Fluxo.BUSCA || fluxo == Fluxo.ATENDIMENTO) {
            return guardar(conhecimento(id, fluxo, entrada, texto));
        }
        return guardar(modelo(id, fluxo, entrada, texto, falhasModelo));
    }

    public Registro feedback(String id, String texto) {
        Registro atual = registros.get(id);
        if (atual == null) {
            throw new IllegalArgumentException("Execucao ausente: " + id);
        }
        Registro novo = atual.comFeedback(texto);
        registros.put(id, novo);
        return novo;
    }

    public Registro registro(String id) {
        return registros.get(id);
    }

    public ConsultaEndereco.Resultado endereco(String codigo) {
        return endereco.consultar(codigo);
    }

    public void drenar() throws IOException {
        fila.drenar();
        for (Registro registro : List.copyOf(registros.values())) {
            FilaTrabalhos.Trabalho trabalho = fila.obter(registro.id());
            if (trabalho == null) {
                continue;
            }
            registros.put(registro.id(), registro.comTrabalho(
                    trabalho.status(), trabalho.tentativas(), trabalho.erros(), trabalho.trilha()));
        }
    }

    public Path raiz() {
        return raiz;
    }

    private Registro agente(String id, Fluxo fluxo, String entrada, String texto) {
        try {
            AgentLoop.Run run = AgentLoop.create(new RagRules(index, config), AgentLimits.load()).run(texto);
            String status = "escalado".equals(run.parada()) || "aguardando_aprovacao".equals(run.parada())
                    ? "revisao_humana"
                    : run.sucesso() ? "concluido" : run.parada();
            String ferramenta = run.acoes().isEmpty() ? "nao_aplicada" : String.join(",", run.acoes());
            return base(id, fluxo, entrada, ferramenta, "agente", run.trace().size(), run.falhas() == 0 ? "nenhum" : "ferramenta",
                    0, run.latenciaMs(), status, false, status, run.resumo(), List.of());
        } catch (RuntimeException ex) {
            return base(id, fluxo, entrada, "nao_aplicada", "agente", 1, "ferramenta", 0, 0, "falhou", false, "falhou", "ausente", List.of());
        }
    }

    private Registro conhecimento(String id, Fluxo fluxo, String entrada, String texto) {
        try {
            String resposta;
            String contexto;
            if (fluxo == Fluxo.ATENDIMENTO) {
                RagAnswer answer = new RagAssistant(raiz.resolve("sessoes")).ask(index, id, "operador", texto, 0, config);
                resposta = answer.render();
                contexto = answer.found() ? answer.docId() : "ausente";
            } else {
                List<Retriever.Hit> hits = new Retriever().search(
                        index.chunks(), texto, Retriever.SearchFilter.of("operador", null), config);
                if (hits.isEmpty()) {
                    resposta = AUSENCIA;
                    contexto = "ausente";
                } else {
                    contexto = hits.get(0).chunk().docId();
                    resposta = "citacao: " + contexto;
                }
            }
            String validacao = AUSENCIA.equals(resposta) ? "sem_evidencia" : "evidencia";
            return base(id, fluxo, entrada, "nao_aplicada", "nenhum", 0, "nenhum", 0, 0, validacao, false, "concluido", resposta, List.of());
        } catch (IOException ex) {
            return base(id, fluxo, entrada, "nao_aplicada", "nenhum", 0, "vetor_indisponivel", 0, 0, "sem_evidencia", false, "concluido", AUSENCIA, List.of());
        }
    }

    private Registro modelo(String id, Fluxo fluxo, String entrada, String texto, int falhasModelo) {
        GatewayResiliente gateway = new GatewayResiliente(
                provedor(falhasModelo), LimitesInferencia.carregar(), new Relogio.Manual());
        ExecucaoInferencia execucao = gateway.executar("classificacao", texto);
        if (!execucao.aceita()) {
            return base(id, fluxo, entrada, "classificacao", GatewayResiliente.NOME, execucao.tentativas(),
                    execucao.tipoFalha(), execucao.custo(), execucao.latenciaMs(), "falhou", false, "falhou", "ausente", List.of());
        }
        return base(id, fluxo, entrada, "classificacao", GatewayResiliente.NOME, execucao.tentativas(),
                "nenhum", execucao.custo(), execucao.latenciaMs(), "aprovada", execucao.fallback(), "concluido",
                execucao.texto(), List.of());
    }

    private static ProvedorInferencia provedor(int falhasModelo) {
        if (falhasModelo <= 0) {
            return new MockCatalogo();
        }
        return new ProvedorInstavel(falhasModelo);
    }

    private Registro guardar(Registro registro) {
        registros.put(registro.id(), registro);
        return registro;
    }

    private static Registro base(
            String id, Fluxo fluxo, String entrada, String ferramenta, String modelo, int tentativas, String erros,
            double custo, long latencia, String validacao, boolean fallback, String status, String resposta, List<String> trilha) {
        return new Registro(
                id,
                fluxo.id(),
                fluxo.destino(),
                entrada,
                fluxo.saida(),
                fluxo.risco(),
                resposta,
                ETAPAS,
                ferramenta,
                fluxo.id(),
                resposta,
                ferramenta,
                resposta,
                modelo,
                VERSAO,
                tentativas,
                erros,
                custo,
                latencia,
                validacao,
                fallback,
                status,
                "ausente",
                trilha);
    }

    static String ocultar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return SEGREDO.matcher(texto).replaceAll("[oculto]");
    }

    private static final class ProvedorInstavel implements ProvedorInferencia {
        private int restantes;
        private final MockCatalogo mock = new MockCatalogo();

        private ProvedorInstavel(int falhas) {
            this.restantes = falhas;
        }

        @Override
        public RespostaProvedor completar(String tarefa, String texto) {
            if (restantes > 0) {
                restantes--;
                return new RespostaProvedor("", 1, 0, 1, true, "indisponivel");
            }
            return mock.completar(tarefa, texto);
        }
    }
}
