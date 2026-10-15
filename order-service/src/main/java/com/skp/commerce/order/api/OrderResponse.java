package com.skp.commerce.order.api;

import com.skp.commerce.order.domain.Order;
import com.skp.commerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        String failureReason,
        List<Line> lines,
        Instant createdAt,
        Instant updatedAt) {

    public record Line(String sku, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getFailureReason(),
                order.getLines().stream()
                        .map(l -> new Line(l.getSku(), l.getQuantity(), l.getUnitPrice()))
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
