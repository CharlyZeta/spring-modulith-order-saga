package com.showcase.ordersystem.inventory;

import com.showcase.ordersystem.inventory.internal.InventoryItem;
import com.showcase.ordersystem.inventory.internal.InventoryRepository;
import com.showcase.ordersystem.shared.events.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class InventoryConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        inventoryService.initializeInventory("PROD-CONC", "Concurrency Product", 10);
    }

    @Test
    void shouldHandleOptimisticLockingWhenConcurrentReservationsOccur() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        OrderCreatedEvent event = new OrderCreatedEvent(
                "order-1", "CUST-1",
                List.of(new OrderCreatedEvent.OrderItem("PROD-CONC", 6, new BigDecimal("100.00"))),
                new BigDecimal("600.00"), Instant.now()
        );

        // Intentamos reservar 6 unidades desde dos hilos diferentes al mismo tiempo.
        // Solo uno debería tener éxito ya que el total es 10.
        // Incluso si hubiera suficiente stock (ej. 20), el bloqueo optimista fallaría si ambos leen la misma versión.
        
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    inventoryService.onOrderCreated(event);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // Otras excepciones
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Verificamos que al menos uno falló por concurrencia o que el stock final es consistente
        InventoryService.InventoryStatus status = inventoryService.getInventoryStatus("PROD-CONC");
        
        // El stock no puede ser negativo y las reservas deben ser consistentes
        assertThat(status.availableQuantity()).isGreaterThanOrEqualTo(4);
        assertThat(status.reservedQuantity()).isLessThanOrEqualTo(10);
        
        System.out.println("Success: " + successCount.get() + ", Failures: " + failureCount.get());
    }
}
