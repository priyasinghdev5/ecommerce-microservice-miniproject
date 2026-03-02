package com.ecom.common.kafka;

/**
 * Central registry of all Kafka topic names.
 * Import this in any service that produces or consumes Kafka events.
 * Never hardcode topic strings — always use these constants.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // ── Order topics ──────────────────────────────────────────────
    public static final String ORDER_CREATED          = "order.created";
    public static final String ORDER_CONFIRMED        = "order.confirmed";
    public static final String ORDER_FAILED           = "order.failed";
    public static final String ORDER_CANCELLED        = "order.cancelled";

    // ── Payment topics ────────────────────────────────────────────
    public static final String PAYMENT_SUCCESS        = "payment.success";
    public static final String PAYMENT_FAILED         = "payment.failed";

    // ── Inventory / stock topics ──────────────────────────────────
    public static final String STOCK_RESERVED         = "stock.reserved";
    public static final String STOCK_RELEASED         = "stock.released";
    public static final String STOCK_INSUFFICIENT     = "stock.insufficient";

    // ── Notification topics ───────────────────────────────────────
    public static final String NOTIFICATION_EVENTS    = "notification.events";
    public static final String NOTIFICATION_DLQ       = "notification.events.dlq";

    // ── Product topics ────────────────────────────────────────────
    public static final String PRODUCT_IMPORT         = "product.import";
    public static final String PRODUCT_UPDATED        = "product.updated";
}
