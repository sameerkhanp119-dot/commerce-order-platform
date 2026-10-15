package com.skp.commerce.payment.service;

import com.skp.commerce.events.OrderCreatedEvent;
import com.skp.commerce.events.PaymentCompletedEvent;
import com.skp.commerce.events.PaymentFailedEvent;
import com.skp.commerce.events.Topics;
import com.skp.commerce.payment.domain.Payment;
import com.skp.commerce.payment.domain.PaymentRepository;
import com.skp.commerce.payment.domain.PaymentStatus;
import com.skp.commerce.payment.gateway.PaymentGateway;
import com.skp.commerce.payment.gateway.PaymentGateway.ChargeResult;
import com.skp.commerce.payment.outbox.OutboxWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository payments;

    @Mock
    private PaymentGateway gateway;

    @Mock
    private OutboxWriter outbox;

    private PaymentService service;

    private final OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(),
            "cust-1", new BigDecimal("49.99"), "USD", Instant.now());

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, gateway, outbox);
    }

    @Test
    void approvedChargePublishesPaymentCompleted() {
        when(payments.existsByOrderId(event.orderId())).thenReturn(false);
        when(gateway.charge(anyString(), any(), anyString(), anyString())).thenReturn(ChargeResult.approved("ref-1"));
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleOrderCreated(event);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(outbox).write(any(UUID.class), eq(event.orderId().toString()), eq(PaymentCompletedEvent.TYPE),
                eq(Topics.PAYMENT_EVENTS), any(PaymentCompletedEvent.class));
    }

    @Test
    void declinedChargePublishesPaymentFailed() {
        when(payments.existsByOrderId(event.orderId())).thenReturn(false);
        when(gateway.charge(anyString(), any(), anyString(), anyString()))
                .thenReturn(ChargeResult.declined("insufficient funds"));

        service.handleOrderCreated(event);

        verify(outbox).write(any(UUID.class), eq(event.orderId().toString()), eq(PaymentFailedEvent.TYPE),
                eq(Topics.PAYMENT_EVENTS), any(PaymentFailedEvent.class));
    }

    @Test
    void duplicateEventIsIgnored() {
        when(payments.existsByOrderId(event.orderId())).thenReturn(true);

        service.handleOrderCreated(event);

        verifyNoInteractions(gateway);
        verify(payments, never()).save(any());
        verifyNoInteractions(outbox);
    }
}
