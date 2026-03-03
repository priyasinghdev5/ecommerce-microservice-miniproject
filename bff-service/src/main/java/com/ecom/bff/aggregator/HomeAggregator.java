package com.ecom.bff.aggregator;
import com.ecom.bff.client.ProductClient;
import com.ecom.bff.dto.HomeViewModel;
import com.ecom.bff.dto.HomeViewModel.BannerDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.*;

/**
 * Home page aggregator — not user-specific, cached globally (TTL 120s).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HomeAggregator {

    private final ProductClient productClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY = "bff:home";
    private static final Duration TTL = Duration.ofSeconds(120);

    public Mono<HomeViewModel> getHome() {
        return redisTemplate.opsForValue().get(KEY)
                .flatMap(cached -> {
                    try { return Mono.just(objectMapper.readValue(cached, HomeViewModel.class)); }
                    catch (Exception e) { return Mono.empty(); }
                })
                .switchIfEmpty(buildAndCache());
    }

    private Mono<HomeViewModel> buildAndCache() {
        return Mono.zip(
                productClient.getFeatured().onErrorReturn(Collections.emptyList()),
                productClient.getCategories().onErrorReturn(List.of()),
                Mono.just(defaultBanners())
        )
        .map(t -> new HomeViewModel(t.getT1(), t.getT2(), t.getT3(), false))
        .flatMap(vm -> {
            try {
                String json = objectMapper.writeValueAsString(vm);
                return redisTemplate.opsForValue().set(KEY, json, TTL).thenReturn(vm);
            } catch (Exception e) { return Mono.just(vm); }
        });
    }

    private List<BannerDto> defaultBanners() {
        return List.of(
            new BannerDto("Summer Sale", "Up to 50% off", "/banners/summer.jpg", "/products"),
            new BannerDto("New Arrivals", "Latest electronics", "/banners/electronics.jpg", "/products/category/electronics"),
            new BannerDto("Free Shipping", "On orders above INR 999", "/banners/ship.jpg", "/products")
        );
    }
}
