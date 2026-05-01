package com.capstone.orderservice.config;

import com.capstone.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurity {

    private final OrderRepository orderRepository;

    public boolean isOwner(UUID orderId, String authenticatedUserId) {
        return orderRepository.findById(orderId)
                .map(o -> o.getUserId().equals(authenticatedUserId))
                .orElse(false);
    }

    public boolean isOwnerByNumber(String orderNumber, String authenticatedUserId) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(o -> o.getUserId().equals(authenticatedUserId))
                .orElse(false);
    }
}