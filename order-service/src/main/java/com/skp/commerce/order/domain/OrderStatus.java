package com.skp.commerce.order.domain;

/**
 * Order lifecycle. Transitions are enforced in {@link Order}:
 * PENDING_PAYMENT -> CONFIRMED, or PENDING_PAYMENT -> CANCELLED (saga compensation).
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED
}
