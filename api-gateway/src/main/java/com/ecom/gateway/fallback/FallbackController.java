package com.ecom.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback endpoints invoked by Circuit Breaker when a downstream
 * service is unavailable (OPEN state).
 *
 * Each route in application.yml has:
 *   fallbackUri: forward:/fallback/{service}
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return fallback("auth-service", "Authentication service is temporarily unavailable");
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        return fallback("user-service", "User service is temporarily unavailable");
    }

    @GetMapping("/product")
    public ResponseEntity<Map<String, Object>> productFallback() {
        return fallback("product-service", "Product service is temporarily unavailable");
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        return fallback("inventory-service", "Inventory service is temporarily unavailable");
    }

    @GetMapping("/cart")
    public ResponseEntity<Map<String, Object>> cartFallback() {
        return fallback("cart-service", "Cart service is temporarily unavailable");
    }

    @GetMapping("/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        return fallback("order-service", "Order service is temporarily unavailable. Please try again shortly.");
    }

    @GetMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        return fallback("payment-service", "Payment service is temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> fallback(String service, String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "service", service,
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }
}
