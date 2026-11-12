package com.mundodev.filafacil.excecoes.adapters;

import com.mundodev.filafacil.excecoes.domain.ExcecaoEntrega;
import com.mundodev.filafacil.excecoes.domain.OrigemExcecao;
import com.mundodev.filafacil.excecoes.domain.Severidade;
import com.mundodev.filafacil.excecoes.domain.TipoExcecao;
import com.mundodev.filafacil.excecoes.domain.TransicaoRecusada;

import java.io.PrintStream;
import java.time.Instant;

public final class ExcecoesMain {

    private ExcecoesMain() {
    }

    public static void main(String[] args) {
        int code = execute(args, new ExcecoesSessao(), System.out);
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int execute(String[] args, ExcecoesSessao sessao, PrintStream out) {
        try {
            if (args == null || args.length == 0) {
                out.println("Uso: abrir | analisar | escalar | resolver | cancelar | consultar");
                return 1;
            }
            ExcecaoEntrega excecao = switch (args[0]) {
                case "abrir" -> abrir(args, sessao);
                case "analisar" -> sessao.ciclo().analisar(obrigatorio(args, "--id"));
                case "escalar" -> sessao.ciclo().escalar(obrigatorio(args, "--id"));
                case "resolver" -> sessao.ciclo().resolver(obrigatorio(args, "--id"), instante(args, "--agora"));
                case "cancelar" -> sessao.ciclo().cancelar(obrigatorio(args, "--id"));
                case "consultar" -> sessao.ciclo().consultar(obrigatorio(args, "--id"));
                default -> throw new TransicaoRecusada("comando desconhecido");
            };
            imprimir(out, excecao);
            return 0;
        } catch (TransicaoRecusada ex) {
            out.println("recusa: " + ex.getMessage());
            return 2;
        } catch (RuntimeException ex) {
            out.println("erro: " + ex.getMessage());
            return 1;
        }
    }

    private static ExcecaoEntrega abrir(String[] args, ExcecoesSessao sessao) {
        return sessao.ciclo().abrir(
                sessao.proximoId(),
                obrigatorio(args, "--entrega"),
                TipoExcecao.valueOf(obrigatorio(args, "--tipo")),
                OrigemExcecao.valueOf(obrigatorio(args, "--origem")),
                Severidade.valueOf(obrigatorio(args, "--severidade")),
                obrigatorio(args, "--descricao"),
                instanteOpcional(args, "--abertura"),
                instante(args, "--janela"));
    }

    private static void imprimir(PrintStream out, ExcecaoEntrega excecao) {
        out.println("id: " + excecao.id());
        out.println("status: " + excecao.status());
        out.println("entrega: " + excecao.codigoEntrega());
        out.println("tipo: " + excecao.tipo());
    }

    private static String obrigatorio(String[] args, String nome) {
        String valor = valor(args, nome);
        if (valor == null || valor.isBlank()) {
            throw new TransicaoRecusada("argumento ausente: " + nome);
        }
        return valor;
    }

    private static Instant instante(String[] args, String nome) {
        return Instant.parse(obrigatorio(args, nome));
    }

    private static Instant instanteOpcional(String[] args, String nome) {
        String valor = valor(args, nome);
        return valor == null || valor.isBlank() ? Instant.now() : Instant.parse(valor);
    }

    private static String valor(String[] args, String nome) {
        for (int i = 0; i < args.length - 1; i++) {
            if (nome.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }
}
