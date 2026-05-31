package com.showcase.ordersystem.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderIdempotencyTest {

    @Autowired
    OrderService orderService;

    @Test
    void shouldReturnSameOrderIdForDuplicateRequest() {
        String productId = "PROD-001";
        String idempotencyKey = UUID.randomUUID().toString();
        
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-IDEM",
                "idem@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        productId, "Laptop", 1, new BigDecimal("1000.00")))
        );

        // 1. First request
        String orderId1 = orderService.createOrder(request, idempotencyKey);
        assertThat(orderId1).isNotNull();

        // 2. Second request with same key
        String orderId2 = orderService.createOrder(request, idempotencyKey);
        
        // 3. Verify it's the same ID
        assertThat(orderId2).isEqualTo(orderId1);
        
        System.out.println("Verified idempotency for key: " + idempotencyKey);
    }
}
