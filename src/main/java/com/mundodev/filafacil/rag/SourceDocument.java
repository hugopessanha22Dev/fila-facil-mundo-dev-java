package com.mundodev.filafacil.rag;

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
        List<Section> secoes) {

    public record Section(String nome, String texto) {
    }

    public SourceDocument withVersao(String novaVersao, String textoDaSecao) {
        List<Section> atualizadas = secoes.stream()
                .map(secao -> new Section(secao.nome(), textoDaSecao))
                .toList();
        return new SourceDocument(id, titulo, tipo, dominio, novaVersao, dataAtualizacao, owner, acesso, atualizadas);
    }
}
