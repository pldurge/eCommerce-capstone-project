package com.capstone.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request body for POST /api/payments/internal/initiate
 * Called by order-service to atomically create the PENDING transaction
 * and a Stripe Checkout session in one HTTP call.
 */
public record InitiatePaymentRequest(

        @NotBlank(message = "orderId is required")
        String orderId,

        @NotBlank(message = "userId is required")
        String userId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount
) {}
