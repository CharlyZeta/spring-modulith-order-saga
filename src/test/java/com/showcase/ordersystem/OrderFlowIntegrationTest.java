package com.showcase.ordersystem;

import com.showcase.ordersystem.inventory.InventoryService;
import com.showcase.ordersystem.orders.OrderService;
import com.showcase.ordersystem.orders.internal.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean up database before each test to ensure isolation
        jdbcTemplate.execute("TRUNCATE TABLE order_items CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE orders CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE inventory_items CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE event_publication CASCADE");

        // Initialize inventory before each test
        inventoryService.initializeInventory("PROD-1", "Test Product", 10);
    }

    @Test
    void shouldCompleteFullOrderFlow() {
        // 1. Create order
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-1",
                "customer@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "PROD-1", "Test Product", 2, new BigDecimal("100.00")))
        );

        orderService.createOrder(request);

        // 2. Wait for completion (asynchronous saga)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var orders = orderService.getOrdersByCustomer("CUST-1", org.springframework.data.domain.Pageable.unpaged());
            assertThat(orders.getContent()).isNotEmpty();
            assertThat(orders.getContent().get(0).status()).isEqualTo(OrderStatus.COMPLETED.name());
        });

        // 3. Verify final state
        InventoryService.InventoryStatus inventory = inventoryService.getInventoryStatus("PROD-1");
        assertThat(inventory.availableQuantity()).isEqualTo(8);
        assertThat(inventory.reservedQuantity()).isEqualTo(2);
    }

    @Test
    void shouldCancelOrderWhenInventoryIsInsufficient() {
        // 1. Create order with too much quantity
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-2",
                "customer2@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "PROD-1", "Test Product", 15, new BigDecimal("100.00")))
        );

        orderService.createOrder(request);

        // 2. Wait for cancellation
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var orders = orderService.getOrdersByCustomer("CUST-2", org.springframework.data.domain.Pageable.unpaged());
            assertThat(orders.getContent()).isNotEmpty();
            assertThat(orders.getContent().get(0).status()).isEqualTo(OrderStatus.CANCELLED.name());
        });

        // 3. Verify inventory unchanged
        InventoryService.InventoryStatus inventory = inventoryService.getInventoryStatus("PROD-1");
        assertThat(inventory.availableQuantity()).isEqualTo(10);
        assertThat(inventory.reservedQuantity()).isEqualTo(0);
    }
}
