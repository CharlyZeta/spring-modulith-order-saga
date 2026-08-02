package com.showcase.ordersystem.inventory;

import com.showcase.ordersystem.inventory.InventoryService;
import com.showcase.ordersystem.orders.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest
@ActiveProfiles("test")
class InventoryCompensationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    InventoryService inventoryService;

    @Test
    void shouldReleaseInventoryWhenOrderIsCancelled() {
        String productId = "PROD-001"; // Laptop from DataInitializer
        
        // 1. Get initial status
        InventoryService.InventoryStatus initial = inventoryService.getInventoryStatus(productId);
        int initialReserved = initial.reservedQuantity();

        // 2. Create Order
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-COMP",
                "comp@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        productId, "Laptop", 1, new BigDecimal("1000.00")))
        );
        String orderId = orderService.createOrder(request);

        // Wait for reservation
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            InventoryService.InventoryStatus status = inventoryService.getInventoryStatus(productId);
            assertThat(status.reservedQuantity()).isEqualTo(initialReserved + 1);
        });

        // 3. Cancel Order
        orderService.cancelOrder(orderId);

        // 4. Verify inventory is released
        // This SHOULD FAIL until Task 7 is implemented
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            InventoryService.InventoryStatus status = inventoryService.getInventoryStatus(productId);
            assertThat(status.reservedQuantity()).isEqualTo(initialReserved);
        });
    }
}
