package com.ecom.cart.handler;

import com.ecom.cart.dto.AddItemRequest;
import com.ecom.cart.model.CartItem;
import com.ecom.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * WebFlux functional handler — processes requests from RouterConfig.
 * Reads X-User-Id header set by api-gateway after JWT validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartHandler {

    private final CartService cartService;

    public Mono<ServerResponse> addItem(ServerRequest request) {
        String userId = getUserId(request);
        return request.bodyToMono(AddItemRequest.class)
                .flatMap(req -> {
                    CartItem item = CartItem.builder()
                            .productId(req.productId())
                            .sku(req.sku())
                            .name(req.name())
                            .quantity(req.quantity())
                            .unitPrice(req.unitPrice())
                            .imageUrl(req.imageUrl())
                            .build();
                    return cartService.addItem(userId, item);
                })
                .then(ServerResponse.status(HttpStatus.NO_CONTENT).build())
                .onErrorResume(e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> getCart(ServerRequest request) {
        String userId = getUserId(request);
        return cartService.getItems(userId)
                .collectList()
                .flatMap(items -> ServerResponse.ok().bodyValue(items));
    }

    public Mono<ServerResponse> getCartSummary(ServerRequest request) {
        String userId = getUserId(request);
        return cartService.getCartSummary(userId)
                .flatMap(summary -> ServerResponse.ok().bodyValue(summary));
    }

    public Mono<ServerResponse> removeItem(ServerRequest request) {
        String userId = getUserId(request);
        String productId = request.pathVariable("productId");
        return cartService.removeItem(userId, productId)
                .then(ServerResponse.status(HttpStatus.NO_CONTENT).build());
    }

    public Mono<ServerResponse> clearCart(ServerRequest request) {
        String userId = getUserId(request);
        return cartService.clearCart(userId)
                .then(ServerResponse.status(HttpStatus.NO_CONTENT).build());
    }

    private String getUserId(ServerRequest request) {
        return request.headers().firstHeader("X-User-Id");
    }
}
