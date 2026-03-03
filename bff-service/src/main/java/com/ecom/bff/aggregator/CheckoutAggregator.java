package com.ecom.bff.aggregator;
import com.ecom.bff.client.CartClient;
import com.ecom.bff.client.UserClient;
import com.ecom.bff.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
 * Checkout aggregator — NOT cached, always fresh.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutAggregator {

    private final CartClient cartClient;
    private final UserClient userClient;
    private final WebClient.Builder webClientBuilder;

    public Mono<CheckoutViewModel> getCheckout(String userId, String token) {
        return Mono.zip(
                cartClient.getCart(userId, token).onErrorReturn(Collections.emptyList()),
                userClient.getProfile(userId, token).onErrorReturn(UserProfileDto.empty()),
                Mono.just(Collections.<CheckoutViewModel.AddressDto>emptyList())
        )
        .map(t -> {
            List<CartItemDto> items = t.getT1();
            int total = items.stream().mapToInt(CartItemDto::quantity).sum();
            BigDecimal amount = items.stream()
                    .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CheckoutViewModel(items, total, amount, t.getT2(), t.getT3());
        });
    }
}
