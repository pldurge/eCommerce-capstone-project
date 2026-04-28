package com.capstone.productcatalog.listeners;

import com.capstone.productcatalog.services.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final IProductService productService;

    @KafkaListener(topics = "order-created", groupId = "product-catalog-group")
    public void handleOrderCreated(Map<String, Object> event) {
        String orderNumber = "UNKNOWN";
        try {
            orderNumber = event.getOrDefault("orderNumber", "UNKNOWN").toString();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) event.get("items");

            if (items == null || items.isEmpty()) {
                log.warn("order-created event for {} had no items — skipping stock reduction", orderNumber);
                return;
            }

            log.info("Reducing stock for order {} ({} line items)", orderNumber, items.size());
            boolean allOk = true;

            for (Map<String, Object> item : items) {
                UUID productId = UUID.fromString(item.get("productId").toString());
                int  quantity  = Integer.parseInt(item.get("quantity").toString());

                boolean ok = productService.reduceStock(productId, quantity);
                if (!ok) {
                    allOk = false;
                    log.warn("Stock insufficient for product={} in order={}", productId, orderNumber);
                }
            }

            if (allOk) log.info("Stock reduction complete for order {}", orderNumber);
            else       log.warn("Partial stock reduction for order {} — manual review needed", orderNumber);

        } catch (Exception e) {
            log.error("Failed to process order-created event for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }
}