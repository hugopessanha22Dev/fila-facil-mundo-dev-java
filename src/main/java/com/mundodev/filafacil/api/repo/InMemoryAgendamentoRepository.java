package com.mundodev.filafacil.api.repo;

import com.mundodev.filafacil.api.error.ApiValidationException;
import com.mundodev.filafacil.api.model.Agendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAgendamentoRepository implements AgendamentoRepository {

    private final AtomicLong seq = new AtomicLong(0);
    private final Map<Long, Agendamento> store = new ConcurrentHashMap<>();

    @Override
    public Agendamento insert(Agendamento agendamento) {
        long id = seq.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        Agendamento saved = new Agendamento(
                id,
                agendamento.transportadora(),
                agendamento.placa(),
                agendamento.doca(),
                agendamento.dataHora(),
                agendamento.status(),
                agendamento.anexoRef(),
                now,
                now);
        store.put(id, saved);
        return saved;
    }

    @Override
    public List<Agendamento> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Agendamento::id))
                .toList();
    }

    @Override
    public List<Agendamento> findByDate(LocalDate data) {
        return store.values().stream()
                .filter(a -> a.dataHora().toLocalDate().equals(data))
                .sorted(Comparator.comparing(Agendamento::id))
                .toList();
    }

    @Override
    public Optional<Agendamento> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Agendamento updateStatus(long id, String status) {
        Agendamento current = require(id);
        Agendamento updated = new Agendamento(
                current.id(),
                current.transportadora(),
                current.placa(),
                current.doca(),
                current.dataHora(),
                status,
                current.anexoRef(),
                current.createdAt(),
                LocalDateTime.now());
        store.put(id, updated);
        return updated;
    }

    @Override
    public Agendamento updateAnexoRef(long id, String anexoRef) {
        Agendamento current = require(id);
        Agendamento updated = new Agendamento(
                current.id(),
                current.transportadora(),
                current.placa(),
                current.doca(),
                current.dataHora(),
                current.status(),
                anexoRef,
                current.createdAt(),
                LocalDateTime.now());
        store.put(id, updated);
        return updated;
    }

    private Agendamento require(long id) {
        Agendamento current = store.get(id);
        if (current == null) {
            throw new ApiValidationException("Agendamento nao encontrado: " + id);
        }
        return current;
    }

    public void clear() {
        store.clear();
        seq.set(0);
    }

    public List<Agendamento> snapshot() {
        return new ArrayList<>(findAll());
    }
}
