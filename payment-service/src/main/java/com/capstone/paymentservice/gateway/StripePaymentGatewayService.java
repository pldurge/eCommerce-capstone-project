package com.capstone.paymentservice.gateway;

import com.capstone.paymentservice.exception.PaymentGatewayException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripePaymentGatewayService implements PaymentGatewayService {

    @Value("${app.payment.stripe-secret-key}")
    private String stripeSecretKey;

    // ─── Direct charge (kept for legacy / test use) ───────────────────────────
    @Override
    public String charge(BigDecimal amount, String token, String orderId) {
        log.info("Direct charge via Stripe for orderId={}, amount={}", orderId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new PaymentGatewayException("Invalid payment amount: " + amount);
        if (token == null || token.isBlank())
            throw new PaymentGatewayException("Payment token is required");
        if ("fail_test_token".equals(token))
            throw new PaymentGatewayException("Card declined (test simulation)");

        try {
            long paise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(paise)
                    .setCurrency("inr")
                    .setPaymentMethod(token)
                    .setConfirm(true)
                    .putMetadata("orderId", orderId)
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(stripeSecretKey)
                    .setIdempotencyKey("charge-" + orderId)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, options);
            log.info("PaymentIntent created for orderId={} status={}", orderId, intent.getStatus());
            return intent.getId();
        } catch (StripeException e) {
            log.error("Stripe error for orderId={}: {}", orderId, e.getMessage());
            throw new PaymentGatewayException("Stripe charge failed: " + e.getMessage());
        }
    }

    // ─── Stripe Checkout Session (hosted payment page) ────────────────────────
    @Override
    public CheckoutResult createCheckoutSession(BigDecimal amount, String orderId,
                                                String successUrl, String cancelUrl) {
        log.info("Creating Stripe Checkout Session for orderId={}, amount={}", orderId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new PaymentGatewayException("Invalid payment amount: " + amount);

        try {
            long paise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    // Stripe appends ?session_id={CHECKOUT_SESSION_ID} automatically
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount(paise)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Order #" + orderId)
                                                                    .build())
                                                    .build())
                                    .build())
                    .putMetadata("orderId", orderId)
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(stripeSecretKey)
                    .setIdempotencyKey("session-" + orderId)
                    .build();

            Session session = Session.create(params, options);
            log.info("Stripe Checkout Session created: {} for orderId={}", session.getId(), orderId);
            return new CheckoutResult(session.getId(), session.getUrl());

        } catch (StripeException e) {
            log.error("Stripe session error for orderId={}: {}", orderId, e.getMessage());
            throw new PaymentGatewayException("Failed to create Stripe Checkout Session: " + e.getMessage());
        }
    }

    // ─── Refund ───────────────────────────────────────────────────────────────
    @Override
    public String refund(String gatewayTransactionId, BigDecimal amount) {
        log.info("Refunding Stripe PaymentIntent={}, amount={}", gatewayTransactionId, amount);
        try {
            long paise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(gatewayTransactionId)
                    .setAmount(paise)
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(stripeSecretKey)
                    .build();

            Refund refund = Refund.create(params, options);
            log.info("Refund created: {}", refund.getId());
            return refund.getId();
        } catch (StripeException e) {
            log.error("Stripe refund error: {}", e.getMessage());
            throw new PaymentGatewayException("Stripe refund failed: " + e.getMessage());
        }
    }
}