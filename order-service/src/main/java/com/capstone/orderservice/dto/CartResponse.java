package com.capstone.orderservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartResponse {
    private String id;
    private String userId;
    private List<CartItemResponse> items = new ArrayList<>();
    private BigDecimal totalPrice;
    private int totalItems;
}