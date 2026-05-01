package com.capstone.orderservice.client;

import com.capstone.orderservice.exceptions.PaymentServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.payment-service.url}")
    private String paymentServiceUrl;

    public CheckoutResult initiatePayment(String orderId, BigDecimal amount, String userId) {
        String url = paymentServiceUrl +  "/api/payments/internal/initiate";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "amount", amount,
                "userId", userId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try{
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<?, ?> responseBody = response.getBody();
            if(responseBody == null) throw new PaymentServiceException("Empty Response From Payment Service");

            String sessionId = responseBody.get("sessionId").toString();
            String checkoutUrl = responseBody.get("checkoutUrl").toString();
            log.info("Payment session created for orderId={}, sessionId={}", orderId, sessionId);
            return new CheckoutResult(sessionId, checkoutUrl);
        }catch (HttpClientErrorException ex) {
            log.error("Payment service client error for orderId={}: {}", orderId, ex.getMessage());
            throw new PaymentServiceException(
                    "Payment service rejected the request: " + ex.getResponseBodyAsString());
        } catch (HttpServerErrorException ex) {
            log.error("Payment service server error for orderId={}: {}", orderId, ex.getMessage());
            throw new PaymentServiceException("Payment service is currently unavailable. Please retry.");
        } catch (ResourceAccessException ex) {
            log.error("Cannot reach payment service for orderId={}: {}", orderId, ex.getMessage());
            throw new PaymentServiceException("Cannot connect to payment service. Please retry later.");
        } catch (Exception ex) {
            log.error("Unexpected error calling payment service for orderId={}: {}", orderId, ex.getMessage());
            throw new PaymentServiceException("Failed to initiate payment: " + ex.getMessage());
        }
    }

    public record CheckoutResult(String sessionId, String checkoutUrl) {}
}
