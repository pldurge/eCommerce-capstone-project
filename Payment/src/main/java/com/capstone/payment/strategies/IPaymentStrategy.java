package com.capstone.payment.strategies;

public interface IPaymentStrategy {
    String generatePaymentLink(String orderId,
                               String name,
                               String email,
                               String phoneNumber,
                               Long amount,
                               String gateway) throws Exception;
}
