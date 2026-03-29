package com.showcase.ordersystem.orders;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for order management.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestBody @jakarta.validation.Valid OrderService.CreateOrderRequest request) {
        
        String orderId = orderService.createOrder(request);
        return ResponseEntity.ok(new CreateOrderResponse(orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderService.OrderInfo> getOrderById(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<org.springframework.data.domain.Page<OrderService.OrderInfo>> getCustomerOrders(
            @PathVariable String customerId,
            org.springframework.data.domain.Pageable pageable) {
        
        org.springframework.data.domain.Page<OrderService.OrderInfo> orders = orderService.getOrdersByCustomer(customerId, pageable);
        return ResponseEntity.ok(orders);
    }

    record CreateOrderResponse(String orderId) {}
}
