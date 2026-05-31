# Modular Order System - Spring Modulith Showcase 🚀

Este proyecto es una demostración de nivel profesional sobre cómo construir **Monolitos Modulares** modernos utilizando **Spring Modulith**, **RabbitMQ** y principios de **Domain-Driven Design (DDD)**. 

El objetivo es mostrar una alternativa viable y eficiente a los microservicios, manteniendo la agilidad de desarrollo sin sacrificar la separación de conceptos y la escalabilidad futura.

## 🏗️ Arquitectura del Sistema

El sistema utiliza un enfoque de **Saga Pattern** coreografiada mediante eventos de dominio para gestionar un flujo de pedidos complejo:

1.  **Orders Module**: Orquesta el ciclo de vida del pedido.
2.  **Inventory Module**: Gestiona el stock de forma atómica y reactiva.
3.  **Notifications Module**: Envía alertas resilientes vía RabbitMQ.

### 🌟 Características Destacadas

*   **Modular Monolith**: Separación física y lógica de módulos validada por Spring Modulith.
*   **Sagas con Integridad Garantizada**: Implementación de transacciones compensatorias y reserva atómica de inventario (evitando el bug de reserva parcial).
*   **Resiliencia Avanzada**: Reintentos automáticos de eventos fallidos mediante el Event Publication Registry de Modulith.
*   **Observabilidad End-to-End**: Propagación de `traceId` (Micrometer Tracing) a través de RabbitMQ para trazabilidad completa.
*   **Idempotencia**: Gestión de peticiones duplicadas mediante `idempotency_key` en la capa de persistencia.

## 🛠️ Stack Tecnológico

*   **Java 21** (con soporte para Virtual Threads).
*   **Spring Boot 3.4**.
*   **Spring Modulith** (Módulos asíncronos y registro de eventos).
*   **RabbitMQ** (Mensajería externa y Dead Letter Queues).
*   **PostgreSQL** & **Flyway** (Migraciones de base de datos).
*   **Micrometer & Zipkin** (Trazabilidad y métricas).
*   **Testcontainers** (Pruebas de integración reales).

## 🚀 Cómo ejecutarlo

1.  **Levantar infraestructura**: `docker-compose up -d`
2.  **Compilar y Ejecutar**: `./mvnw spring-boot:run`
3.  **Swagger UI**: Accede a `http://localhost:8080/swagger-ui.html` para probar los endpoints.

---

## 📐 Decisiones Arquitectónicas (ADRs)

*   **Event-Driven**: Preferimos eventos asíncronos para desacoplar el inventario del pedido, mejorando el tiempo de respuesta del usuario.
*   **Transactional Events**: Usamos `@ApplicationModuleListener` para asegurar que los eventos se publiquen solo si la transacción local tiene éxito.
*   **Virtual Threads**: Habilitados para maximizar el rendimiento en operaciones bloqueantes de I/O (DB y RabbitMQ).

---
Desarrollado por [Tu Nombre/Perfil] como parte de una iniciativa de difusión de **Spring Modulith** en la comunidad hispana. 🌍
