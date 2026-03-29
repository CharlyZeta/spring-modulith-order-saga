package com.showcase.ordersystem.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for inventory management.
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeInventory(@RequestBody InitInventoryRequest request) {
        inventoryService.initializeInventory(request.productId(), request.productName(), request.quantity());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryService.InventoryStatus> getInventoryStatus(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getInventoryStatus(productId));
    }

    public record InitInventoryRequest(
            String productId,
            String productName,
            int quantity
    ) {}
}
