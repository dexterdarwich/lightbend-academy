package com.lightbend.lambdas;

import java.time.Instant;
import java.util.UUID;

class Message {
    private final UUID id;
    private final Instant timestamp;
    private final String content;

    Message(String content) {
        this(UUID.randomUUID(), Instant.now(), content);
    }

    Message(UUID id, Instant timestamp, String content) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
    }

    UUID getId() {
        return id;
    }

    Instant getTimestamp() {
        return timestamp;
    }

    String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Message("+id.toString()+","+timestamp.toString()+","+ content +")";
    }
}
