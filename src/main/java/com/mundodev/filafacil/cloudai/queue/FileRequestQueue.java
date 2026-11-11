package com.mundodev.filafacil.cloudai.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FileRequestQueue {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<QueueMessage>> LIST = new TypeReference<>() {
    };

    private final Path file;
    private final String environment;

    public FileRequestQueue(Path root, String environment) {
        this.environment = environment;
        this.file = root.resolve(environment).resolve("queue").resolve("messages.json");
    }

    public synchronized String enqueue(String text, String origin) throws IOException {
        List<QueueMessage> all = read();
        String id = UUID.randomUUID().toString();
        all.add(new QueueMessage(id, environment, text, origin, QueueState.ACEITA, ""));
        write(all);
        return id;
    }

    public synchronized List<QueueMessage> list() throws IOException {
        return List.copyOf(read());
    }

    public synchronized QueueMessage nextAccepted() throws IOException {
        List<QueueMessage> all = read();
        for (int i = 0; i < all.size(); i++) {
            QueueMessage message = all.get(i);
            if (message.state() == QueueState.ACEITA) {
                QueueMessage processing = message.withState(QueueState.EM_PROCESSAMENTO, "");
                all.set(i, processing);
                write(all);
                return processing;
            }
        }
        return null;
    }

    public synchronized void update(QueueMessage message) throws IOException {
        List<QueueMessage> all = read();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(message.id())) {
                all.set(i, message);
                write(all);
                return;
            }
        }
        throw new IllegalArgumentException("Mensagem ausente: " + message.id());
    }

    public synchronized int depth() throws IOException {
        int count = 0;
        for (QueueMessage message : read()) {
            if (message.state() == QueueState.ACEITA || message.state() == QueueState.EM_PROCESSAMENTO) {
                count++;
            }
        }
        return count;
    }

    private List<QueueMessage> read() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(MAPPER.readValue(file.toFile(), LIST));
    }

    private void write(List<QueueMessage> all) throws IOException {
        Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), all);
    }
}
