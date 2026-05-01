package com.capstone.paymentservice.service;

import com.capstone.paymentservice.client.OrderServiceClient;
import com.capstone.paymentservice.exception.PaymentAlreadyProcessedException;
import com.capstone.paymentservice.exception.PaymentGatewayException;
import com.capstone.paymentservice.exception.PaymentNotFoundException;
import com.capstone.paymentservice.gateway.PaymentGatewayService;
import com.capstone.paymentservice.model.PaymentTransaction;
import com.capstone.paymentservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class PaymentService {

    private static final String PAYMENT_CONFIRMED_TOPIC = "payment-confirmed";
    private static final String PAYMENT_FAILED_TOPIC    = "payment-failed";
    private static final String NOTIFICATION_TOPIC      = "payment-notification";

    private final PaymentTransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object>   kafkaTemplate;
    private final PaymentGatewayService           paymentGatewayService;
    private final OrderServiceClient              orderServiceClient;

    @Value("${app.payment.stripe-success-url}")
    private String stripeSuccessUrl;

    @Value("${app.payment.stripe-cancel-url}")
    private String stripeCancelUrl;

    @Value("${app.payment.stripe-secret-key}")
    private String stripeSecretKey;

    // ─── Kafka: order-created → create PENDING transaction ───────────────────
    @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    public void handleOrderCreated(Map<String, Object> event) {
        try {
            String orderId   = event.get("orderId").toString();
            String userId    = event.get("userId").toString();
            BigDecimal total = new BigDecimal(event.get("totalAmount").toString());

            // Idempotency check
            if (transactionRepository.findByOrderId(orderId).isPresent()) {
                log.warn("Transaction already exists for orderId: {} — skipping", orderId);
                return;
            }

            PaymentTransaction tx = PaymentTransaction.builder()
                    .paymentId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .userId(userId)
                    .amount(total)
                    .currency("INR")
                    .status(PaymentTransaction.PaymentStatus.PENDING)
                    .build();

            transactionRepository.save(tx);
            log.info("Payment transaction created for orderId: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing order-created event", e);
        }
    }

    // ─── List pending transactions for a user ────────────────────────────────
    public List<PaymentTransaction> getPendingTransactions(String userId) {
        return transactionRepository.findByUserIdAndStatus(
                userId, PaymentTransaction.PaymentStatus.PENDING);
    }

    @Transactional
    public CheckoutResponse initiateCheckout(String orderId) {
        PaymentTransaction tx = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment transaction found for orderId: " + orderId));

        if (tx.getStatus() != PaymentTransaction.PaymentStatus.PENDING) {
            throw new PaymentAlreadyProcessedException(orderId, tx.getStatus().name());
        }

        PaymentGatewayService.CheckoutResult result =
                paymentGatewayService.createCheckoutSession(
                        tx.getAmount(), orderId, stripeSuccessUrl, stripeCancelUrl);

        tx.setSessionId(result.sessionId());
        transactionRepository.save(tx);

        log.info("Stripe Checkout Session {} created for orderId={}", result.sessionId(), orderId);
        return new CheckoutResponse(result.sessionId(), result.checkoutUrl(),
                tx.getPaymentId(), orderId, tx.getAmount());
    }

    /**
     * Internal: called by order-service immediately after order creation.
     * Creates the PENDING transaction (idempotent) AND the Stripe checkout session
     * in one atomic step so the caller gets the payment URL right away.
     */
    @Transactional
    public CheckoutResponse initiatePaymentInternal(String orderId, BigDecimal amount, String userId) {
        // Idempotency — reuse existing transaction if already created (e.g. via kafka)
        PaymentTransaction tx = transactionRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    PaymentTransaction newTx = PaymentTransaction.builder()
                            .paymentId(UUID.randomUUID().toString())
                            .orderId(orderId)
                            .userId(userId)
                            .amount(amount)
                            .currency("INR")
                            .status(PaymentTransaction.PaymentStatus.PENDING)
                            .build();
                    return transactionRepository.save(newTx);
                });

        if (tx.getStatus() != PaymentTransaction.PaymentStatus.PENDING) {
            throw new PaymentAlreadyProcessedException(orderId, tx.getStatus().name());
        }

        PaymentGatewayService.CheckoutResult result =
                paymentGatewayService.createCheckoutSession(
                        tx.getAmount(), orderId, stripeSuccessUrl, stripeCancelUrl);

        tx.setSessionId(result.sessionId());
        transactionRepository.save(tx);

        log.info("Stripe session {} created internally for orderId={}", result.sessionId(), orderId);
        return new CheckoutResponse(result.sessionId(), result.checkoutUrl(),
                tx.getPaymentId(), orderId, tx.getAmount());
    }

    // ─── Stripe Callback Handler ──────────────────────────────────────────────
    /**
     * Called after Stripe redirects back with ?session_id=cs_...
     * Verifies the session with Stripe, records the transaction as SUCCESS,
     * updates the order, and fires Kafka events.
     */
    @Transactional
    public PaymentTransaction handleStripeCallback(String stripeSessionId) {
        PaymentTransaction tx = transactionRepository.findBySessionId(stripeSessionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No transaction found for Stripe session: " + stripeSessionId));

        if (tx.getStatus() == PaymentTransaction.PaymentStatus.SUCCESS) {
            log.warn("Duplicate Stripe callback for session {} — already SUCCESS", stripeSessionId);
            return tx;
        }

        try {
            com.stripe.net.RequestOptions retrieveOptions =
                    com.stripe.net.RequestOptions.builder()
                            .setApiKey(stripeSecretKey)
                            .build();
            com.stripe.model.checkout.Session session =
                    com.stripe.model.checkout.Session.retrieve(stripeSessionId, retrieveOptions);

            if (!"paid".equals(session.getPaymentStatus())) {
                // Not paid yet (e.g. user abandoned) — mark failed
                tx.setStatus(PaymentTransaction.PaymentStatus.FAILED);
                tx.setFailureReason("Stripe session status: " + session.getPaymentStatus());
                transactionRepository.save(tx);

                kafkaTemplate.send(PAYMENT_FAILED_TOPIC, tx.getOrderId(),
                        Map.of("orderId", tx.getOrderId(), "reason", tx.getFailureReason()));
                return tx;
            }

            // Payment confirmed
            tx.setGatewayTransactionId(session.getPaymentIntent());
            tx.setStatus(PaymentTransaction.PaymentStatus.SUCCESS);
            tx.setPaymentMethod(PaymentTransaction.PaymentMethod.CREDIT_CARD); // Stripe handles method
            tx = transactionRepository.save(tx);

            // Direct fast-path: update order status immediately via HTTP
            orderServiceClient.updateOrderStatus(tx.getOrderId(), "PAYMENT_CONFIRMED");

            // Kafka: order-service also listens to confirm + set paymentId
            kafkaTemplate.send(PAYMENT_CONFIRMED_TOPIC, tx.getOrderId(),
                    Map.of("orderId",   tx.getOrderId(),
                            "paymentId", tx.getPaymentId(),
                            "userId",    tx.getUserId(),
                            "amount",    tx.getAmount()));

            // Kafka: notification-service sends payment confirmation email
            kafkaTemplate.send(NOTIFICATION_TOPIC, tx.getUserId(),
                    Map.of("userId",   tx.getUserId(),
                            "type",     "PAYMENT_SUCCESS",
                            "orderId",  tx.getOrderId(),
                            "amount",   tx.getAmount().toString()));

            log.info("Stripe payment confirmed for orderId={}, sessionId={}",
                    tx.getOrderId(), stripeSessionId);

        } catch (Exception e) {
            log.error("Error verifying Stripe session {}: {}", stripeSessionId, e.getMessage());
            tx.setStatus(PaymentTransaction.PaymentStatus.FAILED);
            tx.setFailureReason("Stripe verification error: " + e.getMessage());
            transactionRepository.save(tx);

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, tx.getOrderId(),
                    Map.of("orderId", tx.getOrderId(), "reason", e.getMessage()));
        }

        return tx;
    }

    @Transactional
    public PaymentTransaction refundPayment(String paymentId) {
        PaymentTransaction tx = getTransactionByPaymentId(paymentId);

        if (tx.getStatus() != PaymentTransaction.PaymentStatus.SUCCESS) {
            throw new PaymentGatewayException(
                    "Only successful payments can be refunded. Current status: " + tx.getStatus());
        }

        String refundId = paymentGatewayService.refund(
                tx.getGatewayTransactionId(), tx.getAmount());

        tx.setGatewayTransactionId(refundId);
        tx.setStatus(PaymentTransaction.PaymentStatus.REFUNDED);
        tx = transactionRepository.save(tx);

        kafkaTemplate.send(NOTIFICATION_TOPIC, tx.getUserId(),
                Map.of("userId",  tx.getUserId(),
                        "type",    "PAYMENT_REFUNDED",
                        "orderId", tx.getOrderId(),
                        "amount",  tx.getAmount().toString()));

        log.info("Refund processed for paymentId: {}", paymentId);
        return tx;
    }

    public PaymentTransaction getTransactionByPaymentId(String paymentId) {
        return transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    public List<PaymentTransaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    // ─── Response DTO ─────────────────────────────────────────────────────────
    public record CheckoutResponse(
            String sessionId,
            String checkoutUrl,
            String paymentId,
            String orderId,
            BigDecimal amount) {}
}