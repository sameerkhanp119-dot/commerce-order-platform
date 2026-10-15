package com.skp.commerce.payment.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway =
            new SimulatedPaymentGateway(new SimulatorProperties(new BigDecimal("100.00")));

    @Test
    void approvesAtOrBelowLimit() {
        assertThat(gateway.charge("c", new BigDecimal("100.00"), "USD", "order-1").approved()).isTrue();
    }

    @Test
    void declinesAboveLimit() {
        PaymentGateway.ChargeResult result = gateway.charge("c", new BigDecimal("100.01"), "USD", "order-1");
        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).contains("exceeds limit");
    }

    @Test
    void sameIdempotencyKeyGivesSameReference() {
        String first = gateway.charge("c", BigDecimal.ONE, "USD", "order-1").reference();
        String second = gateway.charge("c", BigDecimal.ONE, "USD", "order-1").reference();
        assertThat(first).isEqualTo(second);
    }
}
