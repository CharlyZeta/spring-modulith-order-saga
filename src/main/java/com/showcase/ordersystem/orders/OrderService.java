package com.showcase.ordersystem.orders;

import com.showcase.ordersystem.orders.internal.Order;
import com.showcase.ordersystem.orders.internal.OrderItem;
import com.showcase.ordersystem.orders.internal.OrderRepository;
import com.showcase.ordersystem.orders.internal.OrderStatus;
import com.showcase.ordersystem.shared.events.InventoryReservedEvent;
import com.showcase.ordersystem.shared.events.OrderCompletedEvent;
import com.showcase.ordersystem.shared.events.OrderCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Public API of the Orders module.
 * Manages order lifecycle and publishes domain events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @Transactional
    public String createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());

        // Increment order creation counter
        meterRegistry.counter("orders.created.total").increment();

        // Build order entity
        Order order = Order.builder()
                .customerId(request.customerId())
                .customerEmail(request.customerEmail())
                .totalAmount(calculateTotal(request.items()))
                .build();

        // Add items
        request.items().forEach(itemReq -> {
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.productId())
                    .productName(itemReq.productName())
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .build();
            order.addItem(item);
        });

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Publish domain event (Spring Modulith will handle async processing)
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                mapToEventItems(savedOrder.getItems()),
                savedOrder.getTotalAmount(),
                Instant.now()
        );
        eventPublisher.publishEvent(event);

        log.info("Order created with ID: {}", savedOrder.getId());
        return savedOrder.getId();
    }

    /**
     * Listens to inventory reservation events from the Inventory module.
     * This is an example of inter-module communication via events.
     */
    @ApplicationModuleListener
    void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Received inventory reservation event for order: {}", event.orderId());

        orderRepository.findById(event.orderId()).ifPresent(order -> {
            if (event.success()) {
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
                order.complete();
                orderRepository.save(order);

                // Publish completion event
                OrderCompletedEvent completedEvent = new OrderCompletedEvent(
                        order.getId(),
                        order.getCustomerId(),
                        order.getCustomerEmail(),
                        Instant.now()
                );
                eventPublisher.publishEvent(completedEvent);
                
                log.info("Order {} completed successfully", event.orderId());
            } else {
                order.cancel(event.failureReason());
                orderRepository.save(order);
                log.warn("Order {} cancelled: {}", event.orderId(), event.failureReason());
            }
        });
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToOrderInfo)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderInfo> getOrdersByCustomer(String customerId, org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToOrderInfo);
    }

    // Helper methods
    private BigDecimal calculateTotal(List<CreateOrderRequest.OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderCreatedEvent.OrderItem> mapToEventItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderCreatedEvent.OrderItem(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .collect(Collectors.toList());
    }

    private OrderInfo mapToOrderInfo(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }

    // DTOs
    public record CreateOrderRequest(
            @jakarta.validation.constraints.NotBlank(message = "Customer ID is required")
            String customerId,
            
            @jakarta.validation.constraints.Email(message = "Invalid email format")
            @jakarta.validation.constraints.NotBlank(message = "Customer email is required")
            String customerEmail,
            
            @jakarta.validation.constraints.NotEmpty(message = "Order must have at least one item")
            @jakarta.validation.Valid
            List<OrderItemRequest> items
    ) {
        public record OrderItemRequest(
                @jakarta.validation.constraints.NotBlank(message = "Product ID is required")
                String productId,
                
                @jakarta.validation.constraints.NotBlank(message = "Product name is required")
                String productName,
                
                @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
                int quantity,
                
                @jakarta.validation.constraints.NotNull(message = "Unit price is required")
                @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
                BigDecimal unitPrice
        ) {}
    }

    public record OrderInfo(
            String orderId,
            String customerId,
            BigDecimal totalAmount,
            String status,
            Instant createdAt
    ) {}
}
