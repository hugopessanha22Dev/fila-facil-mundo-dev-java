package com.mundodev.filafacil.auditoria.repo;

import com.mundodev.filafacil.auditoria.model.AuditEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryAuditEventStore implements AuditEventStore {

    private final List<AuditEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void append(AuditEvent event) {
        events.add(event);
    }

    @Override
    public List<AuditEvent> listRecent(int limit) {
        synchronized (events) {
            int from = Math.max(0, events.size() - limit);
            return List.copyOf(events.subList(from, events.size()));
        }
    }
}
