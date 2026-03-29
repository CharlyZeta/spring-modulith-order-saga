package com.showcase.ordersystem.infrastructure;

import com.showcase.ordersystem.orders.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test") // Usar perfil de test para evitar levantar infraestructura real si es posible
class MetricsObservabilityTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldIncrementOrderCounterWhenOrderIsCreated() {
        // Arrange
        double initialCount = getCount("orders.created.total");
        
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-METRIC", "metric@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "P1", "Product 1", 1, new BigDecimal("10.00")))
        );

        // Act
        orderService.createOrder(request);

        // Assert
        double finalCount = getCount("orders.created.total");
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }

    private double getCount(String metricName) {
        return meterRegistry.find(metricName).counter() != null 
                ? meterRegistry.get(metricName).counter().count() 
                : 0.0;
    }
}
