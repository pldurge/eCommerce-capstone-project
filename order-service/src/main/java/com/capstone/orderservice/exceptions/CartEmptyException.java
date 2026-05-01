package com.capstone.orderservice.exceptions;

public class CartEmptyException extends RuntimeException {
    public CartEmptyException() {
        super("Cart is empty. Please add items before placing an order.");
    }
    public CartEmptyException(String message) {
        super(message);
    }
}
