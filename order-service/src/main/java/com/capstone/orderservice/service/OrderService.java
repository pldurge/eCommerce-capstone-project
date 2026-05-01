package com.capstone.orderservice.service;

import com.capstone.orderservice.client.CartServiceClient;
import com.capstone.orderservice.client.PaymentServiceClient;
import com.capstone.orderservice.client.ProductCatalogClient;
import com.capstone.orderservice.dto.*;
import com.capstone.orderservice.exceptions.CartEmptyException;
import com.capstone.orderservice.exceptions.InsufficientStockException;
import com.capstone.orderservice.exceptions.OrderNotFoundException;
import com.capstone.orderservice.exceptions.PaymentServiceException;
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
    private final PaymentServiceClient paymentServiceClient;

    /*
     * Create an order directly from the user's cart.
     * Only the shipping address is passed in — items and prices come from cart-service.
     * Stock is validated before the order is persisted.
     */

    @Transactional
    public CreateOrderResponse createOrder(String userId, Order.ShippingAddress address) {

        // 1. Get Cart
        CartResponse cart = cartServiceClient.getCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new CartEmptyException();
        }

        // 2. Validate stock for every item
        for (CartItemResponse cartItem : cart.getItems()) {
            ProductResponse product = productCatalogClient.getProduct(cartItem.getProductId());

            if (product == null) {
                throw new InsufficientStockException(
                        "Product not found: " + cartItem.getProductId());
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        cartItem.getProductName(),
                        product.getStockQuantity() == null ? 0 : product.getStockQuantity(),
                        cartItem.getQuantity());
            }
        }

        // 3. Map cart items → order items (use ArrayList — JPA cascade requires a mutable list)
        List<OrderItem> items = new java.util.ArrayList<>(cart.getItems().stream()
                .map(ci -> OrderItem.builder()
                        .productId(ci.getProductId())
                        .productName(ci.getProductName())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getPrice() != null ? ci.getPrice() : BigDecimal.ZERO)
                        .build())
                .toList());

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
                .paymentMethod(Order.PaymentMethod.PENDING)
                .build();

        order = orderRepository.save(order);
        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);
        order = orderRepository.save(order);

        // 6. Clear cart now that order is placed
        cartServiceClient.clearCart(userId);

        // 7. Publish order-created → product-catalog-service (stock) + notification-service (email)
        List<Map<String, Object>> itemPayload = new java.util.ArrayList<>(items.stream()
                .map(i -> Map.<String, Object>of(
                        "productId", i.getProductId().toString(),
                        "quantity",  i.getQuantity()))
                .toList());

        kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getId().toString(),
                Map.of("orderId",      order.getId(),
                        "orderNumber",  order.getOrderNumber(),
                        "userId",       userId,
                        "totalAmount",  total,
                        "items",        itemPayload));

        log.info("Order {} created for userId={}, total={}", order.getOrderNumber(), userId, total);

        try {
            PaymentServiceClient.CheckoutResult checkout =
                    paymentServiceClient.initiatePayment(order.getId().toString(), total, userId);

            return CreateOrderResponse.of(order, checkout.checkoutUrl(), checkout.sessionId());

        } catch (PaymentServiceException ex) {
            log.warn("Order {} created but payment session failed: {}", order.getOrderNumber(), ex.getMessage());
            return CreateOrderResponse.withoutPaymentUrl(order, ex.getMessage());
        }
    }

    public Order getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public OrderDto getOrderDto(String id) {
        return OrderDto.from(getOrder(id));
    }

    public Page<OrderDto> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderDto::from);
    }

    public Page<OrderDto> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderDto::from);
    }

    @Transactional
    public OrderDto updateOrderStatus(String orderId, Order.OrderStatus newStatus) {
        Order order = getOrder(orderId);
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        kafkaTemplate.send(ORDER_UPDATED_TOPIC, orderId,
                Map.of("orderId",     orderId,
                        "orderNumber", order.getOrderNumber(),
                        "userId",      order.getUserId(),
                        "newStatus",   newStatus.name()));

        log.info("Order {} status → {}", order.getOrderNumber(), newStatus);
        return OrderDto.from(order);
    }

    // Kafka: payment-confirmed → mark order PAYMENT_CONFIRMED + store paymentId
    @KafkaListener(topics = "payment-confirmed", groupId = "order-service-group")
    public void handlePaymentConfirmed(Map<String, Object> event) {
        try {
            String   orderId   = event.get("orderId").toString();
            String paymentId = event.get("paymentId").toString();

            Order order = getOrder(orderId);
            order.setPaymentId(paymentId);
            order.setStatus(Order.OrderStatus.PAYMENT_CONFIRMED);
            order.setPaymentMethod(Order.PaymentMethod.STRIPE);
            orderRepository.save(order);
            log.info("Payment confirmed (Stripe) for order: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing payment-confirmed event: {}", e.getMessage());
        }
    }

    // Kafka: payment-failed → switch order to COD (cash on delivery)
    @KafkaListener(topics = "payment-failed", groupId = "order-service-group")
    public void handlePaymentFailed(Map<String, Object> event) {
        try {
            String orderId = event.get("orderId").toString();

            Order order = getOrder(orderId);
            order.setStatus(Order.OrderStatus.PAYMENT_PENDING);
            order.setPaymentMethod(Order.PaymentMethod.COD);
            orderRepository.save(order);

            kafkaTemplate.send(ORDER_UPDATED_TOPIC, orderId,
                    Map.of("orderId",     orderId,
                            "orderNumber", order.getOrderNumber(),
                            "userId",      order.getUserId(),
                            "newStatus",   "PAYMENT_PENDING",
                            "paymentMethod", "COD"));

            log.info("Order {} switched to COD after Stripe payment failure", orderId);
        } catch (Exception e) {
            log.error("Error processing payment-failed event: {}", e.getMessage());
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
