package com.capstone.paymentservice.controller;

import com.capstone.paymentservice.model.PaymentTransaction;
import com.capstone.paymentservice.service.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /*
     * Process a payment for a pending transaction.
     * Any authenticated user can process their own payment.
     */
    @PostMapping("/{paymentId}/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentTransaction> processPayment(
            @PathVariable String paymentId,
            @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(
                paymentId, request.getPaymentMethod(), request.getCardToken()));
    }

    /**
     * Get a single transaction by paymentId.
     * Admin can see any; customer sees only their own.
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.isOwner(#paymentId, authentication.name)")
    public ResponseEntity<PaymentTransaction> getTransaction(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getTransactionByPaymentId(paymentId));
    }

    /**
     * Get the current user's transaction history.
     */
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentTransaction>> getUserTransactions(
            @RequestHeader("X-User-Name") String userId) {
        return ResponseEntity.ok(paymentService.getUserTransactions(userId));
    }

    /*
     * Refund a payment. Admin only — customers cannot self-issue refunds.
     * Gateway RoleFilter enforces this at the network level;
     *
     * @PreAuthorize enforces it as a defence-in-depth layer.
     */
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentTransaction> refundPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }

    @Data
    public static class ProcessPaymentRequest {
        private PaymentTransaction.PaymentMethod paymentMethod;
        private String cardToken;
    }
}