package com.showcase.ordersystem.orders;

import com.showcase.ordersystem.orders.internal.Order;
import com.showcase.ordersystem.orders.internal.OrderItem;
import com.showcase.ordersystem.orders.internal.OrderRepository;
import com.showcase.ordersystem.orders.internal.OrderStatus;
import com.showcase.ordersystem.shared.InventoryReservedEvent;
import com.showcase.ordersystem.shared.OrderCancelledEvent;
import com.showcase.ordersystem.shared.OrderCompletedEvent;
import com.showcase.ordersystem.shared.OrderCreatedEvent;
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
        return createOrder(request, null);
    }

    @Transactional
    public String createOrder(CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            log.info("Checking idempotency key: {}", idempotencyKey);
            var existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                log.warn("Duplicate order detected for idempotency key: {}", idempotencyKey);
                return existingOrder.get().getId();
            }
        }

        log.info("Creating order for customer: {}", request.customerId());

        // Increment order creation counter
        meterRegistry.counter("orders.created.total").increment();

        // Build order entity
        Order order = Order.builder()
                .customerId(request.customerId())
                .customerEmail(request.customerEmail())
                .idempotencyKey(idempotencyKey)
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
        log.info("Received inventory reservation event for order: {} (success: {})", 
                event.orderId(), event.success());

        orderRepository.findById(event.orderId()).ifPresent(order -> {
            if (event.success()) {
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
                order.complete();
                orderRepository.save(order);

                // Publish completion event for notifications/shipping
                OrderCompletedEvent completedEvent = new OrderCompletedEvent(
                        order.getId(),
                        order.getCustomerId(),
                        order.getCustomerEmail(),
                        Instant.now()
                );
                eventPublisher.publishEvent(completedEvent);
                
                log.info("Order {} completed successfully", event.orderId());
            } else {
                log.warn("Inventory reservation failed for order {}: {}", event.orderId(), event.failureReason());
                order.cancel(event.failureReason());
                orderRepository.save(order);
                
                // Even if inventory failed, we publish cancellation in case other modules 
                // (like Payments or Shipping) need to compensate.
                publishCancellationEvent(order);
            }
        });
    }

    private void publishCancellationEvent(Order order) {
        List<OrderCancelledEvent.OrderItem> items = order.getItems().stream()
                .map(item -> new OrderCancelledEvent.OrderItem(item.getProductId(), item.getQuantity()))
                .toList();

        eventPublisher.publishEvent(new OrderCancelledEvent(
                order.getId(),
                items,
                Instant.now()
        ));
        log.info("OrderCancelledEvent published for order {}", order.getId());
    }

    @Transactional
    public void cancelOrder(String orderId) {
        log.info("Manually cancelling order: {}", orderId);
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            publishCancellationEvent(order);
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
