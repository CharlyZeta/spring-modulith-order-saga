package com.showcase.ordersystem.orders.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    Page<Order> findByCustomerId(String customerId, Pageable pageable);
    
    List<Order> findByStatus(OrderStatus status);
}
