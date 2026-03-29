# MODULAR ORDER SYSTEM - MASTER PROMPT FOR IDE CONTINUATION

## PROJECT CONTEXT & OBJECTIVES

You are working on a **production-grade Java Spring Boot showcase project** that demonstrates modern microservices architecture patterns. This project serves as a portfolio piece to highlight expertise in:

- **Spring Modulith**: Modular monolith architecture with enforced boundaries
- **Domain-Driven Design (DDD)**: Bounded contexts, domain events, aggregate roots
- **Asynchronous Messaging**: RabbitMQ for inter-module and external communication
- **Clean Architecture**: Clear separation between API, domain logic, and infrastructure
- **Event-Driven Architecture**: Loose coupling via domain events
- **Modern Java Practices**: Records, Java 17+ features, functional programming

**Target Audience**: Hiring managers and technical recruiters evaluating backend Java skills for senior/lead positions.

---

## ARCHITECTURAL DECISIONS & PRINCIPLES

### 1. Spring Modulith Architecture

**Module Structure:**
```
com.showcase.ordersystem/
├── orders/                    # Order management bounded context
│   ├── OrderService.java      # PUBLIC API (visible to other modules)
│   ├── OrderController.java   # PUBLIC API (REST endpoints)
│   └── internal/              # PRIVATE implementation details
│       ├── Order.java         # Entity (JPA)
│       ├── OrderItem.java     # Entity (JPA)
│       ├── OrderStatus.java   # Enum
│       └── OrderRepository.java
├── inventory/                 # Inventory management bounded context
│   ├── InventoryService.java  # PUBLIC API
│   └── internal/
│       ├── InventoryItem.java
│       └── InventoryRepository.java
├── notifications/             # Notification delivery bounded context
│   ├── NotificationService.java # PUBLIC API
│   └── internal/
│       └── (future: notification persistence)
└── shared/
    └── events/                # Domain events (cross-module contracts)
        ├── OrderCreatedEvent.java
        ├── InventoryReservedEvent.java
        └── OrderCompletedEvent.java
```

**Key Principles:**
- **Package-Private by Default**: All `internal/` classes are package-private (no public modifier)
- **Public API Layer**: Only classes in module root package are public
- **Event-Based Communication**: Modules communicate ONLY via domain events, never direct method calls
- **No Circular Dependencies**: Spring Modulith will verify this at compile time

### 2. Event Flow (Saga Pattern)

```
┌──────────────┐
│   Customer   │
└──────┬───────┘
       │ POST /api/orders
       ▼
┌──────────────────────────────────────────────────────────────┐
│ ORDERS MODULE                                                │
│  • OrderService.createOrder()                                │
│  • Saves Order (status: PENDING)                            │
│  • Publishes: OrderCreatedEvent                             │
└──────┬───────────────────────────────────────────────────────┘
       │ Spring Modulith async event
       ▼
┌──────────────────────────────────────────────────────────────┐
│ INVENTORY MODULE                                             │
│  • InventoryService.onOrderCreated()                        │
│  • Attempts inventory reservation                           │
│  • Publishes: InventoryReservedEvent (success/failure)      │
└──────┬───────────────────────────────────────────────────────┘
       │ Spring Modulith async event
       ▼
┌──────────────────────────────────────────────────────────────┐
│ ORDERS MODULE                                                │
│  • OrderService.onInventoryReserved()                       │
│  • If success: Updates Order (status: COMPLETED)            │
│  • If failure: Updates Order (status: CANCELLED)            │
│  • Publishes: OrderCompletedEvent (if success)              │
└──────┬───────────────────────────────────────────────────────┘
       │ Spring Modulith async event
       ▼
┌──────────────────────────────────────────────────────────────┐
│ NOTIFICATIONS MODULE                                         │
│  • NotificationService.onOrderCompleted()                   │
│  • Sends notification message to RabbitMQ                   │
│  • External service consumes from RabbitMQ queue            │
└──────────────────────────────────────────────────────────────┘
```

**Event Handling Pattern:**
- Use `@ApplicationModuleListener` for async, transactional event processing
- Spring Modulith stores events in `event_publication` table for guaranteed delivery
- Failed events can be retried automatically

### 3. Data Model

**Orders Module:**
```sql
CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- PENDING, INVENTORY_RESERVED, COMPLETED, CANCELLED
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    version BIGINT  -- Optimistic locking
);

CREATE TABLE order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
    product_id VARCHAR(36) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);
```

**Inventory Module:**
```sql
CREATE TABLE inventory_items (
    product_id VARCHAR(36) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    version BIGINT  -- Optimistic locking for concurrency
);
```

