package com.capstone.orderservice.dto;

import com.capstone.orderservice.model.Order;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Response returned after placing an order.
 * Includes the saved order and the Stripe checkout URL so the
 * customer can immediately proceed to payment.
 */
public record CreateOrderResponse(

        OrderDto order,

        @JsonProperty("paymentUrl")
        String paymentUrl,

        @JsonProperty("sessionId")
        String sessionId,

        @JsonProperty("totalAmount")
        BigDecimal totalAmount,

        @JsonProperty("message")
        String message
) {
    public static CreateOrderResponse of(Order order, String paymentUrl, String sessionId) {
        return new CreateOrderResponse(
                OrderDto.from(order),
                paymentUrl,
                sessionId,
                order.getTotalAmount(),
                "Order placed successfully. Complete your payment at the provided URL."
        );
    }

    public static CreateOrderResponse withoutPaymentUrl(Order order, String reason) {
        return new CreateOrderResponse(
                OrderDto.from(order),
                null,
                null,
                order.getTotalAmount(),
                "Order placed but payment session could not be created: " + reason
                        + ". Use POST /api/payments/{orderId}/checkout to retry."
        );
    }
}
