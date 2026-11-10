package com.mundodev.filafacil.api.service;

import com.mundodev.filafacil.api.error.ApiValidationException;
import com.mundodev.filafacil.api.model.Agendamento;
import com.mundodev.filafacil.api.repo.AgendamentoRepository;
import com.mundodev.filafacil.api.storage.AttachmentStorage;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AgendamentoService {

    public static final String STATUS_AGENDADO = "agendado";
    public static final String STATUS_CANCELADO = "cancelado";
    public static final String STATUS_EM_ATENDIMENTO = "em_atendimento";
    public static final String STATUS_CONCLUIDO = "concluido";

    private static final Set<String> STATUS_VALIDOS = Set.of(
            STATUS_AGENDADO, STATUS_CANCELADO, STATUS_EM_ATENDIMENTO, STATUS_CONCLUIDO);

    private final AgendamentoRepository repository;
    private final AttachmentStorage attachmentStorage;

    public AgendamentoService(AgendamentoRepository repository, AttachmentStorage attachmentStorage) {
        this.repository = repository;
        this.attachmentStorage = attachmentStorage;
    }

    public Agendamento criar(String transportadora, String placa, String doca, String dataHoraIso) {
        requireText(transportadora, "transportadora");
        requireText(placa, "placa");
        requireText(doca, "doca");
        LocalDateTime dataHora = parseDateTime(dataHoraIso);
        Agendamento novo = new Agendamento(
                null,
                transportadora.trim(),
                placa.trim().toUpperCase(Locale.ROOT),
                doca.trim(),
                dataHora,
                STATUS_AGENDADO,
                null,
                null,
                null);
        return repository.insert(novo);
    }

    public List<Agendamento> listar() {
        return repository.findAll();
    }

    public List<Agendamento> listarPorData(String dataIso) {
        if (dataIso == null || dataIso.isBlank()) {
            throw new ApiValidationException("Parametro data e obrigatorio (YYYY-MM-DD).");
        }
        try {
            LocalDate data = LocalDate.parse(dataIso.trim());
            return repository.findByDate(data);
        } catch (DateTimeParseException ex) {
            throw new ApiValidationException("Data invalida. Use YYYY-MM-DD.");
        }
    }

    public Agendamento atualizarStatus(long id, String status) {
        String normalized = requireText(status, "status").toLowerCase(Locale.ROOT);
        if (!STATUS_VALIDOS.contains(normalized)) {
            throw new ApiValidationException("Status invalido: " + status);
        }
        requireExists(id);
        return repository.updateStatus(id, normalized);
    }

    public Agendamento cancelar(long id) {
        requireExists(id);
        return repository.updateStatus(id, STATUS_CANCELADO);
    }

    public Agendamento anexar(long id, String filename, InputStream content, long sizeBytes) {
        requireExists(id);
        String ref = attachmentStorage.store(id, filename, content, sizeBytes);
        return repository.updateAnexoRef(id, ref);
    }

    private void requireExists(long id) {
        if (repository.findById(id).isEmpty()) {
            throw new ApiValidationException("Agendamento nao encontrado: " + id);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiValidationException("Campo obrigatorio ausente: " + field);
        }
        return value.trim();
    }

    private static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiValidationException("Campo obrigatorio ausente: dataHora");
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new ApiValidationException("dataHora invalida. Use ISO-8601 local, ex.: 2026-11-10T14:30:00");
        }
    }
}
