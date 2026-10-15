package com.skp.commerce.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO-4217 code") String currency,
        @NotEmpty List<@Valid Line> lines) {

    public record Line(
            @NotBlank String sku,
            @Positive int quantity,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {
    }
}
