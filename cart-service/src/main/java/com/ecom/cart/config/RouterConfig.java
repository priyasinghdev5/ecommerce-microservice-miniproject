package com.ecom.cart.config;

import com.ecom.cart.handler.CartHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Functional router — replaces @RestController for WebFlux.
 * Maps HTTP routes to handler methods reactively.
 */
@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> cartRoutes(CartHandler handler) {
        return RouterFunctions.route()
                .POST("/api/cart/items",           handler::addItem)
                .GET("/api/cart",                  handler::getCart)
                .GET("/api/cart/summary",          handler::getCartSummary)
                .DELETE("/api/cart/items/{productId}", handler::removeItem)
                .DELETE("/api/cart",               handler::clearCart)
                .build();
    }
}
