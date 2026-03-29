package com.showcase.ordersystem.inventory.internal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "inventory_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Version
    private Long version; // Optimistic locking

    private Instant lastUpdated;

    public boolean canReserve(int quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException(
                "Insufficient inventory for product: " + productId
            );
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        this.lastUpdated = Instant.now();
    }

    public void releaseReservation(int quantity) {
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        this.lastUpdated = Instant.now();
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        lastUpdated = Instant.now();
    }
}
