package com.capstone.paymentservice.controller;

import com.capstone.paymentservice.model.PaymentTransaction;
import com.capstone.paymentservice.service.PaymentService;
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

    /**
     * List all PENDING payment transactions for the current user.
     * Use this to know which orders need payment before calling /checkout.
     */
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentTransaction>> getPendingPayments(
            @RequestHeader("X-User-Name") String userId) {
        return ResponseEntity.ok(paymentService.getPendingTransactions(userId));
    }

    /**
     * Initiate Stripe Checkout for an order.
     * Pass the orderId — returns a Stripe-hosted checkoutUrl to redirect the user to.
     * On success Stripe calls back to GET /api/payments/stripe/callback?session_id=...
     */
    @PostMapping("/{orderId}/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentService.CheckoutResponse> checkout(
            @PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.initiateCheckout(orderId));
    }

    /**
     * Stripe Checkout success/cancel callback.
     * PUBLIC endpoint — Stripe redirects here after payment attempt.
     * Verifies session with Stripe, updates transaction and order status.
     */
    @GetMapping("/stripe/callback")
    public ResponseEntity<PaymentTransaction> stripeCallback(
            @RequestParam("session_id") String sessionId) {
        return ResponseEntity.ok(paymentService.handleStripeCallback(sessionId));
    }

    /**
     * Get a single transaction. Admin sees any; customer sees only their own.
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.isOwner(#paymentId, authentication.name)")
    public ResponseEntity<PaymentTransaction> getTransaction(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getTransactionByPaymentId(paymentId));
    }

    /**
     * All transactions for the current user.
     */
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentTransaction>> getUserTransactions(
            @RequestHeader("X-User-Name") String userId) {
        return ResponseEntity.ok(paymentService.getUserTransactions(userId));
    }

    /**
     * Refund a payment. Admin only.
     */
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentTransaction> refundPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }
}