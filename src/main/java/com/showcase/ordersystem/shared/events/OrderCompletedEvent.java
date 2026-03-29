package com.showcase.ordersystem.shared.events;

import java.time.Instant;

/**
 * Domain event published when an order is successfully completed.
 * This triggers notification to the customer.
 */
public record OrderCompletedEvent(
    String orderId,
    String customerId,
    String customerEmail,
    Instant timestamp
) {}
