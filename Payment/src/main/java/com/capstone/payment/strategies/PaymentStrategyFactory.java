package com.capstone.payment.strategies;

import com.capstone.payment.dto.PaymentGatewayType;

public class PaymentStrategyFactory {
    public IPaymentStrategy getPaymentStrategy(String paymentStrategy) {
        PaymentGatewayType paymentGateway = PaymentGatewayType.valueOf(paymentStrategy.toUpperCase());

    }
}
