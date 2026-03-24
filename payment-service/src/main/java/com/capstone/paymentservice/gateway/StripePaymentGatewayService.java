package com.capstone.paymentservice.gateway;

import com.capstone.paymentservice.exception.PaymentGatewayException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripePaymentGatewayService implements PaymentGatewayService {

    @Value("${app.payment.stripe-secret-key}")
    private String stripeSecretKey;

    @Override
    public String charge(BigDecimal amount, String token, String orderId) {
        log.info("Processing payment via Stripe for orderId={}, amount={}", orderId, amount);

        // Validate inputs
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentGatewayException("Invalid payment amount: " + amount);
        }
        if (token == null || token.isBlank()) {
            throw new PaymentGatewayException("Payment token is required");
        }

        if ("fail_test_token".equals(token)) {
            throw new PaymentGatewayException("Card declined (test simulation)");
        }

        try {
            //  Convert amount safely (INR → paise)
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            //  Build PaymentIntent params
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPaise)
                    .setCurrency("inr")
                    .setPaymentMethod(token)
                    .setConfirm(true)
                    .putMetadata("orderId", orderId) //
                    .build();

            //  Request options (idempotency + API key)
            RequestOptions options = RequestOptions.builder()
                    .setApiKey(stripeSecretKey)
                    .setIdempotencyKey(orderId)
                    .build();

            //  Call Stripe
            PaymentIntent intent = PaymentIntent.create(params, options);
            log.info("PaymentIntent created for orderId={} status={}", orderId, intent.getStatus());
            return intent.getId();
        }catch (StripeException e) {
            log.error("Stripe error for orderId={} amount={} : {}",
                    orderId, amount, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String refund(String gatewayTransactionId, BigDecimal amount) {
        return "";
    }
}
