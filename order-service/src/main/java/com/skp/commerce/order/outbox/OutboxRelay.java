package com.skp.commerce.order.outbox;

import com.skp.commerce.events.Topics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls unpublished outbox rows and sends them to Kafka, keyed by aggregate id so all events
 * for one order land on the same partition (preserving order). Delivery is at-least-once;
 * consumers are idempotent.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : batch) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(event.getTopic(), event.getAggregateId(), event.getPayload());
            record.headers().add(Topics.EVENT_TYPE_HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));
            try {
                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception e) {
                // Stop at the first failure so events for the same aggregate are not reordered.
                log.warn("outbox relay paused: failed to publish {} ({})", event.getId(), e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
    }
}
