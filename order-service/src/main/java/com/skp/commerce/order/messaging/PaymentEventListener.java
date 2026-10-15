package com.skp.commerce.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skp.commerce.events.PaymentCompletedEvent;
import com.skp.commerce.events.PaymentFailedEvent;
import com.skp.commerce.events.Topics;
import com.skp.commerce.order.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Second half of the saga: applies payment outcomes to orders. Handlers are idempotent. */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "order-service")
    public void onPaymentEvent(ConsumerRecord<String, String> record) throws IOException {
        Header typeHeader = record.headers().lastHeader(Topics.EVENT_TYPE_HEADER);
        String type = typeHeader == null ? "" : new String(typeHeader.value(), StandardCharsets.UTF_8);

        switch (type) {
            case PaymentCompletedEvent.TYPE -> {
                PaymentCompletedEvent event = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
                orderService.confirmOrder(event.orderId());
            }
            case PaymentFailedEvent.TYPE -> {
                PaymentFailedEvent event = objectMapper.readValue(record.value(), PaymentFailedEvent.class);
                orderService.cancelOrder(event.orderId(), event.reason());
            }
            default -> log.warn("ignoring unknown event type '{}' at offset {}", type, record.offset());
        }
    }
}
