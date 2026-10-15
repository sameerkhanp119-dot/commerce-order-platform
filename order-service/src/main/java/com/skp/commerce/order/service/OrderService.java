package com.skp.commerce.order.service;

import com.skp.commerce.events.OrderCreatedEvent;
import com.skp.commerce.events.Topics;
import com.skp.commerce.order.api.CreateOrderRequest;
import com.skp.commerce.order.api.OrderResponse;
import com.skp.commerce.order.domain.Order;
import com.skp.commerce.order.domain.OrderLine;
import com.skp.commerce.order.domain.OrderNotFoundException;
import com.skp.commerce.order.domain.OrderRepository;
import com.skp.commerce.order.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    public static final String ORDER_CACHE = "orders";

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final OutboxWriter outbox;

    public OrderService(OrderRepository orders, OutboxWriter outbox) {
        this.orders = orders;
        this.outbox = outbox;
    }

    /** Saves the order and its OrderCreated event atomically (transactional outbox). */
    @Transactional
    public PlaceOrderResult placeOrder(String idempotencyKey, CreateOrderRequest request) {
        Optional<Order> existing = orders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("idempotent replay for key {} -> order {}", idempotencyKey, existing.get().getId());
            return new PlaceOrderResult(existing.get(), false);
        }

        List<OrderLine> lines = request.lines().stream()
                .map(l -> new OrderLine(l.sku(), l.quantity(), l.unitPrice()))
                .toList();
        Order order = orders.save(Order.place(request.customerId(), idempotencyKey, request.currency(), lines));

        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(eventId, order.getId(), order.getCustomerId(),
                order.getTotalAmount(), order.getCurrency(), Instant.now());
        outbox.write(eventId, order.getId().toString(), OrderCreatedEvent.TYPE, Topics.ORDER_EVENTS, event);

        log.info("order {} placed for customer {} total {} {}", order.getId(), order.getCustomerId(),
                order.getTotalAmount(), order.getCurrency());
        return new PlaceOrderResult(order, true);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ORDER_CACHE, key = "#id")
    public OrderResponse getOrder(UUID id) {
        return orders.findById(id).map(OrderResponse::from).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    @CacheEvict(cacheNames = ORDER_CACHE, key = "#orderId")
    public void confirmOrder(UUID orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.confirm();
        log.info("order {} confirmed", orderId);
    }

    @Transactional
    @CacheEvict(cacheNames = ORDER_CACHE, key = "#orderId")
    public void cancelOrder(UUID orderId, String reason) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel(reason);
        log.info("order {} cancelled: {}", orderId, reason);
    }
}
