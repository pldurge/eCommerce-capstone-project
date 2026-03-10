package com.capstone.payment.service;

import com.capstone.payment.strategies.IPaymentStrategy;
import org.springframework.stereotype.Service;

@Service
public class PaymentService implements IPaymentService {

    private final IPaymentStrategy strategy;

    public PaymentService(IPaymentStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public String generatePaymentLink(String orderId, String name, String email, String phoneNumber, Long amount, String gateway) throws Exception {
        return strategy.generatePaymentLink(orderId, name, email, phoneNumber, amount, gateway);
    }
}
