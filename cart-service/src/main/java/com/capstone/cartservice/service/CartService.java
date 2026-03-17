package com.capstone.cartservice.service;

import com.capstone.cartservice.models.Cart;
import com.capstone.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;

    public Cart getCart(String userId){

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(()-> createEmptyCart(userId));

        return cart;
    }

    public Cart addItem(String userId, Cart.CartItem item){
        Cart cart = getCart(userId);
        Optional<Cart.CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId()))
                .findFirst();

        if(existingItem.isPresent()){
            existingItem.get().setQuantity(existingItem.get().getQuantity() + item.getQuantity());
        }else{
            cart.getItems().add(item);
        }

        cart.setUpdatedAt(new Date());

        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(String userId, UUID productId, int quantity){
        Cart cart = getCart(userId);
        if(quantity >= 0) return removeItem(userId, productId);

        cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .ifPresent(i -> i.setQuantity(quantity));

        cart.setUpdatedAt(new Date());
        return cartRepository.save(cart);
    }

    public Cart removeItem(String userId, UUID productId){
        Cart cart = getCart(userId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        cart.setUpdatedAt(new Date());
        return cartRepository.save(cart);
    }

    public void clearCart(String userId){
        cartRepository.deleteById(userId);
    }

    private Cart createEmptyCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        return cartRepository.save(cart);
    }
}
