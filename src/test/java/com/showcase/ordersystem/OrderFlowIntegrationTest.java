package com.showcase.ordersystem;

import com.showcase.ordersystem.inventory.InventoryService;
import com.showcase.ordersystem.orders.OrderService;
import com.showcase.ordersystem.shared.events.OrderCompletedEvent;
import com.showcase.ordersystem.shared.events.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ApplicationModuleTest
class OrderFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Autowired
    OrderService orderService;

    @Autowired
    InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService.initializeInventory("PROD-1", "Test Product", 10);
    }

    @Test
    void shouldCompleteFullOrderFlow(AssertablePublishedEvents events) {
        // 1. Create Order
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-1",
                "customer@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "PROD-1", "Test Product", 2, new BigDecimal("100.00")))
        );

        String orderId = orderService.createOrder(request);
        assertThat(orderId).isNotNull();

        // 2. Verify OrderCreatedEvent was published
        assertThat(events).contains(OrderCreatedEvent.class)
                .matching(ev -> ev.orderId().equals(orderId));

        // 3. Wait and verify OrderCompletedEvent (due to async processing)
        // Spring Modulith test auto-waits for async events if using AssertablePublishedEvents
        assertThat(events).contains(OrderCompletedEvent.class)
                .matching(ev -> ev.orderId().equals(orderId));

        // 4. Verify Final Order Status
        OrderService.OrderInfo orderInfo = orderService.getOrderById(orderId);
        assertThat(orderInfo.status()).isEqualTo("COMPLETED");

        // 5. Verify Inventory was reduced
        InventoryService.InventoryStatus inventory = inventoryService.getInventoryStatus("PROD-1");
        assertThat(inventory.availableQuantity()).isEqualTo(8);
    }
}
