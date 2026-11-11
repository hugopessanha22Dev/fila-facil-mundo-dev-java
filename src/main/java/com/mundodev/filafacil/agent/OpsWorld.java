package com.mundodev.filafacil.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpsWorld {

    public record Delivery(String codigo, String status, String clienteId, String clienteNome) {
    }

    public record Client(String id, String nome) {
    }

    public record Occurrence(String codigo, String tipo, String descricao) {
    }

    public record Notice(String payload, long instante, String resultado) {
    }

    public record Escalation(String motivo, String estado, String dados, String codigo) {
    }

    private final Map<String, Delivery> deliveries = new LinkedHashMap<>();
    private final Map<String, Client> clients = new LinkedHashMap<>();
    private final List<Occurrence> occurrences = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();
    private final List<Escalation> escalations = new ArrayList<>();
    private boolean consultaIndisponivel;
    private boolean erroEscrita;

    public static OpsWorld demo() {
        OpsWorld world = new OpsWorld();
        world.addDelivery(new Delivery("ENT-100", "atrasada", "CLI-1", "Cliente Atraso"));
        world.addDelivery(new Delivery("ENT-200", "pendente", "CLI-2", "Cliente Endereco"));
        world.addDelivery(new Delivery("COL-300", "falha", "CLI-3", "Motorista Coleta"));
        world.addClient(new Client("CLI-1", "Cliente Atraso"));
        world.addClient(new Client("CLI-2", "Cliente Endereco"));
        world.addClient(new Client("CLI-3", "Motorista Coleta"));
        return world;
    }

    public void addDelivery(Delivery delivery) {
        deliveries.put(delivery.codigo(), delivery);
    }

    public void addClient(Client client) {
        clients.put(client.id(), client);
    }

    public Delivery delivery(String codigo) {
        return deliveries.get(codigo);
    }

    public Client client(String id) {
        return clients.get(id);
    }

    public void addOccurrence(Occurrence occurrence) {
        occurrences.add(occurrence);
    }

    public List<Occurrence> occurrences() {
        return List.copyOf(occurrences);
    }

    public void addNotice(Notice notice) {
        notices.add(notice);
    }

    public List<Notice> notices() {
        return List.copyOf(notices);
    }

    public void addEscalation(Escalation escalation) {
        escalations.add(escalation);
    }

    public List<Escalation> escalations() {
        return List.copyOf(escalations);
    }

    public boolean consultaIndisponivel() {
        return consultaIndisponivel;
    }

    public void setConsultaIndisponivel(boolean consultaIndisponivel) {
        this.consultaIndisponivel = consultaIndisponivel;
    }

    public boolean erroEscrita() {
        return erroEscrita;
    }

    public void setErroEscrita(boolean erroEscrita) {
        this.erroEscrita = erroEscrita;
    }
}
