package com.mundodev.filafacil.inference.catalog;

import java.io.PrintStream;
import java.nio.file.Path;

public final class CatalogMain {

    private CatalogMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, Path.of("data/inference/ativa.json"), System.out);
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, Path arquivo, PrintStream out) {
        try {
            if (args == null || args.length == 0) {
                out.println("Uso: status | promover <versao> --acuracia <n> --formato <n> | rollback | executar --tarefa <id> --texto <texto>");
                return 1;
            }
            PonteiroVersao ponteiro = PonteiroVersao.carregar(arquivo);
            switch (args[0]) {
                case "status" -> out.println("ativa=" + ponteiro.ativa());
                case "promover" -> {
                    if (args.length < 2) {
                        out.println("erro: versao ausente");
                        return 1;
                    }
                    RelatorioPromocao relatorio = PortaoRegressao.carregar().avaliar(
                            ponteiro,
                            args[1],
                            numero(args, "--acuracia"),
                            numero(args, "--formato"));
                    out.println(relatorio.mensagem() + " ativa=" + relatorio.versaoAtiva());
                    if (!relatorio.aceita()) {
                        return 2;
                    }
                    ponteiro.gravar(arquivo);
                }
                case "rollback" -> {
                    ponteiro.rollback();
                    ponteiro.gravar(arquivo);
                    out.println("ativa=" + ponteiro.ativa() + " anterior=" + ponteiro.anterior());
                }
                case "executar" -> {
                    ExecucaoInferencia execucao = GatewayResiliente.mockPadrao()
                            .executar(obrigatorio(args, "--tarefa"), obrigatorio(args, "--texto"));
                    out.println(execucao.gateway() + " " + execucao.tarefa() + " aceita=" + execucao.aceita());
                }
                default -> {
                    out.println("erro: comando desconhecido");
                    return 1;
                }
            }
            return 0;
        } catch (RuntimeException ex) {
            out.println("erro: " + ex.getMessage());
            return 1;
        }
    }

    private static String obrigatorio(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        throw new IllegalArgumentException("faltou " + flag);
    }

    private static double numero(String[] args, String flag) {
        return Double.parseDouble(obrigatorio(args, flag));
    }
}
