package com.capstone.payment.controller;

import com.capstone.payment.dto.PaymentRequestDto;
import com.capstone.payment.service.IPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final IPaymentService paymentService;

    public PaymentController(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<String> generatePaymentLink(@RequestBody PaymentRequestDto requestDto){
        try{
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(paymentService.generatePaymentLink(
                    requestDto.getOrderId(),
                    requestDto.getName(),
                    requestDto.getEmail(),
                    requestDto.getPhoneNumber(),
                    requestDto.getAmount(),
                    requestDto.getPaymentGateway()
            ));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
