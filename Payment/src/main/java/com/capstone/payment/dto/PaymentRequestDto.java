package com.capstone.payment.dto;

import lombok.Getter;

@Getter
public class PaymentRequestDto {
    private String name;
    private String email;
    private String phoneNumber;
    private Long amount;
    private String orderId;
    private String paymentGateway;
}
