package com.capstone.paymentservice.gateway;

import com.stripe.exception.StripeException;

import java.math.BigDecimal;

public interface PaymentGatewayService {
    /**
     * Charge via raw token (legacy / direct).
     * Returns gateway transaction ID, or throws PaymentGatewayException on failure.
     */
    String charge(BigDecimal amount, String token, String orderId);

    /**
     * Creates a Stripe Checkout Session and returns the hosted payment URL.
     * The user is redirected to this URL to complete payment.
     *
     * @param amount     Amount to charge
     * @param orderId    Used as metadata and idempotency key
     * @param successUrl Where Stripe redirects after successful payment (includes ?session_id={CHECKOUT_SESSION_ID})
     * @param cancelUrl  Where Stripe redirects if the user cancels
     * @return           CheckoutResult containing the sessionId and the redirect URL
     */
    CheckoutResult createCheckoutSession(BigDecimal amount, String orderId,
                                         String successUrl, String cancelUrl);

    /**
     * Refund a previously successful charge.
     */
    String refund(String gatewayTransactionId, BigDecimal amount);

    record CheckoutResult(String sessionId, String checkoutUrl) {}
}
