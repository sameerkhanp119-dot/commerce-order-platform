package com.skp.commerce.events;

import java.time.Instant;
import java.util.UUID;

/** Published by payment-service when a charge is declined; order-service compensates by cancelling. */
public record PaymentFailedEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        Instant occurredAt) {

    public static final String TYPE = "PaymentFailed";
}
