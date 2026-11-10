package com.mundodev.filafacil.pipeline.stage;

import com.mundodev.filafacil.pipeline.PipelineContext;
import com.mundodev.filafacil.pipeline.PipelineStage;
import com.mundodev.filafacil.pipeline.io.LayerWriter;
import com.mundodev.filafacil.pipeline.model.ClienteUnidade;
import com.mundodev.filafacil.pipeline.model.ColetaRaw;
import com.mundodev.filafacil.pipeline.model.EntregaRaw;
import com.mundodev.filafacil.pipeline.model.MetricaDiaria;
import com.mundodev.filafacil.pipeline.model.OcorrenciaRaw;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class TransformStage implements PipelineStage {

    @Override
    public String name() {
        return "transform";
    }

    @Override
    public void run(PipelineContext context) throws Exception {
        long descartados = 0;

        List<ColetaRaw> coletasOk = new ArrayList<>();
        for (ColetaRaw c : context.coletasBrutas()) {
            ColetaRaw n = normalizeColeta(c);
            if (isBlank(n.codigoColeta()) || isBlank(n.cliente()) || isBlank(n.dataOperacao()) || isBlank(n.status())) {
                descartados++;
                continue;
            }
            coletasOk.add(n);
        }

        List<OcorrenciaRaw> ocorrenciasOk = new ArrayList<>();
        for (OcorrenciaRaw o : context.ocorrenciasBrutas()) {
            OcorrenciaRaw n = normalizeOcorrencia(o);
            if (isBlank(n.tipo()) || isBlank(n.referencia()) || isBlank(n.data()) || isBlank(n.descricao())) {
                descartados++;
                continue;
            }
            ocorrenciasOk.add(n);
        }

        List<EntregaRaw> entregasOk = new ArrayList<>();
        for (EntregaRaw e : context.entregasBrutas()) {
            EntregaRaw n = normalizeEntrega(e, context.cadastro());
            if (isBlank(n.idEntrega()) || isBlank(n.cliente()) || isBlank(n.dataConclusao()) || isBlank(n.statusFinal())) {
                descartados++;
                continue;
            }
            entregasOk.add(n);
        }

        context.coletasProcessadas().clear();
        context.coletasProcessadas().addAll(coletasOk);
        context.ocorrenciasProcessadas().clear();
        context.ocorrenciasProcessadas().addAll(ocorrenciasOk);
        context.entregasProcessadas().clear();
        context.entregasProcessadas().addAll(entregasOk);

        List<MetricaDiaria> metricas = aggregate(coletasOk, entregasOk, ocorrenciasOk);
        context.setMetricas(metricas);

        context.log().counters().addDescartados(descartados);
        context.log().info("transform ok coletas=" + coletasOk.size()
                + " entregas=" + entregasOk.size()
                + " ocorrencias=" + ocorrenciasOk.size()
                + " descartados=" + descartados
                + " metricas=" + metricas.size());

        Path processada = context.paths().processada(context.dataRef(), context.runId());
        LayerWriter.writeJson(processada, "coletas.json", coletasOk);
        LayerWriter.writeJson(processada, "entregas.json", entregasOk);
        LayerWriter.writeJson(processada, "ocorrencias.json", ocorrenciasOk);
        LayerWriter.writeJson(processada, "metricas.json", metricas);
        context.log().counters().addGravados(4);
        context.log().info("camada processada=" + processada.toAbsolutePath());
    }

    public static ColetaRaw normalizeColeta(ColetaRaw c) {
        return new ColetaRaw(
                trim(c.codigoColeta()).toUpperCase(Locale.ROOT),
                trim(c.cliente()).toUpperCase(Locale.ROOT),
                normalizeRegiao(c.regiao()),
                trim(c.responsavel()),
                normalizeStatus(c.status()),
                trim(c.prazoPrevisto()),
                trim(c.prazoRealizado()),
                trim(c.dataOperacao()));
    }

    public static OcorrenciaRaw normalizeOcorrencia(OcorrenciaRaw o) {
        return new OcorrenciaRaw(
                trim(o.tipo()).toLowerCase(Locale.ROOT),
                trim(o.descricao()),
                trim(o.referencia()).toUpperCase(Locale.ROOT),
                trim(o.data()),
                trim(o.responsavel()),
                normalizeStatus(o.statusResolucao()));
    }

    public static EntregaRaw normalizeEntrega(EntregaRaw e, Map<String, ClienteUnidade> cadastro) {
        String cliente = trim(e.cliente()).toUpperCase(Locale.ROOT);
        ClienteUnidade cu = cadastro.get(cliente);
        String regiao = cu != null ? normalizeRegiao(cu.regiao()) : normalizeRegiao(e.regiao());
        String unidade = cu != null ? cu.unidade() : trim(e.unidade()).toUpperCase(Locale.ROOT);
        return new EntregaRaw(
                trim(e.idEntrega()).toUpperCase(Locale.ROOT),
                regiao,
                trim(e.dataConclusao()),
                trim(e.prazoOriginal()),
                normalizeStatus(e.statusFinal()),
                e.atraso(),
                cliente,
                unidade);
    }

    public static List<MetricaDiaria> aggregate(
            List<ColetaRaw> coletas,
            List<EntregaRaw> entregas,
            List<OcorrenciaRaw> ocorrencias) {
        Map<String, long[]> buckets = new LinkedHashMap<>();

        for (ColetaRaw c : coletas) {
            buckets.computeIfAbsent(key(c.dataOperacao(), c.regiao(), c.status(), c.cliente()), k -> new long[4])[0]++;
        }

        Map<String, String> refRegiao = new LinkedHashMap<>();
        Map<String, String> refCliente = new LinkedHashMap<>();
        for (EntregaRaw e : entregas) {
            refRegiao.put(e.idEntrega(), e.regiao());
            refCliente.put(e.idEntrega(), e.cliente());
            long[] b = buckets.computeIfAbsent(
                    key(e.dataConclusao(), e.regiao(), e.statusFinal(), e.cliente()), k -> new long[4]);
            b[1]++;
            if (e.atraso()) {
                b[2]++;
            }
        }
        for (ColetaRaw c : coletas) {
            refRegiao.put(c.codigoColeta(), c.regiao());
            refCliente.put(c.codigoColeta(), c.cliente());
        }
        for (OcorrenciaRaw o : ocorrencias) {
            String regiao = refRegiao.getOrDefault(o.referencia(), "DESCONHECIDA");
            String cliente = refCliente.getOrDefault(o.referencia(), "N/A");
            buckets.computeIfAbsent(
                    key(o.data(), regiao, o.statusResolucao(), cliente), k -> new long[4])[3]++;
        }

        List<MetricaDiaria> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : buckets.entrySet()) {
            String[] p = e.getKey().split("\\|", -1);
            long[] v = e.getValue();
            out.add(new MetricaDiaria(p[0], p[1], p[2], p[3], v[0], v[1], v[2], v[3]));
        }
        return out;
    }

    private static String key(String data, String regiao, String status, String cliente) {
        return String.join("|",
                Objects.toString(data, ""),
                Objects.toString(regiao, ""),
                Objects.toString(status, ""),
                Objects.toString(cliente, ""));
    }

    static String normalizeStatus(String status) {
        String s = trim(status).toLowerCase(Locale.ROOT).replace(' ', '_');
        return switch (s) {
            case "concluido", "finalizada", "finalizado" -> "concluida";
            case "em rota", "em-rota" -> "em_rota";
            default -> s;
        };
    }

    static String normalizeRegiao(String regiao) {
        String r = trim(regiao).toUpperCase(Locale.ROOT)
                .replace(' ', '-')
                .replace('_', '-');
        return switch (r) {
            case "SE", "SUDESTE" -> "SUDESTE";
            case "S", "SUL" -> "SUL";
            case "CO", "CENTRO-OESTE", "CENTROOESTE" -> "CENTRO-OESTE";
            case "NE", "NORDESTE" -> "NORDESTE";
            case "N", "NORTE" -> "NORTE";
            default -> r.isEmpty() ? "DESCONHECIDA" : r;
        };
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
