package com.mundodev.filafacil.orquestracao;

import java.nio.file.Path;

public final class OrquestracaoMain {

    private OrquestracaoMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, Path.of("data", "orquestracao"));
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, Path raiz) {
        try {
            if (args == null || args.length == 0) {
                System.out.println("Uso: run --text <texto> | eval | endereco --codigo <codigo>");
                return 1;
            }
            return switch (args[0]) {
                case "run" -> run(args, raiz);
                case "eval" -> eval(raiz);
                case "endereco" -> endereco(args, raiz);
                default -> {
                    System.out.println("Comando desconhecido: " + args[0]);
                    yield 1;
                }
            };
        } catch (Exception ex) {
            System.out.println("ERRO orquestracao: " + ex.getMessage());
            return 1;
        }
    }

    private static int run(String[] args, Path raiz) throws Exception {
        String texto = texto(args, "--text");
        if (texto.isBlank()) {
            System.out.println("Texto ausente");
            return 1;
        }
        Registro registro = Orquestrador.criar(raiz).executar(texto);
        System.out.println(registro.fluxo() + " " + registro.status());
        System.out.println(registro.resposta());
        return 0;
    }

    private static int eval(Path raiz) throws Exception {
        Avaliacao.Relatorio relatorio = Avaliacao.avaliar(raiz.resolve("eval"));
        Avaliacao.escrever(raiz.resolve("eval-report.json"), relatorio);
        System.out.println("inventadas=" + relatorio.inventadas());
        System.out.println("aprovado=" + relatorio.aprovado());
        return relatorio.aprovado() ? 0 : 1;
    }

    private static int endereco(String[] args, Path raiz) throws Exception {
        String codigo = texto(args, "--codigo");
        ConsultaEndereco.Resultado resultado = Orquestrador.criar(raiz).endereco(codigo);
        System.out.println(resultado.ok() ? resultado.pagina() : resultado.erro());
        return resultado.ok() ? 0 : 1;
    }

    private static String texto(String[] args, String flag) {
        StringBuilder texto = new StringBuilder();
        boolean copiar = false;
        for (String arg : args) {
            if (flag.equals(arg)) {
                copiar = true;
                continue;
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
