package com.showcase.ordersystem.infrastructure;

import com.showcase.ordersystem.orders.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that events are being recorded in the event_publication table.
 */
@SpringBootTest
@ActiveProfiles("test")
class EventResilienceTest {

    @Autowired
    OrderService orderService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    io.micrometer.tracing.Tracer tracer;

    @Test
    void shouldRecordEventsInRegistry() throws InterruptedException {
        String orderId;
        io.micrometer.tracing.Span newSpan = tracer.nextSpan().name("test-span").start();
        try (io.micrometer.tracing.Tracer.SpanInScope ws = tracer.withSpan(newSpan)) {
            // 1. Create Order
            OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                    "RESILIENCE-CUST",
                    "resilience@test.com",
                    List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                            "PROD-1", "Test Product", 1, new BigDecimal("50.00")))
            );

            orderId = orderService.createOrder(request);
            assertThat(orderId).isNotNull();
            
            System.out.println("TraceID: " + newSpan.context().traceId());
        } finally {
            newSpan.end();
        }

        // 2. Query the event_publication table
        // We wait a bit to ensure async processing has started/finished
        Thread.sleep(1000);

        List<Map<String, Object>> publications = jdbcTemplate.queryForList(
                "SELECT * FROM event_publication WHERE serialized_event LIKE ? ORDER BY publication_date ASC",
                "%" + orderId + "%"
        );

        assertThat(publications).isNotEmpty();
        
        // We look for OrderCreatedEvent specifically
        boolean found = publications.stream()
                .anyMatch(p -> p.get("event_type").toString().contains("OrderCreatedEvent"));
        
        assertThat(found).isTrue();
        
        System.out.println("Verified event publication in registry for order: " + orderId);
    }
}
