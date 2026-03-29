# Modular Order System - Spring Modulith Showcase

This project is a production-grade demonstration of **Modular Monolith** architecture using **Spring Modulith**, **RabbitMQ**, and **Domain-Driven Design (DDD)** principles.

## 🚀 Key Features

- **Modular Architecture**: Strong boundaries enforced by Spring Modulith.
- **Event-Driven Saga**: Decoupled inter-module communication using asynchronous domain events.
- **External Messaging**: Integration with RabbitMQ for shipping notifications.
- **Production-Ready Observability**: Actuator, Prometheus metrics, and JSON logging.
- **API Documentation**: Automated Swagger/OpenAPI 3.0 documentation.
- **Resilient Infrastructure**: PostgreSQL with Flyway migrations and RabbitMQ with DLQ/Retries.

## 🏗 Architecture Overview

The system is divided into three main bounded contexts:

1.  **Orders Module**: Orchestrates the order lifecycle.
2.  **Inventory Module**: Manages stock levels and reservations.
3.  **Notifications Module**: Handles external communication via RabbitMQ.

### Event Flow (Saga)

```mermaid
sequenceDiagram
    participant C as Customer
    participant O as Orders Module
    participant I as Inventory Module
    participant N as Notifications Module
    participant R as RabbitMQ (External)

    C->>O: POST /api/orders
    Note over O: Save Order (STATUS: PENDING)
    O-->>I: OrderCreatedEvent (Async)
    
    rect rgb(240, 240, 240)
        Note over I: Check & Reserve Stock
        I-->>O: InventoryReservedEvent (Success/Failure)
    end

    alt Success
        Note over O: Update Order (STATUS: COMPLETED)
        O-->>N: OrderCompletedEvent (Async)
        N->>R: Send Notification Message
    else Failure
        Note over O: Update Order (STATUS: CANCELLED)
    end
    
    O->>C: HTTP 200 (Order ID)
```

1.  `OrderService` creates an order...

## 🛠 Tech Stack

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Modulith](https://img.shields.io/badge/Spring_Modulith-1.3.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Apache_Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-316192?style=for-the-badge&logo=docker&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=white)

- **Language**: Java 21 (Records, Functional Programming)
- **Framework**: Spring Boot 3.4.1 + Spring Modulith
- **Messaging**: RabbitMQ (Asynchronous Saga Pattern)
- **Persistence**: PostgreSQL + Hibernate JPA + Flyway
- **Observability**: Micrometer + Prometheus + Spring Actuator
- **Testing**: JUnit 5 + Mockito + Testcontainers
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Containerization**: Docker & Multi-stage Dockerfile


## 🚦 Getting Started

### Prerequisites

- Docker and Docker Compose
- JDK 21
- Maven

### Running the Infrastructure

```bash
docker-compose up -d
```

### Running the Application

```bash
./mvnw spring-boot:run
```

## 📖 API Documentation

Once the application is running, you can access:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Actuator Health**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Prometheus Metrics**: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)

## 🧪 Testing

The project includes architecture verification and full-flow integration tests:

```bash
./mvnw test
```

- `ModulithArchitectureTest`: Verifies module boundaries and generates PlantUML diagrams.
- `OrderFlowIntegrationTest`: End-to-end saga validation using Testcontainers.

## 📂 Project Structure

```text
com.showcase.ordersystem/
├── orders/           # Public API & Service
│   └── internal/     # Private Entities & Repository
├── inventory/        # Public API & Service
│   └── internal/     # Private Entities & Repository
├── notifications/    # External Messaging Service
├── infrastructure/   # Global Config (RabbitMQ, Exceptions)
└── shared/           # Cross-module Domain Events
```
