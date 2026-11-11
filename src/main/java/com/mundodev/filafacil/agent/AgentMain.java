package com.mundodev.filafacil.agent;

import com.mundodev.filafacil.rag.ChunkIndex;
import com.mundodev.filafacil.rag.CorpusCatalog;
import com.mundodev.filafacil.rag.RetrievalConfig;
import com.mundodev.filafacil.rag.SourceDocument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class AgentMain {

    private AgentMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, Path.of("data", "rag"), Path.of("data", "agent"));
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, Path ragRoot, Path reportRoot) {
        try {
            if (args == null || args.length == 0) {
                System.out.println("Uso: run --text <texto> | eval");
                return 1;
            }
            RetrievalConfig config = RetrievalConfig.load();
            ChunkIndex index = openIndex(ragRoot, config);
            RagRules rules = new RagRules(index, config);
            AgentLimits limits = AgentLimits.load();
            return switch (args[0]) {
                case "run" -> run(args, rules, limits, reportRoot);
                case "eval" -> eval(rules, limits, reportRoot);
                default -> {
                    System.out.println("Comando desconhecido: " + args[0]);
                    yield 1;
                }
            };
        } catch (Exception ex) {
            System.out.println("ERRO agente: " + ex.getMessage());
            return 1;
        }
    }

    private static int run(String[] args, RagRules rules, AgentLimits limits, Path reportRoot) throws Exception {
        String text = text(args);
        if (text.isBlank()) {
            System.out.println("Texto ausente");
            return 1;
        }
        AgentLoop.Run run = AgentLoop.create(rules, limits).run(text);
        Files.createDirectories(reportRoot);
        Files.writeString(reportRoot.resolve("last-run.txt"), run.render());
        System.out.println(run.render());
        return 0;
    }

    private static int eval(RagRules rules, AgentLimits limits, Path reportRoot) throws Exception {
        AgentEval eval = new AgentEval();
        AgentEval.Report report = eval.evaluate(rules, limits);
        eval.write(reportRoot.resolve("eval-report.json"), report);
        System.out.println(report.render());
        return report.aprovado() ? 0 : 1;
    }

    static ChunkIndex openIndex(Path root, RetrievalConfig config) throws Exception {
        ChunkIndex index = new ChunkIndex(root);
        index.load();
        if (index.chunks().isEmpty()) {
            for (SourceDocument doc : new CorpusCatalog().load()) {
                index.publish(doc, config);
            }
        }
        return index;
    }

    private static String text(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--text".equals(args[i])) {
                int end = args.length;
                for (int j = i + 1; j < args.length; j++) {
                    if (args[j].startsWith("--")) {
                        end = j;
                        break;
                    }
                }
                return String.join(" ", Arrays.copyOfRange(args, i + 1, end));
            }
        }
        return "";
    }
}
