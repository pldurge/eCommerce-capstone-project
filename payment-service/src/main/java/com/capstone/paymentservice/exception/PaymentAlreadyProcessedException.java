package com.capstone.paymentservice.exception;

public class PaymentAlreadyProcessedException extends RuntimeException {
    public PaymentAlreadyProcessedException(String orderId, String status) {
        super("Payment for order " + orderId + " has already been processed with status: " + status);
    }
}
