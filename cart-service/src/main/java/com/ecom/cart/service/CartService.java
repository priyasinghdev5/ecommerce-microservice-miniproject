package com.ecom.cart.service;

import com.ecom.cart.dto.CartSummary;
import com.ecom.cart.model.CartItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Fully reactive cart service.
 * All methods return Mono/Flux — never block.
 * Redis is the ONLY data store — no SQL, no JPA.
 *
 * Redis key pattern:  cart:{userId}
 * Redis type:         Hash  (field=productId, value=CartItem JSON)
 * TTL:                1 hour, sliding (reset on every addItem)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final ReactiveRedisTemplate<String, CartItem> redisTemplate;

    private static final String CART_PREFIX = "cart:";
    private static final Duration CART_TTL  = Duration.ofHours(1);

    private ReactiveHashOperations<String, String, CartItem> hashOps() {
        return redisTemplate.opsForHash();
    }

    private String cartKey(String userId) {
        return CART_PREFIX + userId;
    }

    // ── Add or update item ─────────────────────────────────────────

    public Mono<Void> addItem(String userId, CartItem item) {
        String key = cartKey(userId);
        item.setAddedAt(Instant.now());

        return hashOps().hasKey(key, item.getProductId())
                .flatMap(exists -> {
                    if (exists) {
                        // Update quantity if item already in cart
                        return hashOps().get(key, item.getProductId())
                                .flatMap(existing -> {
                                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                                    return hashOps().put(key, item.getProductId(), existing);
                                });
                    }
                    return hashOps().put(key, item.getProductId(), item);
                })
                .then(redisTemplate.expire(key, CART_TTL))   // sliding TTL
                .then()
                .doOnSuccess(v -> log.debug("Item added to cart: userId={} sku={}", userId, item.getSku()))
                .onErrorResume(e -> {
                    log.error("Failed to add item to cart: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Cart service unavailable"));
                });
    }

    // ── Get all items ──────────────────────────────────────────────

    public Flux<CartItem> getItems(String userId) {
        return hashOps().values(cartKey(userId))
                .onErrorResume(e -> {
                    log.error("Failed to get cart: {}", e.getMessage());
                    return Flux.empty();   // Return empty cart on Redis failure
                });
    }

    // ── Get cart summary with totals ───────────────────────────────

    public Mono<CartSummary> getCartSummary(String userId) {
        return getItems(userId)
                .collectList()
                .map(items -> {
                    int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
                    BigDecimal totalAmount = items.stream()
                            .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CartSummary(userId, items, totalItems, totalAmount);
                });
    }

    // ── Remove one item ────────────────────────────────────────────

    public Mono<Void> removeItem(String userId, String productId) {
        String key = cartKey(userId);
        return hashOps().remove(key, productId)
                .flatMap(removed -> hashOps().size(key))
                .flatMap(size -> size == 0
                        ? redisTemplate.delete(key).then()
                        : Mono.empty())
                .doOnSuccess(v -> log.debug("Item removed: userId={} productId={}", userId, productId));
    }

    // ── Clear entire cart ──────────────────────────────────────────

    public Mono<Void> clearCart(String userId) {
        return redisTemplate.delete(cartKey(userId))
                .then()
                .doOnSuccess(v -> log.debug("Cart cleared: userId={}", userId));
    }
}
