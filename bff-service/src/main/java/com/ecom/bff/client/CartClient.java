package com.ecom.bff.client;
import com.ecom.bff.dto.CartItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartClient {

    private final WebClient.Builder webClientBuilder;

    public Mono<List<CartItemDto>> getCart(String userId, String token) {
        return webClientBuilder.baseUrl("http://cart-service").build()
                .get().uri("/api/cart")
                .header("X-User-Id", userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CartItemDto>>() {})
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.warn("cart-service unavailable: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }
}
