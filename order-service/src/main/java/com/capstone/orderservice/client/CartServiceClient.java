package com.capstone.orderservice.client;

import com.capstone.orderservice.dto.CartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.cart-service.url:http://localhost:8083}")
    private String cartServiceUrl;

    /**
     * Fetch the cart for a given user.
     * Passes X-User-Name header so the cart-service trusts the identity
     * (same pattern the gateway uses — no JWT needed for internal calls).
     */
    public CartResponse getCart(String userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Name", userId);
            headers.set("X-User-Role", "ROLE_CUSTOMER");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(
                    cartServiceUrl + "/api/cart",
                    HttpMethod.GET,
                    entity,
                    CartResponse.class
            ).getBody();
        } catch (Exception e) {
            log.error("Failed to fetch cart for userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("Unable to retrieve cart: " + e.getMessage());
        }
    }

    /**
     * Clears the cart after a successful order is placed.
     */
    public void clearCart(String userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Name", userId);
            headers.set("X-User-Role", "ROLE_CUSTOMER");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    cartServiceUrl + "/api/cart",
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
            log.info("Cart cleared for userId={} after order placement", userId);
        } catch (Exception e) {
            log.warn("Failed to clear cart for userId={} — order was still created: {}", userId, e.getMessage());
        }
    }
}
