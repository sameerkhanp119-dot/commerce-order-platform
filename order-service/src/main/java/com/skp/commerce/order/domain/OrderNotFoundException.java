package com.skp.commerce.order.domain;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("order " + id + " not found");
    }
}
