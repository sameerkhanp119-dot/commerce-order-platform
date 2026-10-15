package com.skp.commerce.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One payment attempt per order. The unique order_id column makes event re-delivery harmless. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
        // for JPA
    }

    private Payment(UUID orderId, String customerId, BigDecimal amount, String currency, PaymentStatus status,
                    String gatewayReference, String declineReason) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.gatewayReference = gatewayReference;
        this.declineReason = declineReason;
        this.createdAt = Instant.now();
    }

    public static Payment approved(UUID orderId, String customerId, BigDecimal amount, String currency,
                                   String gatewayReference) {
        return new Payment(orderId, customerId, amount, currency, PaymentStatus.APPROVED, gatewayReference, null);
    }

    public static Payment declined(UUID orderId, String customerId, BigDecimal amount, String currency,
                                   String reason) {
        return new Payment(orderId, customerId, amount, currency, PaymentStatus.DECLINED, null, reason);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public String getDeclineReason() {
        return declineReason;
    }
}
