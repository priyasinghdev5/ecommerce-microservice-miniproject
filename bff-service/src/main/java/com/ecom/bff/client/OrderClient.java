package com.ecom.bff.client;
import com.ecom.bff.dto.OrderSummaryDto;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OrderClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public Mono<List<OrderSummaryDto>> getRecentOrders(String userId, String token) {
        return webClientBuilder.baseUrl("http://order-service").build()
                .get().uri("/api/orders")
                .header("X-User-Id", userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(resp -> {
                    Object data = resp.get("data");
                    if (data == null) return Collections.<OrderSummaryDto>emptyList();
                    JavaType type = objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, OrderSummaryDto.class);
                    return (List<OrderSummaryDto>) objectMapper.convertValue(data, type);
                })
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.warn("order-service unavailable: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }
}
