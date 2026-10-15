package com.skp.commerce.order.api;

import com.skp.commerce.order.service.OrderService;
import com.skp.commerce.order.service.PlaceOrderResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Places an order. Clients must send an Idempotency-Key; retries with the same key
     * return the original order with 200 instead of creating a duplicate.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        PlaceOrderResult result = orderService.placeOrder(idempotencyKey, request);
        OrderResponse body = OrderResponse.from(result.order());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(body);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }
}
