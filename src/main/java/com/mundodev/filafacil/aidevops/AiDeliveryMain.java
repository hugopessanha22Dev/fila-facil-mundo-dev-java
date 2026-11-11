package com.mundodev.filafacil.aidevops;

import com.mundodev.filafacil.cloudai.Redactor;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.MockInferenceGateway;

import java.nio.file.Path;
import java.util.Map;

public final class AiDeliveryMain {

    private AiDeliveryMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, System.getenv(), Path.of("data", "ai-delivery"));
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, Map<String, String> env, Path root) {
        return execute(args, env, root, new MockInferenceGateway());
    }

    public static int execute(String[] args, Map<String, String> env, Path root, InferenceGateway gateway) {
        try {
            if (args == null || args.length == 0) {
                System.out.println("Uso: evaluate | promote homolog|prod | rollback homolog|prod | status");
                return 1;
            }
            DeliveryCatalog catalog = new DeliveryCatalog();
            ReleaseStore store = new ReleaseStore();
            return switch (args[0]) {
                case "evaluate" -> evaluate(env, root, gateway, catalog, store);
                case "promote" -> store.promote(root, arg(args), catalog.combo());
                case "rollback" -> store.rollback(root, arg(args));
                case "status" -> {
                    System.out.print(store.status(root));
                    yield 0;
                }
                default -> {
                    System.out.println("Comando desconhecido: " + args[0]);
                    yield 1;
                }
            };
        } catch (Exception ex) {
            System.out.println("ERRO ai-delivery: " + Redactor.mask(ex.getMessage()));
            return 1;
        }
    }

    private static int evaluate(
            Map<String, String> env,
            Path root,
            InferenceGateway gateway,
            DeliveryCatalog catalog,
            ReleaseStore store) throws Exception {
        String mode = env.getOrDefault("FILAFACIL_AI_GATEWAY", "mock");
        String baseUrl = env.getOrDefault("FILAFACIL_LLM_BASE_URL", "");
        if ("http".equalsIgnoreCase(mode) || baseUrl.contains("api.openai.com")) {
            System.out.println("Modo HTTP ignorado. Esteira usa mock gratuito.");
        }
        String environment = env.getOrDefault("FILAFACIL_AI_ENV", "dev");
        QualityReport report = new OfflineEvaluation().run(root, environment, gateway, catalog, store);
        System.out.println(report.render());
        return report.approved() ? 0 : 1;
    }

    private static String arg(String[] args) {
        return args.length > 1 ? args[1] : "";
    }
}
