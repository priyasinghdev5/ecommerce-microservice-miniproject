package com.ecom.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Resolves the rate limit key per request.
 * Uses X-User-Id header (set by JwtAuthFilter) for authenticated users.
 * Falls back to IP address for unauthenticated requests (e.g. /api/auth/login).
 *
 * Referenced in application.yml as: key-resolver: "#{@userKeyResolver}"
 */
@Component
public class UserKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

        if (userId != null && !userId.isBlank()) {
            return Mono.just("user:" + userId);
        }

        // Fallback to IP for public endpoints
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        return Mono.just("ip:" + ip);
    }
}
