package com.capstone.cartservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AddToCartRequest {
    private UUID productId;
    private int quantity;
}
