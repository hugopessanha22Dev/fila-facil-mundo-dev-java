package com.mundodev.filafacil.auditoria.repo;

import java.util.Map;
import java.util.Optional;

public interface ReembolsoRepository {

    Optional<Map<String, Object>> findById(long id);

    /**
     * Aprova reembolso pendente. Se failAfterLock=true, simula falha apos ler (para teste de rollback).
     * @return snapshot antigo antes da aprovacao
     */
    Map<String, Object> aprovarEmTransacao(long id, boolean failAfterLock);
}
