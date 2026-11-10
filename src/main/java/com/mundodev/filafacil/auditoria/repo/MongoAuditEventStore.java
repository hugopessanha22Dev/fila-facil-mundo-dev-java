package com.mundodev.filafacil.auditoria.repo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mundodev.filafacil.auditoria.model.AuditEvent;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MongoAuditEventStore implements AuditEventStore, AutoCloseable {

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoAuditEventStore(String uri, String database) {
        this.client = MongoClients.create(uri);
        MongoDatabase db = client.getDatabase(database);
        this.collection = db.getCollection("audit_events");
    }

    @Override
    public void append(AuditEvent event) {
        Document doc = new Document()
                .append("tipo", event.tipo())
                .append("usuario", event.usuario())
                .append("entidade", event.entidade())
                .append("entidadeId", event.entidadeId())
                .append("dadosAntigos", event.dadosAntigos())
                .append("dadosNovos", event.dadosNovos())
                .append("em", event.em().toString());
        collection.insertOne(doc);
    }

    @Override
    public List<AuditEvent> listRecent(int limit) {
        List<AuditEvent> out = new ArrayList<>();
        collection.find()
                .sort(new Document("em", -1))
                .limit(Math.max(1, limit))
                .forEach(doc -> out.add(toEvent(doc)));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static AuditEvent toEvent(Document doc) {
        Map<String, Object> antigos = doc.get("dadosAntigos") instanceof Map
                ? new HashMap<>((Map<String, Object>) doc.get("dadosAntigos"))
                : Map.of();
        Map<String, Object> novos = doc.get("dadosNovos") instanceof Map
                ? new HashMap<>((Map<String, Object>) doc.get("dadosNovos"))
                : Map.of();
        Instant em = Instant.parse(doc.getString("em"));
        return new AuditEvent(
                doc.getString("tipo"),
                doc.getString("usuario"),
                doc.getString("entidade"),
                doc.getString("entidadeId"),
                antigos,
                novos,
                em);
    }

    @Override
    public void close() {
        client.close();
    }
}
