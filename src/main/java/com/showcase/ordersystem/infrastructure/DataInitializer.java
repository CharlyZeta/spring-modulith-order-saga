package com.showcase.ordersystem.infrastructure;

import com.showcase.ordersystem.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes demo data for the application.
 * Runs after application context is fully initialized.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final InventoryService inventoryService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing demo inventory data...");

        try {
            inventoryService.initializeInventory("PROD-001", "Laptop", 50);
            inventoryService.initializeInventory("PROD-002", "Mouse", 200);
            inventoryService.initializeInventory("PROD-003", "Keyboard", 100);
            inventoryService.initializeInventory("PROD-004", "Monitor", 30);
            inventoryService.initializeInventory("PROD-005", "Headset", 75);

            log.info("Demo inventory data initialized successfully");
        } catch (Exception e) {
            log.warn("Could not initialize demo data (may already exist): {}", e.getMessage());
        }
    }
}
