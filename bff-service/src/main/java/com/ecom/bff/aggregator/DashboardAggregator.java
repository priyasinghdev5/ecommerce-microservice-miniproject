package com.ecom.bff.aggregator;
import com.ecom.bff.client.*;
import com.ecom.bff.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fires 3 WebClient calls in PARALLEL via Mono.zip.
 * Total latency = max(user, orders, products) NOT sum.
 * Each call has 3s timeout + onErrorReturn fallback.
 * Result cached in Redis for 60s per user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardAggregator {

    private final UserClient userClient;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final String PREFIX = "bff:dash:";

    public Mono<DashboardViewModel> getDashboard(String userId, String token) {
        String key = PREFIX + userId;
        return redisTemplate.opsForValue().get(key)
                .flatMap(cached -> {
                    try { return Mono.just(objectMapper.readValue(cached, DashboardViewModel.class)); }
                    catch (Exception e) { return Mono.empty(); }
                })
                .switchIfEmpty(buildAndCache(userId, token, key));
    }

    private Mono<DashboardViewModel> buildAndCache(String userId, String token, String key) {
        AtomicBoolean pOk = new AtomicBoolean(true);
        AtomicBoolean oOk = new AtomicBoolean(true);
        AtomicBoolean rOk = new AtomicBoolean(true);

        return Mono.zip(
                userClient.getProfile(userId, token)
                        .doOnError(e -> pOk.set(false))
                        .onErrorReturn(UserProfileDto.empty()),
                orderClient.getRecentOrders(userId, token)
                        .doOnError(e -> oOk.set(false))
                        .onErrorReturn(Collections.emptyList()),
                productClient.getFeatured()
                        .doOnError(e -> rOk.set(false))
                        .onErrorReturn(Collections.emptyList())
        )
        .map(t -> DashboardViewModel.partial(t.getT1(), t.getT2(), t.getT3(),
                pOk.get(), oOk.get(), rOk.get()))
        .flatMap(vm -> {
            try {
                String json = objectMapper.writeValueAsString(vm);
                return redisTemplate.opsForValue().set(key, json, CACHE_TTL).thenReturn(vm);
            } catch (Exception e) {
                return Mono.just(vm);
            }
        })
        .doOnSuccess(vm -> log.info("Dashboard: userId={} profile={} orders={} recs={}",
                userId, vm.profileLoaded(), vm.ordersLoaded(), vm.recommendationsLoaded()));
    }
}
