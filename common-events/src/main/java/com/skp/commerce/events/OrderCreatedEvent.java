package com.skp.commerce.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by order-service when a new order is accepted and is waiting for payment. */
public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        String customerId,
        BigDecimal amount,
        String currency,
        Instant occurredAt) {

    public static final String TYPE = "OrderCreated";
}
