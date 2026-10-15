package com.skp.commerce.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Deterministic stand-in for a real processor so the full saga can be demoed locally. */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final SimulatorProperties properties;

    public SimulatedPaymentGateway(SimulatorProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChargeResult charge(String customerId, BigDecimal amount, String currency, String idempotencyKey) {
        if (amount.compareTo(properties.declineAbove()) > 0) {
            return ChargeResult.declined("amount exceeds limit of " + properties.declineAbove() + " " + currency);
        }
        return ChargeResult.approved("sim_" + UUID.nameUUIDFromBytes(idempotencyKey.getBytes()));
    }
}
