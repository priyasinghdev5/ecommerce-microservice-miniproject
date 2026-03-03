package com.ecom.bff.controller;
import com.ecom.bff.aggregator.*;
import com.ecom.bff.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/bff")
@RequiredArgsConstructor
public class BffController {

    private final HomeAggregator homeAggregator;
    private final DashboardAggregator dashboardAggregator;
    private final CheckoutAggregator checkoutAggregator;

    /** Public — no auth required. Returns featured products + categories + banners. */
    @GetMapping("/home")
    public Mono<ResponseEntity<HomeViewModel>> getHome() {
        return homeAggregator.getHome().map(ResponseEntity::ok);
    }

    /** Authenticated. Returns profile + recent orders + recommendations. Cached 60s per user. */
    @GetMapping("/dashboard")
    public Mono<ResponseEntity<DashboardViewModel>> getDashboard(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
        return dashboardAggregator.getDashboard(userId, token).map(ResponseEntity::ok);
    }

    /** Authenticated. Returns cart + profile + addresses. NOT cached. */
    @GetMapping("/checkout")
    public Mono<ResponseEntity<CheckoutViewModel>> getCheckout(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
        return checkoutAggregator.getCheckout(userId, token).map(ResponseEntity::ok);
    }
}
