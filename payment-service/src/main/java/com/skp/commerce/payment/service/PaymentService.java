package com.skp.commerce.payment.service;

import com.skp.commerce.events.OrderCreatedEvent;
import com.skp.commerce.events.PaymentCompletedEvent;
import com.skp.commerce.events.PaymentFailedEvent;
import com.skp.commerce.events.Topics;
import com.skp.commerce.payment.domain.Payment;
import com.skp.commerce.payment.domain.PaymentRepository;
import com.skp.commerce.payment.gateway.PaymentGateway;
import com.skp.commerce.payment.gateway.PaymentGateway.ChargeResult;
import com.skp.commerce.payment.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final OutboxWriter outbox;

    public PaymentService(PaymentRepository payments, PaymentGateway gateway, OutboxWriter outbox) {
        this.payments = payments;
        this.gateway = gateway;
        this.outbox = outbox;
    }

    /**
     * Charges the customer for a newly created order and records the outcome event in the outbox.
     * Duplicate deliveries of the same OrderCreated event are skipped; the order id is also passed
     * to the gateway as its idempotency key so a retried charge is never taken twice.
     */
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (payments.existsByOrderId(event.orderId())) {
            log.info("payment for order {} already processed; skipping duplicate event", event.orderId());
            return;
        }

        ChargeResult result = gateway.charge(event.customerId(), event.amount(), event.currency(),
                event.orderId().toString());
        UUID eventId = UUID.randomUUID();
        String aggregateId = event.orderId().toString();

        if (result.approved()) {
            Payment payment = payments.save(Payment.approved(event.orderId(), event.customerId(), event.amount(),
                    event.currency(), result.reference()));
            outbox.write(eventId, aggregateId, PaymentCompletedEvent.TYPE, Topics.PAYMENT_EVENTS,
                    new PaymentCompletedEvent(eventId, event.orderId(), payment.getId(), event.amount(), Instant.now()));
            log.info("payment {} approved for order {}", payment.getId(), event.orderId());
        } else {
            payments.save(Payment.declined(event.orderId(), event.customerId(), event.amount(),
                    event.currency(), result.declineReason()));
            outbox.write(eventId, aggregateId, PaymentFailedEvent.TYPE, Topics.PAYMENT_EVENTS,
                    new PaymentFailedEvent(eventId, event.orderId(), result.declineReason(), Instant.now()));
            log.info("payment declined for order {}: {}", event.orderId(), result.declineReason());
        }
    }
}
