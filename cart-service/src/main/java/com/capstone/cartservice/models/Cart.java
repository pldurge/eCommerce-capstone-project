package com.capstone.cartservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cart {
    @Id
    private String id;
    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private Date createdAt;
    private Date updatedAt;

    // ← ADD THESE TWO FIELDS
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Builder.Default
    private int totalItems = 0;

    public void recalculate() {
        if (items == null || items.isEmpty()) {
            this.totalPrice = BigDecimal.ZERO;
            this.totalItems = 0;
            return;
        }
        this.totalPrice = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}