package com.capstone.paymentservice.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String identifier) {
        super("Payment transaction not found: " + identifier);
    }
}
