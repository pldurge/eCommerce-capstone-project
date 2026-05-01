package com.capstone.orderservice.exceptions;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) {
        super("Order not found with ID: " + id);
    }
    public OrderNotFoundException(String orderNumber) {
        super("Order not found with number: " + orderNumber);
    }
}