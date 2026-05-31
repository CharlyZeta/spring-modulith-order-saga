package com.showcase.ordersystem.inventory;

import com.showcase.ordersystem.inventory.internal.InventoryItem;
import com.showcase.ordersystem.inventory.internal.InventoryRepository;
import com.showcase.ordersystem.shared.InventoryReservedEvent;
import com.showcase.ordersystem.shared.OrderCancelledEvent;
import com.showcase.ordersystem.shared.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API of the Inventory module.
 * Listens to OrderCreatedEvent and manages inventory reservations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Listens to OrderCreatedEvent from the Orders module.
     * Attempts to reserve inventory and publishes the result.
     * 
     * This demonstrates asynchronous inter-module communication via domain events.
     */
    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event for order: {}", event.orderId());

        String reservationId = UUID.randomUUID().toString();
        
        try {
            // Attempt to reserve inventory for each item
            for (OrderCreatedEvent.OrderItem item : event.items()) {
                InventoryItem inventoryItem = inventoryRepository
                        .findByProductId(item.productId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Product not found: " + item.productId()
                        ));

                if (!inventoryItem.canReserve(item.quantity())) {
                    log.warn("Insufficient stock for product {} in order {}", item.productId(), event.orderId());
                    publishFailure(event.orderId(), reservationId, String.format(
                            "Insufficient stock for product %s (requested: %d, available: %d)",
                            item.productId(), item.quantity(), inventoryItem.getAvailableQuantity()
                    ));
                    // We throw an exception to trigger @Transactional rollback of any previous items in the loop
                    throw new RuntimeException("Insufficient stock - rolling back partial reservations");
                }

                inventoryItem.reserve(item.quantity());
                inventoryRepository.save(inventoryItem);
                
                log.info("Reserved {} units of product {} for order {}",
                        item.quantity(), item.productId(), event.orderId());
            }

            // If all items reserved successfully
            publishSuccess(event.orderId(), reservationId);

        } catch (IllegalArgumentException e) {
            log.error("Validation error for order {}: {}", event.orderId(), e.getMessage());
            publishFailure(event.orderId(), reservationId, e.getMessage());
            throw e; // Rollback
        } catch (Exception e) {
            if (!(e instanceof RuntimeException && e.getMessage().contains("Insufficient stock"))) {
                log.error("Unexpected error reserving inventory for order: {}", event.orderId(), e);
                publishFailure(event.orderId(), reservationId, "Internal error: " + e.getMessage());
            }
            throw e; // Rollback
        }
    }

    private void publishSuccess(String orderId, String reservationId) {
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                orderId, reservationId, true, null, Instant.now()
        );
        eventPublisher.publishEvent(reservedEvent);
        log.info("Published SUCCESS inventory reserved event for order: {}", orderId);
    }

    private void publishFailure(String orderId, String reservationId, String reason) {
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                orderId, reservationId, false, reason, Instant.now()
        );
        eventPublisher.publishEvent(reservedEvent);
        log.info("Published FAILURE inventory reserved event for order: {}", orderId);
    }

    /**
     * Listens to OrderCancelledEvent and releases reserved inventory.
     * This is a compensating transaction (Saga pattern).
     */
    @ApplicationModuleListener
    void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order cancelled event for order: {}. Releasing inventory.", event.orderId());

        event.items().forEach(item -> {
            inventoryRepository.findByProductId(item.productId()).ifPresent(inventoryItem -> {
                inventoryItem.release(item.quantity());
                inventoryRepository.save(inventoryItem);
                log.info("Released {} units of product {} for order {}", 
                        item.quantity(), item.productId(), event.orderId());
            });
        });
    }

    /**
     * Initialize inventory for testing purposes.
     */
    @Transactional
    public void initializeInventory(String productId, String productName, int quantity) {
        InventoryItem item = InventoryItem.builder()
                .productId(productId)
                .productName(productName)
                .availableQuantity(quantity)
                .reservedQuantity(0)
                .build();
        inventoryRepository.save(item);
        log.info("Initialized inventory: {} with {} units", productName, quantity);
    }

    @Transactional(readOnly = true)
    public InventoryStatus getInventoryStatus(String productId) {
        return inventoryRepository.findByProductId(productId)
                .map(item -> new InventoryStatus(
                        item.getProductId(),
                        item.getProductName(),
                        item.getAvailableQuantity(),
                        item.getReservedQuantity()
                ))
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    public record InventoryStatus(
            String productId,
            String productName,
            int availableQuantity,
            int reservedQuantity
    ) {}
}
