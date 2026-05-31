package com.showcase.ordersystem.shared;

import java.time.Instant;
import java.util.List;

/**
 * Domain event published when an order is cancelled.
 */
public record OrderCancelledEvent(
        String orderId,
        List<OrderItem> items,
        Instant timestamp
) {
    public record OrderItem(
            String productId,
            int quantity
    ) {}
}
