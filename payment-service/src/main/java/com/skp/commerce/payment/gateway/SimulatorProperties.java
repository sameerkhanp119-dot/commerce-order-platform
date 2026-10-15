package com.skp.commerce.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** @param declineAbove charges strictly above this amount are declined, to exercise the compensation path */
@ConfigurationProperties(prefix = "payment.simulator")
public record SimulatorProperties(BigDecimal declineAbove) {

    public SimulatorProperties {
        if (declineAbove == null) {
            declineAbove = new BigDecimal("1000.00");
        }
    }
}
