package com.ecom.order.grpc;

import com.ecom.inventory.grpc.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC client — calls inventory-service on port 9090.
 * Wrapped with Resilience4j Circuit Breaker.
 * If inventory-service is down, fallback returns false (cannot reserve).
 */
@Slf4j
@Component
public class InventoryGrpcClient {

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    @CircuitBreaker(name = "inventory-grpc", fallbackMethod = "checkStockFallback")
    public CheckStockResponse checkStock(String productId, String sku, int quantity) {
        log.debug("gRPC checkStock: sku={} qty={}", sku, quantity);
        return inventoryStub.checkStock(CheckStockRequest.newBuilder()
                .setProductId(productId)
                .setSku(sku)
                .setQuantity(quantity)
                .build());
    }

    @CircuitBreaker(name = "inventory-grpc", fallbackMethod = "reserveStockFallback")
    public ReserveStockResponse reserveStock(String orderId, String productId, String sku, int quantity) {
        log.debug("gRPC reserveStock: orderId={} sku={} qty={}", orderId, sku, quantity);
        return inventoryStub.reserveStock(ReserveStockRequest.newBuilder()
                .setOrderId(orderId)
                .setProductId(productId)
                .setSku(sku)
                .setQuantity(quantity)
                .build());
    }

    @CircuitBreaker(name = "inventory-grpc")
    public ReleaseStockResponse releaseStock(String orderId, String reservationId) {
        return inventoryStub.releaseStock(ReleaseStockRequest.newBuilder()
                .setOrderId(orderId)
                .setReservationId(reservationId)
                .build());
    }

    // ── Fallback methods ───────────────────────────────────────────

    public CheckStockResponse checkStockFallback(String productId, String sku, int quantity, Throwable t) {
        log.error("Circuit breaker open for checkStock: {}", t.getMessage());
        return CheckStockResponse.newBuilder()
                .setAvailable(false)
                .setCurrentStock(0)
                .setMessage("Inventory service unavailable")
                .build();
    }

    public ReserveStockResponse reserveStockFallback(String orderId, String productId, String sku, int quantity, Throwable t) {
        log.error("Circuit breaker open for reserveStock: {}", t.getMessage());
        return ReserveStockResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Inventory service unavailable — please retry")
                .build();
    }
}
