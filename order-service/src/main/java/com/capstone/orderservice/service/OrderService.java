package com.capstone.orderservice.service;

import com.capstone.orderservice.model.Order;
import com.capstone.orderservice.model.OrderItem;
import com.capstone.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_UPDATED_TOPIC = "order-status-updated";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Order createOrder(String userId, List<OrderItem> items, Order.ShippingAddress address) {
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .shippingAddress(address)
                .totalAmount(total)
                .status(Order.OrderStatus.PAYMENT_PENDING)
                .build();

        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);
        order = orderRepository.save(order);

        // Publish to Kafka → Payment Service will consume
        kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getId().toString(),
                Map.of("orderId", order.getId(), "orderNumber", order.getOrderNumber(),
                        "userId", userId, "totalAmount", total));

        log.info("Order created: {} for user: {}", order.getOrderNumber(), userId);
        return order;
    }

    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }

    public Page<Order> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, Order.OrderStatus newStatus) {
        Order order = getOrder(orderId);
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        kafkaTemplate.send(ORDER_UPDATED_TOPIC, orderId.toString(),
                Map.of("orderId", orderId, "orderNumber", order.getOrderNumber(),
                        "userId", order.getUserId(), "newStatus", newStatus.name()));

        log.info("Order {} status updated to: {}", order.getOrderNumber(), newStatus);
        return order;
    }

    // Listen for payment confirmation events from Payment Service
    @KafkaListener(topics = "payment-confirmed", groupId = "order-service-group")
    public void handlePaymentConfirmed(Map<String, Object> event) {
        try {
            UUID orderId = UUID.fromString(event.get("orderId").toString());
            String paymentId = event.get("paymentId").toString();

            Order order = getOrder(orderId);
            order.setPaymentId(paymentId);
            order.setStatus(Order.OrderStatus.PAYMENT_CONFIRMED);
            orderRepository.save(order);

            log.info("Payment confirmed for order: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing payment confirmation event", e);
        }
    }

    // Listen for payment failure events
    @KafkaListener(topics = "payment-failed", groupId = "order-service-group")
    public void handlePaymentFailed(Map<String, Object> event) {
        try {
            UUID orderId = UUID.fromString(event.get("orderId").toString());
            updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
            log.info("Order {} cancelled due to payment failure", orderId);
        } catch (Exception e) {
            log.error("Error processing payment failure event", e);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Admin-only: fetch all orders across all users. */
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}
