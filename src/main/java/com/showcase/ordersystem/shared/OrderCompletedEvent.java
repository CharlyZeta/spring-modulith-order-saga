package com.showcase.ordersystem.shared;

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
