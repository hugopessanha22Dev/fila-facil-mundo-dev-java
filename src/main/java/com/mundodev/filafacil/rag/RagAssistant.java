package com.mundodev.filafacil.rag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RagAssistant {

    private static final Pattern PERFIL = Pattern.compile("(?i)\\bperfil\\s+([a-z0-9]+)");

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
        List<String> dominios = DomainIntent.dominiosDe(text);
        String filtro = dominios.size() > 1 ? null : domain;
        List<Retriever.Hit> hits = ContextoCompressor.comprimir(
                retriever.search(index.chunks(), text, Retriever.SearchFilter.of(role, filtro), config),
                text);
        List<String> facts = new ArrayList<>();
        if (session != null) {
            facts.addAll(session.facts());
        }
        facts.add(text);
        String formato = text.toLowerCase(Locale.ROOT).contains("formato curto")
                ? "curto"
                : session == null || session.formato() == null ? "" : session.formato();
        String perfil = perfilDe(text);
        if (perfil.isBlank() && session != null && session.perfil() != null) {
            perfil = session.perfil();
        }
        memory.save(new SessionMemory.State(
                sessionId, text, domain, List.copyOf(facts), nowMs, "operacao", perfil, formato));
        if (hits.isEmpty()) {
            return RagAnswer.missing(domain);
        }
        Retriever.Hit top = hits.get(0);
        return RagAnswer.cited(top.chunk(), domain, top.score());
    }

    private static String perfilDe(String text) {
        Matcher matcher = PERFIL.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : "";
    }
}
