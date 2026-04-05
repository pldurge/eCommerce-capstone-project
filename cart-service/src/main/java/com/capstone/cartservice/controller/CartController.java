package com.capstone.cartservice.controller;

import com.capstone.cartservice.dto.AddToCartRequest;
import com.capstone.cartservice.models.Cart;
import com.capstone.cartservice.models.CartItem;
import com.capstone.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    /*
     * All cart endpoints require authentication (enforced at gateway AND here).
     * The userId is ALWAYS taken from the trusted X-User-Name header — never from
     * a request body — so a user can only access their own cart.
     */

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cart> getCart(@RequestHeader("X-User-Name") String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cart> addItem(
            @RequestHeader("X-User-Name") String userId,
            @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }
    

    @DeleteMapping("/items/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cart> removeItem(
            @RequestHeader("X-User-Name") String userId,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Name") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
