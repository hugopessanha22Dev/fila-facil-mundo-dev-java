package com.mundodev.filafacil.cloudai;

import com.mundodev.filafacil.cloudai.cache.MemoryResponseCache;
import com.mundodev.filafacil.cloudai.cache.RedisResponseCache;
import com.mundodev.filafacil.cloudai.cache.ResponseCache;
import com.mundodev.filafacil.inference.HttpOpenAiCompatibleGateway;
import com.mundodev.filafacil.inference.InferenceConfig;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.MockInferenceGateway;

import java.nio.file.Path;
import java.util.Map;

public final class CloudAiMain {

    private CloudAiMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, System.getenv(), Path.of("data", "cloud-ai"), Path.of("").toAbsolutePath());
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, Map<String, String> env, Path root, Path repoRoot) {
        try {
            if (args == null || args.length == 0) {
                System.out.println("Uso: enqueue --text <texto> --origin <cliente|operador|parceiro> | drain");
                return 1;
            }
            AiEnvironment environment = AiEnvironment.parse(env.get("FILAFACIL_AI_ENV"));
            SecretSource.Decision secret = SecretSource.resolve(environment, env, repoRoot);
            if (secret.refused()) {
                System.out.println("Recusado: " + secret.reason());
                return 1;
            }
            QuotaPolicy quota = new QuotaCatalog().forEnv(environment);
            InferenceGateway gateway = gateway(env, secret);
            ResponseCache cache = cache(env);
            try (TriagePipeline pipeline = new TriagePipeline(
                    root, environment, quota, gateway, cache, "v1", "v1")) {
                if ("enqueue".equals(args[0])) {
                    String text = arg(args, "--text");
                    String origin = arg(args, "--origin");
                    String id = pipeline.enqueue(text, origin);
                    System.out.println("aceita id=" + id);
                    return 0;
                }
                if ("drain".equals(args[0])) {
                    RunSummary summary = pipeline.drain(System.currentTimeMillis());
                    System.out.println(summary.render());
                    return 0;
                }
                System.out.println("Comando desconhecido: " + args[0]);
                return 1;
            }
        } catch (Exception ex) {
            System.out.println("ERRO cloud-ai: " + Redactor.mask(ex.getMessage()));
            return 1;
        }
    }

    static InferenceGateway gateway(Map<String, String> env, SecretSource.Decision secret) {
        String mode = env.getOrDefault("FILAFACIL_AI_GATEWAY", "mock");
        if (!"http".equalsIgnoreCase(mode) || secret.value() == null || secret.value().isBlank()) {
            return new MockInferenceGateway();
        }
        String baseUrl = env.getOrDefault("FILAFACIL_LLM_BASE_URL", "https://api.groq.com/openai/v1");
        if (baseUrl.contains("api.openai.com")) {
            System.out.println("Provedor pago recusado. Usando mock gratuito.");
            return new MockInferenceGateway();
        }
        InferenceConfig config = new InferenceConfig(
                "http",
                secret.value(),
                baseUrl,
                env.getOrDefault("FILAFACIL_LLM_MODEL_LIGHT", "openai/gpt-oss-20b"),
                env.getOrDefault("FILAFACIL_LLM_MODEL_DETAILED", "openai/gpt-oss-120b"),
                20,
                1,
                0);
        return new HttpOpenAiCompatibleGateway(config);
    }

    private static ResponseCache cache(Map<String, String> env) {
        if (!"redis".equalsIgnoreCase(env.getOrDefault("FILAFACIL_AI_CACHE", "memory"))) {
            return new MemoryResponseCache();
        }
        String host = env.getOrDefault("FILAFACIL_REDIS_HOST", "localhost");
        int port = Integer.parseInt(env.getOrDefault("FILAFACIL_REDIS_PORT", "6379"));
        return RedisResponseCache.tryOpen(host, port)
                .map(ResponseCache.class::cast)
                .orElseGet(MemoryResponseCache::new);
    }

    private static String arg(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return "";
    }
}
