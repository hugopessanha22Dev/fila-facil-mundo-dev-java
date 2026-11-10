package com.mundodev.filafacil.pipeline;

import com.mundodev.filafacil.pipeline.inter.InterConfig;
import com.mundodev.filafacil.pipeline.inter.InterRunReport;
import com.mundodev.filafacil.pipeline.inter.PipelineInterModule;

public final class PipelineCli {

    private PipelineCli() {
    }

    public static void main(String[] args) throws Exception {
        PipelineConfig config = PipelineConfig.fromEnv();
        boolean inter = hasFlag(args, "--inter") || hasFlag(args, "--mode");
        System.out.println("FilaFacil Pipeline");
        System.out.println("root=" + config.pipelineRoot().toAbsolutePath());

        if (inter) {
            InterConfig interConfig = InterConfig.fromEnvAndArgs(config.pipelineRoot(), args);
            System.out.println("mode=" + interConfig.mode() + " latePolicy=" + interConfig.latePolicy());
            PipelineInterModule.InterRunResult result = PipelineInterModule.run(interConfig);
            InterRunReport report = result.report();
            System.out.println(report.resumo());
            report.tempoMsPorFonte().forEach((k, v) -> System.out.println("fonte." + k + ".ms=" + v));
            if (!"SUCCESS".equals(report.status())) {
                System.exit(1);
            }
            return;
        }

        System.out.println("FilaFacil Pipeline batch (basico)");
        PipelineModule.PipelineRunResult result = PipelineModule.run(config);
        PipelineRunLog log = result.log();
        System.out.println(log.resumo());
        log.tempoMsPorEtapa().forEach((k, v) -> System.out.println("etapa." + k + ".ms=" + v));
        if (!"SUCCESS".equals(log.status())) {
            System.exit(1);
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) {
            if (flag.equals(a) || a.startsWith(flag + "=")) {
                return true;
            }
        }
        return false;
    }
}