**Spring Modulith Event Store:**
```sql
CREATE TABLE event_publication (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    serialized_event TEXT NOT NULL,
    completion_date TIMESTAMP
);
```

---

## COMPLETED COMPONENTS (Already Implemented)

### ✅ Core Domain Model
- [x] `Order` entity with JPA annotations, lifecycle methods
- [x] `OrderItem` entity with bidirectional relationship
- [x] `OrderStatus` enum
- [x] `InventoryItem` entity with optimistic locking
- [x] Domain events as Java records: `OrderCreatedEvent`, `InventoryReservedEvent`, `OrderCompletedEvent`

### ✅ Service Layer
- [x] `OrderService` with `createOrder()`, `onInventoryReserved()` event listener
- [x] `InventoryService` with `onOrderCreated()` event listener, inventory reservation logic
- [x] `NotificationService` with `onOrderCompleted()` event listener, RabbitMQ integration

### ✅ REST API
- [x] `OrderController` with POST `/api/orders` and GET `/api/orders/customer/{customerId}`

### ✅ Maven Configuration
- [x] `pom.xml` with Spring Boot 3.2.5, Spring Modulith 1.1.4, PostgreSQL, RabbitMQ, TestContainers

---

## PENDING IMPLEMENTATION (What You Need to Build)

### 1. RabbitMQ Configuration

**File: `src/main/java/com/showcase/ordersystem/infrastructure/RabbitMQConfig.java`**

Requirements:
- Create exchange: `notifications.exchange` (Topic Exchange)
- Create queue: `notification.email.queue` (Durable)
- Bind queue to exchange with routing key: `notification.email`
- Configure JSON message converter (`Jackson2JsonMessageConverter`)
- Add retry policy for failed messages (3 retries with exponential backoff)
- Dead letter queue: `notification.email.dlq`

Example structure:
```java
@Configuration
public class RabbitMQConfig {
    
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange("notifications.exchange", true, false);
    }
    
    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable("notification.email.queue")
            .withArgument("x-dead-letter-exchange", "notifications.dlx")
            .withArgument("x-dead-letter-routing-key", "notification.email.dlq")
            .build();
    }
    
    @Bean
    public Binding emailNotificationBinding() {
        return BindingBuilder
            .bind(emailNotificationQueue())
            .to(notificationsExchange())
            .with("notification.email");
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // Add DLQ, retry configuration...
}
```

### 2. Application Configuration

**File: `src/main/resources/application.yml`**

Required properties:
```yaml
spring:
  application:
    name: modular-order-system
  
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: orderuser
    password: orderpass
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update  # Use Flyway/Liquibase in production
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 3000
          max-attempts: 3
          multiplier: 2.0
  
  modulith:
    events:
      jdbc:
        schema-initialization:
          enabled: true

logging:
  level:
    com.showcase.ordersystem: DEBUG
    org.springframework.modulith: DEBUG
```

**File: `src/main/resources/application-test.yml`** (for TestContainers)

### 3. Docker Compose

**File: `docker-compose.yml`**

Services needed:
- **PostgreSQL 15**: Port 5432, database `orderdb`, user `orderuser`
- **RabbitMQ 3-management**: Ports 5672 (AMQP), 15672 (Management UI)
- **Application container** (optional): Build from Dockerfile

Include:
- Volume mounts for data persistence
- Health checks for all services
- Network configuration
- Environment variables

### 4. Database Migration (Flyway or Liquibase)

**File: `src/main/resources/db/migration/V1__initial_schema.sql`**

Create all tables with proper indexes:
- Index on `orders.customer_id`
- Index on `orders.status`
- Index on `order_items.order_id`
- Unique constraint on `inventory_items.product_id`

### 5. Testing Suite

#### A. Module Verification Test

**File: `src/test/java/com/showcase/ordersystem/ModulithArchitectureTest.java`**

```java
@SpringBootTest
class ModulithArchitectureTest {
    
    @Test
    void verifyModularStructure() {
        ApplicationModules.of(ModularOrderSystemApplication.class)
            .verify();  // Fails if module boundaries are violated
    }
    
    @Test
    void documentModules() throws IOException {
        ApplicationModules modules = ApplicationModules.of(ModularOrderSystemApplication.class);
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml();
    }
}
```

#### B. Integration Test with TestContainers

**File: `src/test/java/com/showcase/ordersystem/OrderFlowIntegrationTest.java`**

