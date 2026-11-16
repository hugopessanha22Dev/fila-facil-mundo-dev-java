package com.mundodev.filafacil.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public record SourceDocument(
        String id,
        String titulo,
        String tipo,
        String dominio,
        String versao,
        String dataAtualizacao,
        String owner,
        String acesso,
        List<Section> secoes,
        String status,
        String conteudoHash,
        String versaoAnterior,
        String versaoPosterior,
        String tenant,
        String namespace) {

    public record Section(String nome, String texto) {
    }

    public static SourceDocument vigente(
            String id,
            String titulo,
            String tipo,
            String dominio,
            String versao,
            String dataAtualizacao,
            String owner,
            String acesso,
            List<Section> secoes) {
        return new SourceDocument(
                id,
                titulo,
                tipo,
                dominio,
                versao,
                dataAtualizacao,
                owner,
                acesso,
                secoes,
                "vigente",
                hashDe(secoes),
                "",
                "",
                "operacao",
                "ativo");
    }

    public SourceDocument withVersao(String novaVersao, String textoDaSecao) {
        List<Section> atualizadas = secoes.stream()
                .map(secao -> new Section(secao.nome(), textoDaSecao))
                .toList();
        return new SourceDocument(
                id,
                titulo,
                tipo,
                dominio,
                novaVersao,
                dataAtualizacao,
                owner,
                acesso,
                atualizadas,
                "vigente",
                hashDe(atualizadas),
                versao,
                versaoPosterior == null ? "" : versaoPosterior,
                tenant,
                "ativo");
    }

    public SourceDocument withStatus(String novoStatus) {
        return new SourceDocument(
                id, titulo, tipo, dominio, versao, dataAtualizacao, owner, acesso, secoes,
                novoStatus, conteudoHash, versaoAnterior, versaoPosterior, tenant, namespace);
    }

    public SourceDocument withTenant(String novoTenant) {
        return new SourceDocument(
                id, titulo, tipo, dominio, versao, dataAtualizacao, owner, acesso, secoes,
                status, conteudoHash, versaoAnterior, versaoPosterior, novoTenant, namespace);
    }

    public static String hashDe(List<Section> secoes) {
        StringBuilder raw = new StringBuilder();
        for (Section secao : secoes) {
            raw.append(secao.nome()).append('\n').append(secao.texto()).append('\n');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 ausente", ex);
        }
    }
}
