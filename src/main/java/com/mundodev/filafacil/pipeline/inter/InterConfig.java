package com.mundodev.filafacil.pipeline.inter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

public record InterConfig(
        Path pipelineRoot,
        InterMode mode,
        LatePolicy latePolicy,
        int parallelism,
        Optional<SourceId> onlySource,
        Optional<LocalDateTime> reprocessFrom,
        Optional<LocalDateTime> reprocessTo,
        boolean reprocess) {

    public static InterConfig defaults(Path root) {
        return new InterConfig(
                root,
                InterMode.INCREMENTAL,
                LatePolicy.parse(env("FILAFACIL_PIPELINE_LATE_POLICY", "mark")),
                Integer.parseInt(env("FILAFACIL_PIPELINE_PARALLELISM", "4")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false);
    }

    public static InterConfig fromEnvAndArgs(Path root, String[] args) {
        InterMode mode = InterMode.INCREMENTAL;
        LatePolicy late = LatePolicy.parse(env("FILAFACIL_PIPELINE_LATE_POLICY", "mark"));
        int parallelism = Integer.parseInt(env("FILAFACIL_PIPELINE_PARALLELISM", "4"));
        Optional<SourceId> only = Optional.empty();
        Optional<LocalDateTime> from = Optional.empty();
        Optional<LocalDateTime> to = Optional.empty();
        boolean reprocess = false;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--mode".equals(a) && i + 1 < args.length) {
                mode = InterMode.parse(args[++i]);
            } else if ("--reprocess".equals(a)) {
                reprocess = true;
            } else if ("--source".equals(a) && i + 1 < args.length) {
                only = Optional.of(parseSource(args[++i]));
            } else if ("--from".equals(a) && i + 1 < args.length) {
                from = Optional.of(TimeParse.parse(args[++i]));
                reprocess = true;
            } else if ("--to".equals(a) && i + 1 < args.length) {
                to = Optional.of(TimeParse.parse(args[++i]));
                reprocess = true;
            }
        }
        return new InterConfig(root, mode, late, parallelism, only, from, to, reprocess);
    }

    private static SourceId parseSource(String raw) {
        String s = raw.trim().toLowerCase();
        return switch (s) {
            case "api_coletas", "coletas", "api" -> SourceId.API_COLETAS;
            case "clientes", "clientes_relacional" -> SourceId.CLIENTES;
            case "entregas" -> SourceId.ENTREGAS;
            case "motoristas", "rotas" -> SourceId.MOTORISTAS;
            default -> throw new IllegalArgumentException("fonte desconhecida: " + raw);
        };
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}
