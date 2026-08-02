package com.showcase.ordersystem.inventory;

import com.showcase.ordersystem.inventory.internal.InventoryItem;
import com.showcase.ordersystem.inventory.internal.InventoryRepository;
import com.showcase.ordersystem.shared.InventoryReservedEvent;
import com.showcase.ordersystem.shared.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InventoryBugReproductionTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldNotLeavePartialReservationsWhenOneItemFails() {
        // Arrange
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String prod1 = "PROD-BUG-1-" + suffix;
        String prod2 = "PROD-BUG-2-" + suffix;
        
        inventoryService.initializeInventory(prod1, "Product 1", 10);
        inventoryService.initializeInventory(prod2, "Product 2", 1); // Only 1 available

        OrderCreatedEvent event = new OrderCreatedEvent(
                "order-bug-" + suffix, "CUST-BUG",
                List.of(
                    new OrderCreatedEvent.OrderItem(prod1, 5, new BigDecimal("10.00")), // Success
                    new OrderCreatedEvent.OrderItem(prod2, 5, new BigDecimal("10.00"))  // Should Fail
                ),
                new BigDecimal("100.00"), Instant.now()
        );

        // Act - Call directly (synchronous for this thread if not intercepted)
        // But since it's a @Service bean in @SpringBootTest, it might be async if @Async or similar is active.
        // Spring Modulith's @ApplicationModuleListener is async by default.
        inventoryService.onOrderCreated(event);

        // Assert with Awaitility
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            InventoryService.InventoryStatus status1 = inventoryService.getInventoryStatus(prod1);
            assertThat(status1.reservedQuantity())
                .as("Product 1 should NOT have reserved quantity if the whole order failed")
                .isEqualTo(0);
        });
    }
}
