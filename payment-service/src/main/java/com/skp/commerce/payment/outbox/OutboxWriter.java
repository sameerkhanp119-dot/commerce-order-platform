package com.skp.commerce.payment.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Serialises an event and stores it in the outbox. Must be called inside the caller's transaction. */
@Component
public class OutboxWriter {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void write(UUID eventId, String aggregateId, String eventType, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new OutboxEvent(eventId, aggregateId, eventType, topic, payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialise " + eventType, e);
        }
    }
}
