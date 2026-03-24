package com.capstone.paymentservice.security;

import com.capstone.paymentservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurityService {

    private final PaymentTransactionRepository transactionRepository;

    /*
     * Returns true if the payment transaction belongs to the requesting user.
     *
     * @param paymentId  UUID of the payment transaction
     * @param username   Authenticated user's email
     */
    public boolean isOwner(String paymentId, String username) {
        return transactionRepository.findByPaymentId(paymentId)
                .map(tx -> tx.getUserId().equals(username))
                .orElse(false);
    }
}
