package com.skp.commerce.events;

/** Kafka topic names shared by all services. */
public final class Topics {
    public static final String ORDER_EVENTS = "commerce.order.events.v1";
    public static final String PAYMENT_EVENTS = "commerce.payment.events.v1";

    /** Header carrying the logical event type so consumers can route without type info in the payload. */
    public static final String EVENT_TYPE_HEADER = "event-type";

    private Topics() {
    }
}
