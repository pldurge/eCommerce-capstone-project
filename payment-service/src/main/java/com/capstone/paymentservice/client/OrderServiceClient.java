package com.capstone.paymentservice.client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.order-service.url:http://localhost:8084}")
    private String orderServiceUrl;

    /**
     * Calls the internal PATCH endpoint on order-service to update order status.
     * Not exposed via the gateway — internal service-to-service call only.
     *
     * @param orderId   UUID of the order (String form, as stored in PaymentTransaction)
     * @param status    New status string, e.g. "PAYMENT_CONFIRMED", "CANCELLED"
     */
    public void updateOrderStatus(String orderId, String status) {
        try {
            String url = orderServiceUrl
                    + "/api/orders/internal/" + orderId
                    + "/status?status=" + status;
            restTemplate.patchForObject(url, null, Void.class);
            log.info("Order {} status updated to {} via internal call", orderId, status);
        } catch (Exception e) {
            log.error("Failed to update order {} to status {}: {}", orderId, status, e.getMessage());
            // Don't re-throw — the Kafka event already handles the order-service side.
            // This call is a direct fast-path update; Kafka is the fallback.
        }
    }
}
