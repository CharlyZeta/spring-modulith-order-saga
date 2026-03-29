package com.showcase.ordersystem.orders;

import com.showcase.ordersystem.shared.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
class OrderModuleIsolatedTest {

    @Autowired
    OrderService orderService;

    @Test
    void shouldPublishOrderCreatedEvent(AssertablePublishedEvents events) {
        // Arrange
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-MOD", "mod@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "P-MOD", "Mod Product", 1, new BigDecimal("10.00")))
        );

        // Act
        String orderId = orderService.createOrder(request);

        // Assert
        assertThat(orderId).isNotNull();
        
        // Verificamos que el evento se publicó correctamente.
        // @ApplicationModuleTest se encarga de interceptar los eventos.
        events.assertThat()
                .contains(OrderCreatedEvent.class)
                .matching(event -> event.orderId().equals(orderId))
                .matching(event -> event.customerId().equals("CUST-MOD"));
    }
}
