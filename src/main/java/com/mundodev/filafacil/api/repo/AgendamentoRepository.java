package com.mundodev.filafacil.api.repo;

import com.mundodev.filafacil.api.model.Agendamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository {

    Agendamento insert(Agendamento agendamento);

    List<Agendamento> findAll();

    List<Agendamento> findByDate(LocalDate data);

    Optional<Agendamento> findById(long id);

    Agendamento updateStatus(long id, String status);

    Agendamento updateAnexoRef(long id, String anexoRef);
}
