package com.capstone.paymentservice.gateway;

import com.stripe.exception.StripeException;

import java.math.BigDecimal;

public interface PaymentGatewayService {
    /*
     * Charge a customer for the given amount.
     *
     * @param amount    Amount to charge in the configured currency
     * @param token     Payment method token (e.g. Stripe card token)
     * @param orderId   Reference ID for idempotency and reconciliation
     * @return          Gateway-assigned transaction ID
     * @throws PaymentGatewayException if the charge fails
     */
    String charge(BigDecimal amount, String token, String orderId);

    /*
     * Refund a previously successful charge.
     *
     * @param gatewayTransactionId  The transaction ID returned by charge()
     * @param amount                Amount to refund (may be partial)
     * @return                      Gateway-assigned refund ID
     * @throws PaymentGatewayException if the refund fails
     */
    String refund(String gatewayTransactionId, BigDecimal amount);
}
