package com.skp.commerce.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published by payment-service when a charge for an order succeeds. */
public record PaymentCompletedEvent(
        UUID eventId,
        UUID orderId,
        UUID paymentId,
        BigDecimal amount,
        Instant occurredAt) {

    public static final String TYPE = "PaymentCompleted";
}
