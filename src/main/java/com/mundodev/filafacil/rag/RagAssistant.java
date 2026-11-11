package com.mundodev.filafacil.rag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RagAssistant {

    private final Retriever retriever = new Retriever();
    private final SessionMemory memory;

    public RagAssistant(java.nio.file.Path root) {
        this.memory = new SessionMemory(root);
    }

    public RagAnswer ask(
            ChunkIndex index,
            String sessionId,
            String role,
            String text,
            long nowMs,
            RetrievalConfig config) throws IOException {
        SessionMemory.State session = memory.load(sessionId, nowMs, config.sessionTtlMinutes());
        String detected = DomainIntent.domainOf(text);
        String domain = detected != null ? detected : session == null ? null : session.lastDomain();
        List<Retriever.Hit> hits = retriever.search(
                index.chunks(), text, Retriever.SearchFilter.of(role, domain), config);
        List<String> facts = new ArrayList<>();
        if (session != null) {
            facts.addAll(session.facts());
        }
        facts.add(text);
        memory.save(new SessionMemory.State(sessionId, text, domain, List.copyOf(facts), nowMs));
        if (hits.isEmpty()) {
            return RagAnswer.missing(domain);
        }
        Retriever.Hit top = hits.get(0);
        return RagAnswer.cited(top.chunk(), domain, top.score());
    }
}
