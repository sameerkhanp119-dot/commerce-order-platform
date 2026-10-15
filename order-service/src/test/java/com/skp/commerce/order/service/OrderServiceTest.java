package com.skp.commerce.order.service;

import com.skp.commerce.events.OrderCreatedEvent;
import com.skp.commerce.events.Topics;
import com.skp.commerce.order.api.CreateOrderRequest;
import com.skp.commerce.order.domain.Order;
import com.skp.commerce.order.domain.OrderLine;
import com.skp.commerce.order.domain.OrderNotFoundException;
import com.skp.commerce.order.domain.OrderRepository;
import com.skp.commerce.order.domain.OrderStatus;
import com.skp.commerce.order.outbox.OutboxWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private OutboxWriter outbox;

    private OrderService service;

    private final CreateOrderRequest request = new CreateOrderRequest("cust-1", "USD",
            List.of(new CreateOrderRequest.Line("SKU-1", 3, new BigDecimal("10.00"))));

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, outbox);
    }

    @Test
    void newOrderIsSavedAndEventWrittenToOutbox() {
        when(orders.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceOrderResult result = service.placeOrder("key-1", request);

        assertThat(result.created()).isTrue();
        assertThat(result.order().getTotalAmount()).isEqualByComparingTo("30.00");
        verify(outbox).write(any(UUID.class), eq(result.order().getId().toString()),
                eq(OrderCreatedEvent.TYPE), eq(Topics.ORDER_EVENTS), any(OrderCreatedEvent.class));
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingOrderWithoutNewEvent() {
        Order existing = Order.place("cust-1", "key-1", "USD",
                List.of(new OrderLine("SKU-1", 3, new BigDecimal("10.00"))));
        when(orders.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        PlaceOrderResult result = service.placeOrder("key-1", request);

        assertThat(result.created()).isFalse();
        assertThat(result.order()).isSameAs(existing);
        verify(orders, never()).save(any());
        verify(outbox, never()).write(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void paymentFailureCancelsOrder() {
        Order order = Order.place("cust-1", "key-1", "USD",
                List.of(new OrderLine("SKU-1", 1, BigDecimal.TEN)));
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        service.cancelOrder(order.getId(), "insufficient funds");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void unknownOrderRaisesNotFound() {
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmOrder(id)).isInstanceOf(OrderNotFoundException.class);
    }
}
