package com.capstone.cartservice.service;

import com.capstone.cartservice.client.ProductCatalogClient;
import com.capstone.cartservice.dto.AddToCartRequest;
import com.capstone.cartservice.dto.ProductResponse;
import com.capstone.cartservice.exceptions.InsufficientStockException;
import com.capstone.cartservice.exceptions.ProductNotFoundException;
import com.capstone.cartservice.models.Cart;
import com.capstone.cartservice.models.CartItem;
import com.capstone.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final String CART_CACHE_PREFIX = "cart:";
    private static final String CART_UPDATED_TOPIC = "cart-updated";

    private final CartRepository cartRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductCatalogClient productCatalogClient;

    @Value("${app.cart.ttl-seconds:86400}")
    private long cartTtlSeconds;

    public Cart getCart(String userId){
        // Try Redis first
        String cacheKey = CART_CACHE_PREFIX + userId;
        Cart cached = (Cart) redisTemplate.opsForValue().get(cacheKey);
        if(cached != null){
            log.debug("Cart found for userId={}", userId);
            return cached;
        }

        // Fallback to MongoDB
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(()-> createEmptyCart(userId));

        cacheCart(userId, cart);
        return cart;
    }

    public Cart addItem(String userId, AddToCartRequest request){
        if(request.getQuantity() < 0) throw new IllegalArgumentException("Invalid Quantity, Quantity must be greater than zero");
        if(request.getQuantity() == 0) return removeItem(userId, request.getProductId());

        ProductResponse response = productCatalogClient.getProduct(request.getProductId());
        if(response == null) throw new ProductNotFoundException(request.getProductId());

        int available = response.getStockQuantity() == null ? 0 : response.getStockQuantity();
        if (available < request.getQuantity()) {
            throw new InsufficientStockException(response.getName(), available, request.getQuantity());
        }

        Cart cart = getCart(userId);
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst();

        if(existingItem.isPresent()){
            CartItem item = existingItem.get();
            item.setQuantity(request.getQuantity()); // absolute set — not cumulative
            item.setPrice(response.getPrice());
        }else{
            cart.getItems().add(CartItem.builder()
                    .productId(response.getId())
                    .productName(response.getName())
                    .price(response.getPrice())
                    .quantity(request.getQuantity())
                    .imageUrl(response.getImageUrl())
                    .build()
            );
        }

        cart.setUpdatedAt(new Date());
        cart.recalculate();
        cart = cartRepository.save(cart);
        cacheCart(userId, cart);

        kafkaTemplate.send(CART_UPDATED_TOPIC, userId,
                Map.of("userId", userId, "action", "UPSERT_ITEM",
                        "productId", request.getProductId(),
                        "quantity", request.getQuantity()));
        return cart;
    }

    public Cart removeItem(String userId, UUID productId){
        Cart cart = getCart(userId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        cart.setUpdatedAt(new Date());
        cart.recalculate();
        cart = cartRepository.save(cart);
        cacheCart(userId, cart);

        kafkaTemplate.send(CART_UPDATED_TOPIC, userId,
                Map.of("userId", userId, "action", "REMOVE_ITEM", "productId", productId));
        return cart;
    }

    public void clearCart(String userId){
        cartRepository.deleteByUserId(userId);
        redisTemplate.delete(CART_CACHE_PREFIX + userId);
        log.info("Cart cleared for user: {}", userId);
    }

    private Cart createEmptyCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        return cartRepository.save(cart);
    }

    private void cacheCart(String userId, Cart cart) {
        redisTemplate.opsForValue().set(CART_CACHE_PREFIX + userId, cart, cartTtlSeconds, TimeUnit.SECONDS);
    }
}