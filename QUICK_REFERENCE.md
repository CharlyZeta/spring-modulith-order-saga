# QUICK REFERENCE - IDE AI Assistant Commands

Use these prompts directly with your IDE AI assistant (Cursor, GitHub Copilot, etc.)

---

## Phase 1: Configuration Setup

### RabbitMQ Configuration
```
Create RabbitMQConfig.java in com.showcase.ordersystem.infrastructure with:
- Topic exchange "notifications.exchange"
- Durable queue "notification.email.queue"
- Binding with routing key "notification.email"
- Dead letter queue "notification.email.dlq"
- Jackson2JsonMessageConverter
- Retry policy: 3 attempts, exponential backoff (initial 3s, multiplier 2.0)
Follow Spring AMQP best practices.
```

### Docker Compose
```
Create docker-compose.yml with:
- PostgreSQL 15 (port 5432, db: orderdb, user: orderuser, pass: orderpass)
- RabbitMQ 3-management (ports 5672 AMQP, 15672 UI)
- Persistent volumes for both
- Health checks for both services
- Network: order-network
Use production-ready configuration.
```

### Application Properties
```
Create application.yml with:
- Spring datasource config for PostgreSQL
- JPA config (Hibernate, show SQL, dialect)
- RabbitMQ config (host, port, credentials, retry policy)
- Spring Modulith events JDBC initialization
- Logging levels (DEBUG for com.showcase.ordersystem, org.springframework.modulith)
Also create application-test.yml for TestContainers.
```

---

## Phase 2: Database Migration

### Flyway Migration
```
Create V1__initial_schema.sql in src/main/resources/db/migration with:
- orders table (id, customer_id, customer_email, total_amount, status, created_at, completed_at, version)
- order_items table (id, order_id FK, product_id, product_name, quantity, unit_price, total_price)
- inventory_items table (product_id PK, product_name, available_quantity, reserved_quantity, last_updated, version)
Add indexes on: orders.customer_id, orders.status, order_items.order_id
Use PostgreSQL syntax with proper constraints.
```

---

## Phase 3: Testing

### Module Verification Test
```
Create ModulithArchitectureTest.java that:
- Uses @SpringBootTest
- Has verifyModularStructure() test that calls ApplicationModules.of().verify()
- Has documentModules() test that generates PlantUML diagrams
- Fails if module boundaries are violated
Follow Spring Modulith testing guide.
```

### Integration Test
```
Create OrderFlowIntegrationTest.java that:
- Uses @SpringBootTest, @Testcontainers
- Starts PostgreSQL and RabbitMQ containers
- Tests complete order flow:
  1. POST /api/orders (create order)
  2. Verify OrderCreatedEvent published
  3. Verify inventory reservation
  4. Verify InventoryReservedEvent published
  5. Verify order status changed to COMPLETED
  6. Verify OrderCompletedEvent published
  7. Verify RabbitMQ message in notification.email.queue
- Use ApplicationEvents recorder from Spring Modulith
- Use @Transactional for test isolation
```

### Service Unit Tests
```
Create OrderServiceTest.java with:
- Mock OrderRepository
- Test createOrder() with valid data
- Test createOrder() calculates total correctly
- Test onInventoryReserved() with success scenario
- Test onInventoryReserved() with failure scenario
- Verify event publication with ApplicationEventPublisher mock
Use Mockito and JUnit 5.
```

---

## Phase 4: REST API Enhancements

### Extended OrderController
```
Add to OrderController.java:
- GET /api/orders/{orderId} - returns OrderDetails DTO
- GET /api/orders - returns Page<OrderInfo> with pagination
Handle EntityNotFoundException with proper HTTP status.
```

### InventoryController
```
Create InventoryController.java in inventory package with:
- POST /api/inventory - initializeInventory (productId, productName, quantity)
- GET /api/inventory/{productId} - getInventoryStatus
- PUT /api/inventory/{productId}/adjust - adjustInventory (delta)
Return proper HTTP status codes.
```

---

## Phase 5: Cross-Cutting Concerns

### Global Exception Handler
```
Create GlobalExceptionHandler.java with @ControllerAdvice that handles:
- EntityNotFoundException → 404
- OptimisticLockException → 409
- InsufficientInventoryException (custom) → 400
- ConstraintViolationException → 400
- Generic Exception → 500
Return RFC 7807 Problem Details format with type, title, status, detail, instance.
```

### Custom Exception
```
Create InsufficientInventoryException.java in inventory package:
- Extends RuntimeException
- Constructor takes productId, requested, available
- Provides detailed message
```

### Data Initializer
```
Create DataInitializer.java that implements ApplicationRunner:
- Injects InventoryService
- In run() method, initializes 5 sample products with inventory
- Use @Profile("!test") to skip in tests
```

---

## Phase 6: Observability

