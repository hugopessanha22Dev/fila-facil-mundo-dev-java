package com.mundodev.filafacil.aiops;

import java.nio.file.Path;
import java.util.Map;

public final class AiOpsMain {

    private AiOpsMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, Path.of("data", "aiops"));
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, Path raiz) {
        try {
            if (args == null || args.length == 0) {
                System.out.println("Uso: catalogo | gate <cenario> [--decisao <texto>] | checar --fluxo <id> --text <texto>");
                return 1;
            }
            return switch (args[0]) {
                case "catalogo" -> catalogo();
                case "gate" -> gate(args);
                case "checar" -> checar(args);
                case "incidente" -> incidente(args, raiz);
                default -> {
                    System.out.println("Comando desconhecido: " + args[0]);
                    yield 1;
                }
            };
        } catch (Exception ex) {
            System.out.println("ERRO aiops: " + ex.getMessage());
            return 1;
        }
    }

    private static int catalogo() {
        for (CatalogoFluxos.Fluxo fluxo : CatalogoFluxos.carregar().fluxos()) {
            System.out.println(fluxo.id() + " dono=" + fluxo.dono());
        }
        return 0;
    }

    private static int gate(String[] args) {
        if (args.length < 2) {
            System.out.println("Cenario ausente");
            return 1;
        }
        GateRelease.Decisao decisao = GateRelease.avaliar(args[1], valor(args, "--decisao"));
        System.out.println("status=" + decisao.status());
        System.out.println("promovido=" + decisao.promovido());
        System.out.println("alerta=" + decisao.alerta());
        System.out.println("versao=" + decisao.versao());
        System.out.println("falhos=" + String.join(",", decisao.falhos()));
        return decisao.promovido() ? 0 : 1;
    }

    private static int checar(String[] args) {
        ControleSeguranca.Resultado resultado = ControleSeguranca.avaliar(
                valor(args, "--fluxo"), texto(args), valor(args, "--ferramenta"), Map.of());
        System.out.println(resultado.motivo());
        System.out.println(resultado.texto());
        return resultado.aceito() ? 0 : 1;
    }

    private static int incidente(String[] args, Path raiz) {
        MesaIncidentes mesa = new MesaIncidentes(raiz);
        mesa.abrir(valor(args, "--fluxo"), "alta", "operacao", "rollback", true, "modelo", "revisar o prompt");
        System.out.println(mesa.executar(valor(args, "--fluxo")));
        return 0;
    }

    private static String valor(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return "";
    }

    private static String texto(String[] args) {
        StringBuilder texto = new StringBuilder();
        boolean copiar = false;
        for (String arg : args) {
            if ("--text".equals(arg)) {
                copiar = true;
                continue;
            }
            if (copiar && arg.startsWith("--")) {
                break;
            }
            if (copiar) {
                if (texto.length() > 0) {
                    texto.append(' ');
                }
                texto.append(arg);
            }
        }
        return texto.toString();
    }
}
