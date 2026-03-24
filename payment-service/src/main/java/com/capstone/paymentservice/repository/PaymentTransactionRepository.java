package com.capstone.paymentservice.repository;

import com.capstone.paymentservice.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByPaymentId(String paymentId);
    List<PaymentTransaction> findByUserId(String userId);
    Optional<PaymentTransaction> findByOrderId(String orderId);
}
