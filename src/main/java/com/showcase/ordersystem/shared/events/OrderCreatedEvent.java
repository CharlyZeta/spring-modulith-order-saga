package com.showcase.ordersystem.shared.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Domain event published when a new order is created.
 * This event triggers:
 * - Inventory reservation
 * - Customer notification
 */
public record OrderCreatedEvent(
    String orderId,
    String customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    Instant timestamp
) {
    public record OrderItem(
        String productId,
        int quantity,
        BigDecimal unitPrice
    ) {}
}
