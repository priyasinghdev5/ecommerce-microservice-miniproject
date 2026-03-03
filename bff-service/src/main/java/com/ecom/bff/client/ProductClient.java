package com.ecom.bff.client;
import com.ecom.bff.dto.ProductSummaryDto;
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
public class ProductClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public Mono<List<ProductSummaryDto>> getFeatured() {
        return webClientBuilder.baseUrl("http://product-service").build()
                .get().uri("/api/products/featured")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(resp -> extractList(resp, ProductSummaryDto.class))
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.warn("product-service unavailable: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    public Mono<List<String>> getCategories() {
        return Mono.just(List.of("footwear","clothing","electronics",
                "wearables","sports","books","home"));
    }

    private <T> List<T> extractList(Map<String, Object> resp, Class<T> type) {
        Object data = resp.get("data");
        if (data == null) return Collections.emptyList();
        JavaType jt = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
        return objectMapper.convertValue(data, jt);
    }
}
