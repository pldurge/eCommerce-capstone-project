package com.capstone.payment.configs;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorPayClient {

    @Value("${razorpay.keyId}")
    private String razorPayKeyId;

    @Value("${razorpay.keySecret}")
    private String razorPayKeySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(razorPayKeyId, razorPayKeySecret);
    }
}
