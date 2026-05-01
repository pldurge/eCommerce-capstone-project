package com.capstone.cartservice.exceptions;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super("Product not found with ID: " + productId);
    }
    public ProductNotFoundException(String message) {
        super(message);
    }
}
