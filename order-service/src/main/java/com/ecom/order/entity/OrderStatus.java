package com.ecom.order.entity;

public enum OrderStatus {
    PENDING,
    STOCK_RESERVED,
    CONFIRMED,
    STOCK_UNAVAILABLE,
    PAYMENT_FAILED,
    CANCELLED,
    SHIPPED,
    DELIVERED
}
