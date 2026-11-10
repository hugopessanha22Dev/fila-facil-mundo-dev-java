package com.mundodev.filafacil.manifest;

import com.mundodev.filafacil.error.AppException;

import java.nio.file.Path;

/**
 * CLI headless para CI/CD.
 * Uso: java ... ManifestCli &lt;manifesto.json|csv&gt; [--out relatorio.txt]
 */
public class ManifestCli {

    public static void main(String[] args) {
        int code = run(args);
        System.exit(code);
    }

    static int run(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Uso: ManifestCli <arquivo.json|csv> [--out relatorio.txt]");
            return 1;
        }
        Path manifesto = null;
        Path out = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--out".equals(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("Flag --out requer caminho.");
                    return 1;
                }
                out = Path.of(args[++i]);
            } else if (!arg.startsWith("-")) {
                manifesto = Path.of(arg);
            } else {
                System.err.println("Argumento desconhecido: " + arg);
                return 1;
            }
        }
        if (manifesto == null) {
            System.err.println("Arquivo de manifesto obrigatorio.");
            return 1;
        }

        try {
            ManifestValidationService service = new ManifestValidationService();
            ValidationReport report = service.validar(manifesto);
            System.out.print(report.render());
            if (out != null) {
                service.salvarRelatorio(report, out);
                System.out.println("Relatorio salvo em: " + out.toAbsolutePath());
            }
            return report.aprovado() ? 0 : 1;
        } catch (AppException ex) {
            System.err.println("ERRO [" + ex.getOrigem() + "]: " + ex.getMessage());
            return 1;
        } catch (Exception ex) {
            System.err.println("ERRO inesperado: " + ex.getMessage());
            return 1;
        }
    }
}
