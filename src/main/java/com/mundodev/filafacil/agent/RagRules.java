package com.mundodev.filafacil.agent;

import com.mundodev.filafacil.rag.ChunkIndex;
import com.mundodev.filafacil.rag.DomainIntent;
import com.mundodev.filafacil.rag.KnowledgeChunk;
import com.mundodev.filafacil.rag.RetrievalConfig;
import com.mundodev.filafacil.rag.Retriever;

import java.util.List;

public final class RagRules {

    public record Hit(boolean encontrada, String trecho, String citacao) {
    }

    private final Retriever retriever = new Retriever();
    private final ChunkIndex index;
    private final RetrievalConfig config;

    public RagRules(ChunkIndex index, RetrievalConfig config) {
        this.index = index;
        this.config = config;
    }

    public Hit lookup(String pergunta) {
        String domain = DomainIntent.domainOf(pergunta);
        List<Retriever.Hit> hits = retriever.search(
                index.chunks(), pergunta, Retriever.SearchFilter.of("operador", domain), config);
        if (hits.isEmpty()) {
            return new Hit(false, "", "");
        }
        KnowledgeChunk chunk = hits.get(0).chunk();
        String citacao = chunk.docId() + " | " + chunk.versao() + " | " + chunk.secao();
        return new Hit(true, chunk.texto(), citacao);
    }
}
