package com.mundodev.filafacil.cloudai.queue;

public record QueueMessage(
        String id,
        String environment,
        String text,
        String origin,
        QueueState state,
        String error) {

    public QueueMessage withState(QueueState state, String error) {
        return new QueueMessage(id, environment, text, origin, state, error);
    }
}
