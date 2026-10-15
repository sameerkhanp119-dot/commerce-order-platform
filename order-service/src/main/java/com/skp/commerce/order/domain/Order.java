package com.skp.commerce.order.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Order aggregate. All state changes go through methods that guard the allowed transitions. */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "failure_reason")
    private String failureReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Order() {
        // for JPA
    }

    public static Order place(String customerId, String idempotencyKey, String currency, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("an order needs at least one line");
        }
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.idempotencyKey = idempotencyKey;
        order.currency = currency;
        order.lines = new ArrayList<>(lines);
        order.totalAmount = lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.status = OrderStatus.PENDING_PAYMENT;
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;
        return order;
    }

    /** Payment succeeded. Re-delivery of the same event is a no-op. */
    public void confirm() {
        if (status == OrderStatus.CONFIRMED) {
            return;
        }
        requireStatus(OrderStatus.PENDING_PAYMENT, "confirm");
        status = OrderStatus.CONFIRMED;
        updatedAt = Instant.now();
    }

    /** Saga compensation: payment failed, so the order is cancelled. Re-delivery is a no-op. */
    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            return;
        }
        requireStatus(OrderStatus.PENDING_PAYMENT, "cancel");
        status = OrderStatus.CANCELLED;
        failureReason = reason;
        updatedAt = Instant.now();
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("cannot " + action + " order " + id + " in status " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
