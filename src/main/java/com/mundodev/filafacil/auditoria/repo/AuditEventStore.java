package com.mundodev.filafacil.auditoria.repo;

import com.mundodev.filafacil.auditoria.model.AuditEvent;

import java.util.List;

public interface AuditEventStore {

    void append(AuditEvent event);

    List<AuditEvent> listRecent(int limit);
}
