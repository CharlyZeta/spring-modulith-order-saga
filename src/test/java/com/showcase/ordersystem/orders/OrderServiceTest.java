package com.showcase.ordersystem.orders;

import com.showcase.ordersystem.orders.internal.Order;
import com.showcase.ordersystem.orders.internal.OrderRepository;
import com.showcase.ordersystem.shared.events.OrderCreatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@lombok.extern.slf4j.Slf4j
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    @Mock
    private Counter counter;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Arrange
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-1", "test@test.com", 
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "P1", "Prod 1", 1, new BigDecimal("10.00")))
        );

        Order savedOrder = Order.builder()
                .customerId("CUST-1")
                .totalAmount(new BigDecimal("10.00"))
                .build();
        
        // Use reflection on the 'id' field which is in BaseEntity or Order
        try {
            java.lang.reflect.Field idField = null;
            Class<?> current = savedOrder.getClass();
            while (current != null && idField == null) {
                try {
                    idField = current.getDeclaredField("id");
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(savedOrder, "order-123");
            }
        } catch (Exception e) {
            log.error("Failed to set ID via reflection", e);
        }

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        String orderId = orderService.createOrder(request);

        // Assert
        assertThat(orderId).isEqualTo("order-123");
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
        verify(counter).increment();
    }
}
