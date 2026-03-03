package com.ecom.inventory.service;

import com.ecom.common.events.StockReleasedEvent;
import com.ecom.common.events.StockReservedEvent;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.inventory.dto.*;
import com.ecom.inventory.entity.Inventory;
import com.ecom.inventory.entity.StockReservation;
import com.ecom.inventory.repository.InventoryRepository;
import com.ecom.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecom.inventory.grpc.InventoryServiceGrpc;
import com.ecom.inventory.grpc.CheckStockRequest;
import com.ecom.inventory.grpc.CheckStockResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "inventory:stock:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    // ── Stock check (used by REST controller for admin) ────────────

    @Transactional(readOnly = true)
    public StockResponse checkStock(String sku) {
        // Try Redis cache first
        Object cached = redisTemplate.opsForValue().get(CACHE_PREFIX + sku);
        if (cached != null) {
            return (StockResponse) cached;
        }
        Inventory inv = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + sku));
        StockResponse response = new StockResponse(inv.getSku(), inv.getAvailableQty(), inv.getTotalQuantity(), inv.getReservedQty());
        redisTemplate.opsForValue().set(CACHE_PREFIX + sku, response, CACHE_TTL);
        return response;
    }

    // ── Reserve stock (called by gRPC server) ──────────────────────

    @Transactional
    public ReserveStockResult reserveStock(UUID orderId, UUID productId, String sku, int quantity) {
        Inventory inventory = inventoryRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + sku));

        if (!inventory.isAvailable(quantity)) {
            log.warn("Insufficient stock for sku={} requested={} available={}",
                    sku, quantity, inventory.getAvailableQty());

            // Publish insufficient event for saga
            kafkaTemplate.send(KafkaTopics.STOCK_INSUFFICIENT, orderId.toString(),
                    new StockInsufficientPayload(orderId, sku, quantity));
            return new ReserveStockResult(false, null, "Insufficient stock");
        }

        // Create reservation
        StockReservation reservation = StockReservation.builder()
                .orderId(orderId)
                .inventory(inventory)
                .sku(sku)
                .quantity(quantity)
                .status("RESERVED")
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();
        StockReservation saved = reservationRepository.save(reservation);

        // Increment reserved count
        inventory.setReservedQty(inventory.getReservedQty() + quantity);
        inventoryRepository.save(inventory);

        // Evict cache
        redisTemplate.delete(CACHE_PREFIX + sku);

        // Publish StockReservedEvent → payment-service picks this up
        StockReservedEvent event = new StockReservedEvent(
                UUID.randomUUID(), orderId,
                List.of(new StockReservedEvent.ReservationDetail(
                        inventory.getId(), saved.getId(), sku, quantity)),
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.STOCK_RESERVED, orderId.toString(), event);
        log.info("Stock reserved: orderId={} sku={} qty={} reservationId={}", orderId, sku, quantity, saved.getId());
        return new ReserveStockResult(true, saved.getId().toString(), "Stock reserved successfully");
    }

    // ── Release stock (Saga compensation) ─────────────────────────

    @Transactional
    public void releaseStock(UUID orderId, UUID reservationId) {
        StockReservation reservation = reservationRepository.findByIdAndOrderId(reservationId, orderId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!"RESERVED".equals(reservation.getStatus())) {
            log.warn("Reservation {} already in status {}", reservationId, reservation.getStatus());
            return;
        }

        Inventory inventory = reservation.getInventory();
        inventory.setReservedQty(Math.max(0, inventory.getReservedQty() - reservation.getQuantity()));
        inventoryRepository.save(inventory);

        reservation.setStatus("RELEASED");
        reservationRepository.save(reservation);

        // Evict cache
        redisTemplate.delete(CACHE_PREFIX + reservation.getSku());

        // Publish StockReleasedEvent
        StockReleasedEvent event = new StockReleasedEvent(
                UUID.randomUUID(), orderId, "PAYMENT_FAILED", Instant.now());
        kafkaTemplate.send(KafkaTopics.STOCK_RELEASED, orderId.toString(), event);
        log.info("Stock released: orderId={} reservationId={}", orderId, reservationId);
    }

    // ── Admin: add stock ───────────────────────────────────────────

    @Transactional
    public InventoryResponse addStock(String sku, int quantity) {
        Inventory inv = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + sku));
        inv.setTotalQuantity(inv.getTotalQuantity() + quantity);
        Inventory saved = inventoryRepository.save(inv);
        redisTemplate.delete(CACHE_PREFIX + sku);
        return toResponse(saved);
    }

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest req) {
        Inventory inv = Inventory.builder()
                .productId(req.productId())
                .sku(req.sku())
                .totalQuantity(req.initialQuantity())
                .reservedQty(0)
                .reorderPoint(req.reorderPoint())
                .warehouseLoc(req.warehouseLoc())
                .build();
        return toResponse(inventoryRepository.save(inv));
    }

    // ── Expired reservation cleanup ────────────────────────────────

    @Scheduled(fixedDelay = 60000)  // every 60 seconds
    @Transactional
    public void cleanupExpiredReservations() {
        List<StockReservation> expired = reservationRepository.findExpiredReservations(Instant.now());
        for (StockReservation r : expired) {
            Inventory inv = r.getInventory();
            inv.setReservedQty(Math.max(0, inv.getReservedQty() - r.getQuantity()));
            inventoryRepository.save(inv);
            r.setStatus("RELEASED");
            reservationRepository.save(r);
            redisTemplate.delete(CACHE_PREFIX + r.getSku());
            log.info("Expired reservation cleaned up: id={} orderId={}", r.getId(), r.getOrderId());
        }
        if (!expired.isEmpty()) {
            log.info("Cleaned up {} expired reservations", expired.size());
        }
    }

    private InventoryResponse toResponse(Inventory inv) {
        return new InventoryResponse(inv.getId(), inv.getProductId(), inv.getSku(),
                inv.getTotalQuantity(), inv.getReservedQty(), inv.getAvailableQty());
    }

    // Inner records for internal use
    public record ReserveStockResult(boolean success, String reservationId, String message) {}
    public record StockInsufficientPayload(UUID orderId, String sku, int requested) {}
}
