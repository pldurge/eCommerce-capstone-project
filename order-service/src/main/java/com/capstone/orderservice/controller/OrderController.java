package com.capstone.orderservice.controller;

import com.capstone.orderservice.model.Order;
import com.capstone.orderservice.model.OrderItem;
import com.capstone.orderservice.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create an order. Any authenticated user can place an order for themselves.
     * The userId comes from X-User-Name (set by gateway), NOT from the request body —
     * this prevents a user from placing orders on behalf of someone else.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> createOrder(
            @RequestHeader("X-User-Name") String userId,
            @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(userId, request.getItems(), request.getShippingAddress());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * List orders. CUSTOMER sees only their own; ADMIN sees all.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Order>> getOrders(
            @RequestHeader("X-User-Name") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Order> orders = role.equals("ROLE_ADMIN")
                ? orderService.getAllOrders(PageRequest.of(page, size))      // admin sees all
                : orderService.getUserOrders(userId, PageRequest.of(page, size)); // customer sees own

        return ResponseEntity.ok(orders);
    }

    /**
     * Get a specific order.
     * - ADMIN can fetch any order.
     * - CUSTOMER can only fetch their own order (ownership enforced via OrderSecurityService).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#id, authentication.name)")
    public ResponseEntity<Order> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    /**
     * Lookup order by human-readable order number.
     * Same ownership rules as getOrder.
     */
    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwnerByNumber(#orderNumber, authentication.name)")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    /**
     * Update order status (e.g. SHIPPED, DELIVERED). Admin only.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @Data
    public static class CreateOrderRequest {
        private List<OrderItem> items;
        private Order.ShippingAddress shippingAddress;
    }
}
