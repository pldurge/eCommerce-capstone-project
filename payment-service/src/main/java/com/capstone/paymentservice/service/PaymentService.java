package com.capstone.paymentservice.service;

import com.capstone.paymentservice.exception.PaymentGatewayException;
import com.capstone.paymentservice.gateway.PaymentGatewayService;
import com.capstone.paymentservice.model.PaymentTransaction;
import com.capstone.paymentservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final PaymentGatewayService paymentGatewayService;

    // ─── Kafka Listener: order-created → create pending transaction ──────────
    @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    public void handleOrderCreated(Map<String, Object> event) {
        try {
            String orderId        = (event.get("orderId").toString());
            String userId       = event.get("userId").toString();
            BigDecimal total    = new BigDecimal(event.get("totalAmount").toString());

            // Check idempotency — don't create duplicate transactions
            if (transactionRepository.findByOrderId(orderId).isPresent()) {
                log.warn("Transaction already exists for orderId: {} — skipping", orderId);
                return;
            }

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .paymentId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .userId(userId)
                    .amount(total)
                    .currency("USD")
                    .status(PaymentTransaction.PaymentStatus.PENDING)
                    .build();

            transactionRepository.save(transaction);
            log.info("Payment transaction created for orderId: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing order-created event", e);
        }
    }

    // ─── Process Payment ──────────────────────────────────────────────────────

    @Transactional
    public PaymentTransaction processPayment(String paymentId,
                                             PaymentTransaction.PaymentMethod method,
                                             String cardToken) {
        PaymentTransaction transaction = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + paymentId));

        if (transaction.getStatus() != PaymentTransaction.PaymentStatus.PENDING) {
            throw new RuntimeException("Transaction " + paymentId + " is not in PENDING state");
        }

        transaction.setStatus(PaymentTransaction.PaymentStatus.PROCESSING);
        transaction.setPaymentMethod(method);
        transactionRepository.save(transaction);

        try {
            // FIX 4: Delegate to PaymentGatewayService (Stripe-ready)
            String gatewayTxId = paymentGatewayService.charge(
                    transaction.getAmount(), cardToken, transaction.getOrderId()
            );

            transaction.setGatewayTransactionId(gatewayTxId);
            transaction.setStatus(PaymentTransaction.PaymentStatus.SUCCESS);
            transaction = transactionRepository.save(transaction);

            kafkaTemplate.send(PAYMENT_CONFIRMED_TOPIC, transaction.getOrderId(),
                    Map.of("orderId", transaction.getOrderId(),
                            "paymentId", paymentId,
                            "userId", transaction.getUserId(),
                            "amount", transaction.getAmount()));

            kafkaTemplate.send(NOTIFICATION_TOPIC, transaction.getUserId(),
                    Map.of("userId", transaction.getUserId(),
                            "type", "PAYMENT_SUCCESS",
                            "orderId", transaction.getOrderId(),
                            "amount", transaction.getAmount()));

            log.info("Payment successful for orderId: {}", transaction.getOrderId());

        } catch (PaymentGatewayException e) {
            transaction.setStatus(PaymentTransaction.PaymentStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, transaction.getOrderId().toString(),
                    Map.of("orderId", transaction.getOrderId(), "reason", e.getMessage()));

            log.error("Payment failed for orderId: {} — {}", transaction.getOrderId(), e.getMessage());
        }

        return transaction;
    }

    // ─── Refund ───────────────────────────────────────────────────────────────

    @Transactional
    public PaymentTransaction refundPayment(String paymentId) {
        PaymentTransaction transaction = getTransactionByPaymentId(paymentId);

        if (transaction.getStatus() != PaymentTransaction.PaymentStatus.SUCCESS) {
            throw new RuntimeException("Only successful payments can be refunded");
        }

        // FIX 4: Delegate refund to gateway
        String refundId = paymentGatewayService.refund(
                transaction.getGatewayTransactionId(), transaction.getAmount()
        );

        transaction.setGatewayTransactionId(refundId);
        transaction.setStatus(PaymentTransaction.PaymentStatus.REFUNDED);
        transaction = transactionRepository.save(transaction);

        kafkaTemplate.send(NOTIFICATION_TOPIC, transaction.getUserId(),
                Map.of("userId", transaction.getUserId(),
                        "type", "PAYMENT_REFUNDED",
                        "orderId", transaction.getOrderId(),
                        "amount", transaction.getAmount()));

        log.info("Refund processed for paymentId: {}", paymentId);
        return transaction;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public PaymentTransaction getTransactionByPaymentId(String paymentId) {
        return transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + paymentId));
    }

    public List<PaymentTransaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserId(userId);
    }
}
