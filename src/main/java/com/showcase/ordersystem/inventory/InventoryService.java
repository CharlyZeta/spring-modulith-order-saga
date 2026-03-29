package com.showcase.ordersystem.inventory;

import com.showcase.ordersystem.inventory.internal.InventoryItem;
import com.showcase.ordersystem.inventory.internal.InventoryRepository;
import com.showcase.ordersystem.shared.events.InventoryReservedEvent;
import com.showcase.ordersystem.shared.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
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
    void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event for order: {}", event.orderId());

        String reservationId = UUID.randomUUID().toString();
        boolean allItemsReserved = true;
        String failureReason = null;

        try {
            // Attempt to reserve inventory for each item
            for (OrderCreatedEvent.OrderItem item : event.items()) {
                InventoryItem inventoryItem = inventoryRepository
                        .findByProductId(item.productId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Product not found: " + item.productId()
                        ));

                if (!inventoryItem.canReserve(item.quantity())) {
                    allItemsReserved = false;
                    failureReason = String.format(
                            "Insufficient stock for product %s (requested: %d, available: %d)",
                            item.productId(),
                            item.quantity(),
                            inventoryItem.getAvailableQuantity()
                    );
                    break;
                }

                inventoryItem.reserve(item.quantity());
                inventoryRepository.save(inventoryItem);
                
                log.info("Reserved {} units of product {} for order {}",
                        item.quantity(), item.productId(), event.orderId());
            }

        } catch (Exception e) {
            allItemsReserved = false;
            failureReason = "Error reserving inventory: " + e.getMessage();
            log.error("Failed to reserve inventory for order: {}", event.orderId(), e);
        }

        // Publish inventory reservation result
        InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                event.orderId(),
                reservationId,
                allItemsReserved,
                failureReason,
                Instant.now()
        );
        eventPublisher.publishEvent(reservedEvent);

        log.info("Published inventory reserved event for order: {} (success: {})",
                event.orderId(), allItemsReserved);
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
