package com.skp.commerce.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order newOrder() {
        return Order.place("cust-1", "key-1", "USD", List.of(
                new OrderLine("SKU-1", 2, new BigDecimal("19.99")),
                new OrderLine("SKU-2", 1, new BigDecimal("5.00"))));
    }

    @Test
    void placedOrderIsPendingWithComputedTotal() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("44.98");
        assertThat(order.getId()).isNotNull();
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatThrownBy(() -> Order.place("cust-1", "key-1", "USD", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new OrderLine("SKU-1", 0, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmIsIdempotent() {
        Order order = newOrder();
        order.confirm();
        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelRecordsReasonAndIsIdempotent() {
        Order order = newOrder();
        order.cancel("card declined");
        order.cancel("card declined");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getFailureReason()).isEqualTo("card declined");
    }

    @Test
    void cannotCancelConfirmedOrder() {
        Order order = newOrder();
        order.confirm();

        assertThatThrownBy(() -> order.cancel("late failure"))
                .isInstanceOf(IllegalStateException.class);
    }
}
