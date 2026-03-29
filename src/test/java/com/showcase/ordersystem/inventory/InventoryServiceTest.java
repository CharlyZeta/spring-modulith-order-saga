package com.showcase.ordersystem.inventory;

import com.showcase.ordersystem.inventory.internal.InventoryItem;
import com.showcase.ordersystem.inventory.internal.InventoryRepository;
import com.showcase.ordersystem.shared.events.InventoryReservedEvent;
import com.showcase.ordersystem.shared.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void shouldReserveInventorySuccessfully() {
        // Arrange
        String productId = "PROD-1";
        InventoryItem item = InventoryItem.builder()
                .productId(productId)
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();

        OrderCreatedEvent event = new OrderCreatedEvent(
                "order-1", "CUST-1",
                List.of(new OrderCreatedEvent.OrderItem(productId, 2, new BigDecimal("100.00"))),
                new BigDecimal("200.00"), Instant.now()
        );

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));

        // Act
        inventoryService.onOrderCreated(event);

        // Assert
        verify(inventoryRepository).save(item);
        assertThat(item.getAvailableQuantity()).isEqualTo(8);
        assertThat(item.getReservedQuantity()).isEqualTo(2);

        ArgumentCaptor<InventoryReservedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().success()).isTrue();
    }

    @Test
    void shouldFailReservationWhenStockInsufficient() {
        // Arrange
        String productId = "PROD-1";
        InventoryItem item = InventoryItem.builder()
                .productId(productId)
                .availableQuantity(1)
                .reservedQuantity(0)
                .build();

        OrderCreatedEvent event = new OrderCreatedEvent(
                "order-1", "CUST-1",
                List.of(new OrderCreatedEvent.OrderItem(productId, 5, new BigDecimal("100.00"))),
                new BigDecimal("500.00"), Instant.now()
        );

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item));

        // Act
        inventoryService.onOrderCreated(event);

        // Assert
        verify(inventoryRepository, never()).save(any());
        
        ArgumentCaptor<InventoryReservedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().success()).isFalse();
        assertThat(eventCaptor.getValue().failureReason()).contains("Insufficient stock");
    }
}
