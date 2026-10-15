package com.skp.commerce.payment.gateway;

import java.math.BigDecimal;

/** Port to an external card processor. Swap the implementation for Stripe, Adyen, etc. */
public interface PaymentGateway {

    ChargeResult charge(String customerId, BigDecimal amount, String currency, String idempotencyKey);

    record ChargeResult(boolean approved, String reference, String declineReason) {

        public static ChargeResult approved(String reference) {
            return new ChargeResult(true, reference, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}
