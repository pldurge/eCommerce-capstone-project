package com.capstone.orderservice.service;

import com.capstone.orderservice.client.CartServiceClient;
import com.capstone.orderservice.client.ProductCatalogClient;
import com.capstone.orderservice.dto.CartItemResponse;
import com.capstone.orderservice.dto.CartResponse;
import com.capstone.orderservice.dto.ProductResponse;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final CartServiceClient cartServiceClient;
    private final ProductCatalogClient productCatalogClient;

    /*
     * Create an order directly from the user's cart.
     * Only the shipping address is passed in — items and prices come from cart-service.
     * Stock is validated before the order is persisted.
     */

    @Transactional
    public Order createOrder(String userId, Order.ShippingAddress address) {

        // Get Cart
        CartResponse cart = cartServiceClient.getCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Add items before placing an order.");
        }

        // Validate Stock for every item
        for (CartItemResponse cartItem : cart.getItems()) {
            ProductResponse product =
                    productCatalogClient.getProduct(cartItem.getProductId());

            if (product == null) {
                throw new RuntimeException("Product not found: " + cartItem.getProductId());
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for \"" + cartItem.getProductName()
                                + "\". Available: " + (product.getStockQuantity() == null ? 0 : product.getStockQuantity())
                                + ", Requested: " + cartItem.getQuantity());
            }
        }

        List<OrderItem> items = cart.getItems().stream()
                .map(ci -> OrderItem.builder()
                        .productId(ci.getProductId())
                        .productName(ci.getProductName())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getPrice())
                        .build())
                .toList();

        // 4. Calculate total
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Persist order
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

        // 6. Clear cart now that order is placed
        cartServiceClient.clearCart(userId);

        // 7. Publish order-created → payment-service creates PENDING transaction
        //    and product-catalog-service reduces stock
        List<Map<String, Object>> itemPayload = items.stream()
                .map(i -> Map.<String, Object>of(
                        "productId", i.getProductId().toString(),
                        "quantity",  i.getQuantity()))
                .toList();

        kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getId().toString(),
                Map.of("orderId",      order.getId(),
                        "orderNumber",  order.getOrderNumber(),
                        "userId",       userId,
                        "totalAmount",  total,
                        "items",        itemPayload));

        log.info("Order {} created for userId={}, total={}", order.getOrderNumber(), userId, total);
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

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, Order.OrderStatus newStatus) {
        Order order = getOrder(orderId);
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        kafkaTemplate.send(ORDER_UPDATED_TOPIC, orderId.toString(),
                Map.of("orderId",     orderId,
                        "orderNumber", order.getOrderNumber(),
                        "userId",      order.getUserId(),
                        "newStatus",   newStatus.name()));

        log.info("Order {} status → {}", order.getOrderNumber(), newStatus);
        return order;
    }

    // Kafka: payment-confirmed → mark order PAYMENT_CONFIRMED + store paymentId
    @KafkaListener(topics = "payment-confirmed", groupId = "order-service-group")
    public void handlePaymentConfirmed(Map<String, Object> event) {
        try {
            UUID   orderId   = UUID.fromString(event.get("orderId").toString());
            String paymentId = event.get("paymentId").toString();

            Order order = getOrder(orderId);
            order.setPaymentId(paymentId);
            order.setStatus(Order.OrderStatus.PAYMENT_CONFIRMED);
            orderRepository.save(order);
            log.info("Payment confirmed for order: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing payment-confirmed event", e);
        }
    }

    // Kafka: payment-failed → cancel order
    @KafkaListener(topics = "payment-failed", groupId = "order-service-group")
    public void handlePaymentFailed(Map<String, Object> event) {
        try {
            UUID orderId = UUID.fromString(event.get("orderId").toString());
            updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
            log.info("Order {} cancelled due to payment failure", orderId);
        } catch (Exception e) {
            log.error("Error processing payment-failed event", e);
        }
    }

    /**
     * Generates a human-readable order number: ORD-YYYYMMDD-XXXXX
     * e.g. ORD-20250327-A3X7K
     */
    private String generateOrderNumber() {
        String date   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString()
                .replaceAll("-", "")
                .substring(0, 5)
                .toUpperCase();
        return "ORD-" + date + "-" + suffix;
    }
}
