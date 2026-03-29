package com.showcase.ordersystem.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
    
    @Lock(LockModeType.OPTIMISTIC)
    Optional<InventoryItem> findByProductId(String productId);
}
