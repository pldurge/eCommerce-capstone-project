package com.capstone.orderservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CartItemResponse {
    private UUID productId;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private String imageUrl;
}
