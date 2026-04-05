package com.capstone.orderservice.client;

import com.capstone.orderservice.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogClient {

    private final RestTemplate restTemplate;

    @Value("${app.product-service.url:http://localhost:8082}")
    private String productServiceUrl;

    public ProductResponse getProduct(UUID productId) {
        try {
            String url = productServiceUrl + "/api/products/" + productId;
            return restTemplate.getForObject(url, ProductResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch product {} for stock check: {}", productId, e.getMessage());
            return null;
        }
    }
}
