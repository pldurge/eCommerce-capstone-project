package com.capstone.orderservice.controller;

import com.capstone.orderservice.dto.CreateOrderResponse;
import com.capstone.orderservice.dto.OrderDto;
import com.capstone.orderservice.model.Order;
import com.capstone.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestHeader("X-User-Name") String userId,
            @RequestBody Order.ShippingAddress shippingAddress) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(userId, shippingAddress));
    }

    /** CUSTOMER sees own orders; ADMIN sees all. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderDto>> getOrders(
            @RequestHeader("X-User-Name") String userId,
            @RequestHeader("X-User-Role")  String role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<OrderDto> orders = role.equals("ROLE_ADMIN")
                ? orderService.getAllOrders(PageRequest.of(page, size))
                : orderService.getUserOrders(userId, PageRequest.of(page, size));

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#id, authentication.name)")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderDto(id));
    }

    /** Admin: update order status via API (e.g. mark SHIPPED, DELIVERED). */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable String id,
            @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    /**
     * Internal endpoint — called by payment-service after Stripe confirms payment.
     * Not exposed via the API Gateway (no route defined for /api/orders/internal/**).
     */
    @PatchMapping("/internal/{orderId}/status")
    public ResponseEntity<OrderDto> updateOrderStatusInternal(
            @PathVariable String orderId,
            @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}
