package com.showcase.ordersystem.infrastructure;

import com.showcase.ordersystem.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final InventoryService inventoryService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing sample inventory data...");
        try {
            inventoryService.initializeInventory("PROD-001", "MacBook Pro M3", 50);
            inventoryService.initializeInventory("PROD-002", "Logitech MX Master 3", 200);
            inventoryService.initializeInventory("PROD-003", "Keychron MX Mechanical", 100);
            log.info("Sample inventory data initialized successfully.");
        } catch (Exception e) {
            log.warn("Inventory initialization skipped (data might already exist): {}", e.getMessage());
        }
    }
}