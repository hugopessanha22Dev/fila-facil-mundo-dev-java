package com.mundodev.filafacil.rag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class RagMain {

    private RagMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, Path.of("data", "rag"));
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, Path root) {
        try {
            if (args == null || args.length == 0) {
                System.out.println("Uso: ingest | ask --session <id> --role <operador|parceiro> --text <texto> | eval");
                return 1;
            }
            RetrievalConfig config = RetrievalConfig.load();
            return switch (args[0]) {
                case "ingest" -> ingest(root, config);
                case "ask" -> ask(args, root, config);
                case "eval" -> eval(root, config);
                default -> {
                    System.out.println("Comando desconhecido: " + args[0]);
                    yield 1;
                }
            };
        } catch (Exception ex) {
            System.out.println("ERRO rag: " + ex.getMessage());
            return 1;
        }
    }

    private static int ingest(Path root, RetrievalConfig config) throws Exception {
        ChunkIndex index = new ChunkIndex(root);
        for (SourceDocument doc : new CorpusCatalog().load()) {
            index.publish(doc, config);
        }
        System.out.println("ingeridos=" + index.chunks().size());
        return 0;
    }

    private static int ask(String[] args, Path root, RetrievalConfig config) throws Exception {
        ChunkIndex index = new ChunkIndex(root);
        index.load();
        String session = value(args, "--session", "operador-local");
        String role = value(args, "--role", "operador");
        String text = text(args);
        if (text.isBlank()) {
            System.out.println("Texto ausente");
            return 1;
        }
        RagAnswer answer = new RagAssistant(root).ask(index, session, role, text, System.currentTimeMillis(), config);
        String rendered = answer.render();
        Files.createDirectories(root);
        Files.writeString(root.resolve("last-answer.txt"), rendered);
        System.out.println(rendered);
        return 0;
    }

    private static int eval(Path root, RetrievalConfig config) throws Exception {
        ChunkIndex index = new ChunkIndex(root);
        index.load();
        if (index.chunks().isEmpty()) {
            ingest(root, config);
            index.load();
        }
        RetrievalEval.Report report = new RetrievalEval().evaluate(index, root, config);
        Files.createDirectories(root);
        new RetrievalEval().write(root.resolve("eval-report.json"), report);
        System.out.println(report.render());
        return report.cobertura() == 1.0 && report.groundedness() == 1.0 && report.semEvidencia() == 1.0 ? 0 : 1;
    }

    private static String value(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return fallback;
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
