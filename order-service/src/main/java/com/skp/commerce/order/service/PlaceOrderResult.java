package com.skp.commerce.order.service;

import com.skp.commerce.order.domain.Order;

/** @param created false when an existing order was returned for a repeated Idempotency-Key */
public record PlaceOrderResult(Order order, boolean created) {
}