Test complete order flow:
1. Create order via REST API
2. Verify `OrderCreatedEvent` published
3. Verify inventory reservation
4. Verify `InventoryReservedEvent` published
5. Verify order completion
6. Verify `OrderCompletedEvent` published
7. Verify RabbitMQ message sent

Use:
- `@SpringBootTest`
- `@Testcontainers` with PostgreSQL and RabbitMQ containers
- `@Transactional` for test isolation
- `ApplicationEvents` recorder from Spring Modulith

#### C. Unit Tests

- `OrderServiceTest`: Mock `OrderRepository`, verify domain logic
- `InventoryServiceTest`: Test reservation logic, optimistic locking
- `NotificationServiceTest`: Verify RabbitMQ message sending

### 6. REST API Enhancements

**Additional endpoints needed:**

```java
// OrderController additions
@GetMapping("/{orderId}")
ResponseEntity<OrderDetails> getOrderById(@PathVariable String orderId)

@GetMapping
ResponseEntity<Page<OrderInfo>> getAllOrders(Pageable pageable)
```

**New Controller: `InventoryController.java`**
```java
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    @PostMapping
    ResponseEntity<Void> initializeInventory(@RequestBody InitInventoryRequest request)
    
    @GetMapping("/{productId}")
    ResponseEntity<InventoryStatus> getInventoryStatus(@PathVariable String productId)
}
```

### 7. Exception Handling

**File: `src/main/java/com/showcase/ordersystem/infrastructure/GlobalExceptionHandler.java`**

Handle:
- `EntityNotFoundException`
- `OptimisticLockException`
- `InsufficientInventoryException` (custom)
- `ValidationException`
- Generic `RuntimeException`

Return RFC 7807 Problem Details:
```json
{
  "type": "https://api.example.com/errors/insufficient-inventory",
  "title": "Insufficient Inventory",
  "status": 400,
  "detail": "Product XYZ has only 5 units available, requested 10",
  "instance": "/api/orders/123"
}
```

### 8. Observability

**Add dependencies:**
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

**Configure endpoints:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,modulith
  metrics:
    export:
      prometheus:
        enabled: true
