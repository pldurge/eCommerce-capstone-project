package com.capstone.payment.service;

public interface IPaymentService {
    String  generatePaymentLink(String orderId, String name, String email, String phoneNumber, Long amount, String gateway) throws Exception;
}
