# Modular Order System - Spring Modulith Showcase 🚀

Este proyecto es una demostración de nivel profesional sobre cómo construir **Monolitos Modulares** modernos utilizando **Spring Modulith**, **RabbitMQ** y principios de **Domain-Driven Design (DDD)**. 

El objetivo es mostrar una alternativa viable y eficiente a los microservicios, manteniendo la agilidad de desarrollo sin sacrificar la separación de conceptos, la observabilidad y la escalabilidad futura.

## 🏗️ Arquitectura del Sistema

El sistema utiliza un enfoque de **Saga Pattern** coreografiada mediante eventos de dominio para gestionar un flujo de pedidos complejo entre tres módulos principales:

1.  **Orders Module**: Orquesta el ciclo de vida del pedido (`PENDING`, `COMPLETED`, `CANCELLED`).
2.  **Inventory Module**: Gestiona el stock de forma atómica. Implementa reserva atómica para evitar estados inconsistentes (reservas parciales).
3.  **Notifications Module**: Envía alertas resilientes consumiendo eventos de éxito y fallos.

### 🌟 Características Destacadas

*   **Modular Monolith**: Separación física y lógica de módulos validada por Spring Modulith.
*   **Sagas con Integridad Garantizada**: Implementación de transacciones compensatorias. Si la reserva de inventario falla, el pedido se cancela automáticamente.
*   **Resiliencia Avanzada**: Reintentos automáticos de eventos fallidos mediante el Event Publication Registry de Modulith.
*   **Observabilidad End-to-End**: Propagación de `traceId` (Micrometer Tracing) a través de RabbitMQ para trazabilidad completa entre módulos.
*   **Idempotencia**: Gestión de peticiones duplicadas mediante `idempotency_key` en la capa de persistencia (SQL).

## 🛠️ Stack Tecnológico

*   **Java 21** (con soporte para Virtual Threads habilitado).
*   **Spring Boot 3.4.1**.
*   **Spring Modulith** (Verificación de módulos y Event Publication Registry).
*   **RabbitMQ** (Mensajería para integraciones externas y DLQs).
*   **PostgreSQL 15** & **Flyway** (Gestión de esquema).
*   **Micrometer & Zipkin** (Trazabilidad distribuida).
*   **JUnit 5, AssertJ, Awaitility** (Testing robusto de flujos asíncronos).

## 🚀 Cómo ejecutarlo

1.  **Levantar infraestructura**: 
    ```bash
    docker-compose up -d
    ```
2.  **Compilar y Ejecutar**: 
    ```bash
    ./mvnw spring-boot:run
    ```
3.  **Explorar la API**:
    *   **Swagger UI**: Accede a [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
    *   **Métricas (Prometheus)**: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
    *   **Trazas (Zipkin)**: [http://localhost:9411](http://localhost:9411)

---

## 📐 Decisiones de Ingeniería (ADRs)

*   **Event-Driven Communication**: Se utiliza el bus de eventos interno de Spring para comunicación entre módulos, asegurando un bajo acoplamiento.
*   **Transactional Outbox**: Aprovechamos el registro de publicación de eventos de Modulith para garantizar que ningún evento se pierda si el sistema falla tras una transacción.
*   **Atomic Reservations**: El módulo de inventario procesa reservas en una sola transacción `REQUIRES_NEW`, permitiendo revertir cambios locales sin afectar la publicación del evento de fallo a la Saga.

---
Desarrollado como un showcase de arquitectura moderna en el ecosistema Spring. 🌍