### Actuator Configuration
```
Add to pom.xml:
- spring-boot-starter-actuator
- micrometer-registry-prometheus

Add to application.yml:
- management.endpoints.web.exposure.include: health,metrics,prometheus,modulith
- management.metrics.export.prometheus.enabled: true
```

### Custom Metrics
```
Add to OrderService:
- MeterRegistry injection
- Counter "orders.created.total" incremented on createOrder()
- Timer "order.creation.duration" wrapping createOrder()

Add to InventoryService:
- Counter "inventory.reservations.failed.total" incremented on reservation failure
```

---

## Phase 7: Documentation

### OpenAPI Configuration
```
Add springdoc-openapi-starter-webmvc-ui dependency.
Create OpenAPIConfig.java with:
- Custom OpenAPI bean
- Info: title "Modular Order System API", version "1.0.0"
- Description highlighting Spring Modulith and event-driven architecture
- Contact info (optional)
```

### Module Documentation
```
Create package-info.java for each module (orders, inventory, notifications):
- @ApplicationModule annotation
- JavaDoc describing module purpose
- List of published events
- List of consumed events
See Spring Modulith documentation guide.
```

### README
```
Create README.md with:
1. Project Overview (1-2 paragraphs)
2. Architecture Diagram (Mermaid or PlantUML)
3. Technology Stack (bulleted list)
4. Key Features (checkboxes)
5. Prerequisites
6. Quick Start (docker-compose up, mvn spring-boot:run)
7. API Examples (curl commands)
8. Testing Instructions
9. Monitoring (RabbitMQ UI, Actuator endpoints)
10. Design Decisions section
11. Future Enhancements
Make it visually appealing with emojis and badges.
```

---

## Phase 8: DevOps

### Dockerfile
```
Create multi-stage Dockerfile:
Stage 1: Build with Maven (eclipse-temurin:17-jdk-alpine)
Stage 2: Runtime with JRE (eclipse-temurin:17-jre-alpine)
EXPOSE 8080
HEALTHCHECK with curl to /actuator/health
```

### .gitignore
```
Create .gitignore with:
target/, .idea/, *.iml, .DS_Store, *.log, .env, docker-compose.override.yml
```

### GitHub Actions (Optional)
```
Create .github/workflows/ci.yml:
- Trigger on push/PR
- Job: build and test
- Steps: checkout, setup Java 17, build with Maven, run tests
- Upload test coverage report
```

---

## Validation Commands

After implementation, verify with these commands:

```bash
# 1. Verify module architecture
mvn test -Dtest=ModulithArchitectureTest

# 2. Run all tests
mvn clean test

# 3. Start infrastructure
docker-compose up -d

# 4. Start application
mvn spring-boot:run

# 5. Create test order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "test@example.com",
    "items": [{
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 999.99
    }]
  }'

# 6. Check Swagger UI
open http://localhost:8080/swagger-ui.html

# 7. Check RabbitMQ Management
open http://localhost:15672 (guest/guest)

# 8. Check metrics
curl http://localhost:8080/actuator/prometheus
```

---

## Common AI Assistant Prompts

### Generate boilerplate
```
"Generate a REST controller for [Entity] with CRUD endpoints following the same pattern as OrderController"
```

### Fix test
```
"This test is failing: [paste error]. Fix it while maintaining test coverage."
```

### Add feature
```
"Add order cancellation feature: new endpoint DELETE /api/orders/{id}, publish OrderCancelledEvent, inventory module listens and releases reservation"
```

### Refactor
```
"Extract this logic into a separate service following single responsibility principle"
```

### Documentation
```
"Generate JavaDoc for this class explaining its role in the Spring Modulith architecture"
```

---

## Pro Tips for IDE AI

1. **Be specific about patterns**: "Follow the repository pattern", "Use builder pattern for this DTO"
2. **Reference existing code**: "Create InventoryServiceTest similar to OrderServiceTest"
3. **Specify constraints**: "Use Java 17 records, not classes with getters/setters"
4. **Ask for explanations**: "Explain why we use @ApplicationModuleListener vs @EventListener"
5. **Iterative refinement**: Start simple, then ask to add error handling, logging, metrics
6. **Use architecture terms**: "This violates module boundaries, refactor to use events"

---

## Checkpoint Verification

After each phase, verify:

✅ Phase 1: `docker-compose up -d` succeeds, app starts without errors
✅ Phase 2: Database tables created, `mvn flyway:info` shows applied migrations
✅ Phase 3: All tests pass, PlantUML diagrams generated
✅ Phase 4: All endpoints return expected responses
✅ Phase 5: Exception scenarios return proper error responses
✅ Phase 6: Metrics visible at /actuator/prometheus
✅ Phase 7: Swagger UI renders all endpoints
✅ Phase 8: Docker image builds successfully

---

This is your execution roadmap. Work phase by phase. Commit after each phase completion.
