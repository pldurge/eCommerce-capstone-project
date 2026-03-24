package com.capstone.orderservice.service;

import com.capstone.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/*
 * Spring bean used inside @PreAuthorize SpEL expressions to check ownership.
 *
 * Usage in controller:
 *   @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#id, authentication.name)")
 *
 * "authentication.name" is the email injected by the API Gateway as X-User-Name,
 * which SecurityConfig maps to the Spring principal name.
 */
@Service("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurityService {

    private final OrderRepository orderRepository;

    /*
     * Returns true if the order with the given id belongs to the requesting user.
     *
     * @param orderId   The order ID from the path variable
     * @param username  The authenticated user's email (from JWT / X-User-Name header)
     */
    public boolean isOwner(UUID orderId, String username) {
        return orderRepository.findById(orderId)
                .map(order -> order.getUserId().equals(username))
                .orElse(false);
    }

    /*
     * Returns true if the order with the given orderNumber belongs to the requesting user.
     */
    public boolean isOwnerByNumber(String orderNumber, String username) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(order -> order.getUserId().equals(username))
                .orElse(false);
    }
}
