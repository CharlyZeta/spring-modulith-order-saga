package com.showcase.ordersystem.shared.events;

import java.time.Instant;

/**
 * Domain event published when inventory is successfully reserved for an order.
 */
public record InventoryReservedEvent(
    String orderId,
    String reservationId,
    boolean success,
    String failureReason,
    Instant timestamp
) {}
