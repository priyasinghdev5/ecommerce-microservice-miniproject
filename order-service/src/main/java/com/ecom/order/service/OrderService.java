package com.ecom.order.service;

import com.ecom.common.events.OrderCreatedEvent;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.order.dto.*;
import com.ecom.order.entity.*;
import com.ecom.order.grpc.InventoryGrpcClient;
import com.ecom.order.mapper.OrderMapper;
import com.ecom.order.outbox.OutboxEvent;
import com.ecom.order.outbox.OutboxRepository;
import com.ecom.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.inventory.grpc.ReserveStockResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final InventoryGrpcClient inventoryClient;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    /**
     * Create order — Transactional Outbox Pattern.
     *
     * SINGLE TRANSACTION:
     *   1. INSERT order (PENDING)
     *   2. INSERT order_items
     *   3. INSERT outbox_event (NEW)
     *   4. COMMIT
     *
     * OutboxScheduler picks up the event and publishes to Kafka.
     * Stock reservation happens via gRPC BEFORE writing the order.
     */
    @Transactional
    @Bulkhead(name = "order-service", type = Bulkhead.Type.SEMAPHORE)
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {

        // Step 1: Reserve stock via gRPC for each item (synchronous)
        for (CreateOrderRequest.OrderItemRequest item : request.items()) {
            ReserveStockResponse stockResponse = inventoryClient.reserveStock(
                    "PENDING_" + userId,   // temp order id before DB insert
                    item.productId().toString(),
                    item.sku(),
                    item.quantity()
            );
            if (!stockResponse.getSuccess()) {
                throw new RuntimeException("Stock unavailable for SKU: " + item.sku()
                        + " — " + stockResponse.getMessage());
            }
        }

        // Step 2: Build order entity
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress())
                .currency(request.currency())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest itemReq : request.items()) {
            BigDecimal lineTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(lineTotal);
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.productId())
                    .sku(itemReq.sku())
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .lineTotal(lineTotal)
                    .build();
            order.addItem(item);
        }
        order.setTotalAmount(total);

        // Step 3: Save order
        Order saved = orderRepository.save(order);

        // Step 4: Write outbox event IN SAME TRANSACTION
        OrderCreatedEvent event = buildOrderCreatedEvent(saved);
        OutboxEvent outbox = buildOutboxEvent(saved, event);
        outboxRepository.save(outbox);

        log.info("Order created with outbox: orderId={} userId={} total={}",
                saved.getId(), userId, total);
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(orderMapper::toResponse).toList();
    }

    // Called by saga handlers when payment succeeds/fails
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.updateStatus(newStatus, reason);
        orderRepository.save(order);
        log.info("Order status updated: orderId={} status={}", orderId, newStatus);
    }

    // ── Private helpers ────────────────────────────────────────────

    private OrderCreatedEvent buildOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItem> items = order.getItems().stream()
                .map(i -> new OrderCreatedEvent.OrderItem(
                        i.getProductId(), i.getSku(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderCreatedEvent(
                UUID.randomUUID(), order.getId(), order.getUserId(),
                items, order.getTotalAmount(), order.getCurrency(),
                order.getShippingAddress(), Instant.now());
    }

    private OutboxEvent buildOutboxEvent(Order order, OrderCreatedEvent event) {
        try {
            return OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(order.getId())
                    .eventType("OrderCreatedEvent")
                    .topic(KafkaTopics.ORDER_CREATED)
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxEvent.OutboxStatus.NEW)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
