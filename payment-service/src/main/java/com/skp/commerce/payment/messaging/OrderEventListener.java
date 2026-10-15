package com.skp.commerce.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skp.commerce.events.OrderCreatedEvent;
import com.skp.commerce.events.Topics;
import com.skp.commerce.payment.service.PaymentService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "payment-service")
    public void onOrderEvent(ConsumerRecord<String, String> record) throws IOException {
        Header typeHeader = record.headers().lastHeader(Topics.EVENT_TYPE_HEADER);
        String type = typeHeader == null ? "" : new String(typeHeader.value(), StandardCharsets.UTF_8);

        if (OrderCreatedEvent.TYPE.equals(type)) {
            paymentService.handleOrderCreated(objectMapper.readValue(record.value(), OrderCreatedEvent.class));
        } else {
            log.debug("ignoring event type '{}'", type);
        }
    }
}