```

**Custom metrics:**
- Counter: `orders.created.total`
- Counter: `inventory.reservations.failed.total`
- Timer: `order.creation.duration`

### 9. API Documentation

**Add Swagger/OpenAPI:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

**Configure:**
```java
@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Modular Order System API")
                .version("1.0.0")
                .description("Spring Modulith + RabbitMQ showcase"));
    }
}
```

### 10. Data Initialization

**File: `src/main/java/com/showcase/ordersystem/infrastructure/DataInitializer.java`**

```java
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    
    private final InventoryService inventoryService;
    
    @Override
    public void run(ApplicationArguments args) {
        // Initialize sample products
        inventoryService.initializeInventory("PROD-001", "Laptop", 50);
        inventoryService.initializeInventory("PROD-002", "Mouse", 200);
        inventoryService.initializeInventory("PROD-003", "Keyboard", 100);
    }
}
```

### 11. Professional README

**File: `README.md`**

Must include:
- **Project Overview**: What it demonstrates, target audience
- **Architecture Diagram**: PlantUML or Mermaid showing modules and event flow
- **Technology Stack**: Spring Boot 3.2, Spring Modulith, PostgreSQL, RabbitMQ
- **Key Features**: List with checkmarks
- **Prerequisites**: Java 17+, Docker, Maven
- **Quick Start**: 
  ```bash
  docker-compose up -d
  mvn spring-boot:run
  ```
- **API Examples**: curl commands for creating orders
- **Testing**: How to run tests
- **Monitoring**: Access RabbitMQ UI (http://localhost:15672), Actuator endpoints
- **Module Documentation**: Link to generated PlantUML diagrams
- **Design Decisions**: Why Spring Modulith, why events over REST calls
- **Future Enhancements**: Kafka support, CQRS, Event Sourcing

### 12. Additional Files

**`.gitignore`**:
```
target/
.idea/
*.iml
.DS_Store
*.log
```

**`Dockerfile`** (multi-stage build):
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`package-info.java`** for each module (for Spring Modulith documentation):
```java
/**
 * Orders module - Manages order lifecycle and orchestrates the order fulfillment saga.
 * 
 * <p>Published Events:</p>
 * <ul>
 *   <li>{@link OrderCreatedEvent} - When a new order is placed</li>
 *   <li>{@link OrderCompletedEvent} - When order is successfully completed</li>
 * </ul>
 * 
 * <p>Consumed Events:</p>
 * <ul>
 *   <li>{@link InventoryReservedEvent} - From inventory module</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule
package com.showcase.ordersystem.orders;
```

---

## TECHNICAL GUIDELINES

### Code Quality Standards

1. **Naming Conventions:**
   - Events: Past tense (OrderCreatedEvent, not OrderCreateEvent)
   - DTOs: Nested records within service classes
   - Entities: Singular nouns (Order, not Orders)

2. **Logging:**
   - Use SLF4J with Lombok's `@Slf4j`
   - Log at entry/exit of event listeners
   - Log business-critical operations (order creation, inventory reservation)
   - Never log sensitive data (passwords, credit cards)

3. **Error Handling:**
   - Throw domain-specific exceptions
   - Use `@ControllerAdvice` for global exception handling
   - Always include contextual information in exceptions

4. **Testing:**
   - Minimum 80% code coverage
   - Test all event listeners
   - Test transaction boundaries
   - Test optimistic locking scenarios

5. **Performance:**
   - Use `@Transactional(readOnly = true)` for queries
   - Implement pagination for list endpoints
   - Add database indexes on foreign keys and frequently queried columns

### Security Considerations (Future)

- Add Spring Security with OAuth2
- Implement rate limiting
- Add input validation with `@Valid`
- Sanitize error messages (don't leak stack traces)

---

## EXAMPLE USAGE SCENARIOS

### Scenario 1: Successful Order
```bash
# 1. Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "customer@example.com",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Laptop",
        "quantity": 2,
        "unitPrice": 999.99
      }
    ]
  }'

# Response: {"orderId": "550e8400-e29b-41d4-a716-446655440000"}

# 2. Check order status
curl http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000

# Response: {"status": "COMPLETED", ...}

# 3. Verify RabbitMQ message
# Login to http://localhost:15672 (guest/guest)
# Check queue: notification.email.queue
# Should see 1 message
```

### Scenario 2: Insufficient Inventory
```bash
# Order quantity exceeds available stock
# Expected: Order created with status CANCELLED
# InventoryReservedEvent.success = false
# No notification sent
```

---

## DELIVERABLES CHECKLIST

- [ ] RabbitMQ configuration with exchanges, queues, bindings
- [ ] Docker Compose with PostgreSQL + RabbitMQ
- [ ] application.yml with all required properties
- [ ] Flyway migration scripts for database schema
- [ ] Module verification test
- [ ] Integration test with TestContainers
- [ ] Unit tests for all services
- [ ] Additional REST endpoints (GET order, inventory management)
- [ ] Global exception handler with RFC 7807
- [ ] Actuator + Prometheus metrics
- [ ] Swagger/OpenAPI documentation
- [ ] Data initializer for demo products
- [ ] Professional README with architecture diagrams
- [ ] Dockerfile and .gitignore
- [ ] package-info.java for each module

---

## SUCCESS CRITERIA

This project is complete when:

1. ✅ All tests pass (module verification + integration + unit)
2. ✅ `docker-compose up` starts all services successfully
3. ✅ Can create order via REST API and see complete event flow in logs
4. ✅ RabbitMQ message appears in queue after order completion
5. ✅ Swagger UI accessible at http://localhost:8080/swagger-ui.html
6. ✅ README has clear instructions and architecture diagram
7. ✅ Code coverage > 80%
8. ✅ No module boundary violations (verified by Spring Modulith test)

---

## ADDITIONAL NOTES

- **Why Spring Modulith?** It's a stepping stone between monolith and microservices. Shows understanding of modular architecture without operational complexity of distributed systems.

- **Why RabbitMQ?** Demonstrates async messaging patterns, essential for microservices. Shows you can decouple modules and integrate with external systems.

- **Portfolio Impact:** This project shows:
  - Modern Java practices (Records, Java 17)
  - DDD and clean architecture
  - Event-driven design
  - Testing discipline (unit + integration + architecture)
  - DevOps readiness (Docker, metrics, health checks)
  - Production awareness (error handling, logging, observability)

**This is not a toy project. This is enterprise-grade code.**

---

## GETTING STARTED IN YOUR IDE

1. Import the existing project (pom.xml already created)
2. Start with RabbitMQ configuration (needed for NotificationService to work)
3. Create docker-compose.yml and test `docker-compose up`
4. Add application.yml configuration
5. Write and run ModulithArchitectureTest - should PASS
6. Write integration test - verify complete flow
7. Add remaining endpoints and exception handling
8. Generate documentation and diagrams
9. Write README
10. Push to GitHub with professional presentation

---

Good luck! This project will demonstrate your expertise far better than listing "Spring Boot, RabbitMQ" on a CV. Show, don't tell.
