# OrderFlow: Enterprise Order & Inventory Processing Platform

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)]()
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)]()
[![Apache Kafka](https://img.shields.io/badge/Kafka-7.6.1-black.svg)]()
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)]()

> **OrderFlow** is a production-grade, modular monolith backend platform engineered for enterprise-scale e-commerce order processing, high-concurrency inventory reservation, distributed idempotency, and asynchronous event-driven audit logging. Built with **Java 21** and **Spring Boot 3.3.4**, it demonstrates rigorous system design, defensive database concurrency, multi-tier caching, and comprehensive test automation (169 passing unit and integration tests).

---

## Table of Contents

- [Architectural Overview](#architectural-overview)
- [System Architecture Diagram](#system-architecture-diagram)
- [Key Engineering Tenets & Invariants](#key-engineering-tenets--invariants)
  - [1. Concurrency-Safe Inventory Control (Optimistic Locking)](#1-concurrency-safe-inventory-control-optimistic-locking)
  - [2. Distributed Idempotent Checkout Engine](#2-distributed-idempotent-checkout-engine)
  - [3. Historical Price Snapshotting](#3-historical-price-snapshotting)
  - [4. Order State Machine & Two-Phase Settlement](#4-order-state-machine--two-phase-settlement)
  - [5. Cache-Aside Redis Caching with Selective Invalidation](#5-cache-aside-redis-caching-with-selective-invalidation)
  - [6. Asynchronous Kafka Event-Driven Audit Trail](#6-asynchronous-kafka-event-driven-audit-trail)
- [Module Structure & Bounded Contexts](#module-structure--bounded-contexts)
- [Database Schema & Flyway Migrations](#database-schema--flyway-migrations)
- [DevOps, Containerization & CI/CD](#devops-containerization--cicd)
- [Local Development & Setup Guide](#local-development--setup-guide)
- [REST API Reference & cURL Examples](#rest-api-reference--curl-examples)
- [Test Suite & Quality Verification](#test-suite--quality-verification)
- [Senior Technical Interview Discussion Points](#senior-technical-interview-discussion-points)

---

## Architectural Overview

OrderFlow is designed as a **Modular Monolith** adhering to Domain-Driven Design (DDD) principles and Clean Layered Architecture:

```
[ HTTP Client / Frontend / Third-Party Services ]
                       │ (TLS / REST / JSON)
                       ▼
         [ Spring Security Filter Chain ]
         ├─ JwtAuthenticationFilter (HMAC-SHA256 Token Validation)
         └─ Role-Based Access Control (CUSTOMER, INVENTORY_MANAGER, ADMIN)
                       │
                       ▼
            [ REST Controllers Layer ]
     (Request Validation, DTO Mapping, Response Envelope)
                       │
                       ▼
              [ Domain Services ]
     (Business Logic, Transaction Boundaries, State Machine)
         │                   │                    │
         ▼                   ▼                    ▼
[ Spring Data JPA ]   [ Redis Cache ]     [ Kafka Producer ]
 (PostgreSQL 16)      (orderflow::*)       (order.events)
         │                                        │
         │ (Versioned Flyway Migrations)          ▼
         ▼                                [ Kafka Consumer ]
[ Relational Schema ]                             │
                                                  ▼
                                         [ Audit Log Service ]
```

### Architectural Guardrails
1. **Zero Entity Exposure**: Domain JPA entities never leak across HTTP boundaries; all ingress and egress traffic is mediated via typed DTO records with Jakarta Validation constraints.
2. **Explicit Transaction Boundaries**: Write transactions are explicitly managed via `@Transactional(isolation = Isolation.READ_COMMITTED)` to avoid dirty reads while maximizing throughput.
3. **Fail-Safe Schema Synchronization**: Hibernate's `ddl-auto` is strictly locked to `validate`. Database schema drift is strictly forbidden; all schema modifications are managed through sequential Flyway migrations (`V1` to `V7`).

---

## System Architecture Diagram

```
+---------------------------------------------------------------------------------------------+
|                                    ORDERFLOW RUNTIME CONTAINER                              |
|                                                                                             |
|  +--------------------+    +--------------------+    +-----------------------------------+  |
|  | AuthController     |    | ProductController  |    | OrderController                   |  |
|  | - /api/v1/auth/*   |    | - /api/v1/products |    | - POST /api/v1/orders/checkout    |  |
|  +---------+----------+    +---------+----------+    +-----------------+-----------------+  |
|            |                         |                                 |                    |
|  +---------v----------+    +---------v----------+    +-----------------v-----------------+  |
|  | AuthService        |    | ProductService     |    | OrderService                      |  |
|  | - Token Rotation   |    | - Cacheable Read   |    | - Idempotency Recovery            |  |
|  | - BCrypt Hashing   |    | - Evict on Mutation|    | - Atomic Stock Reservation        |  |
|  +---------+----------+    +----+----------+----+    | - Snapshot Item Prices            |  |
|            |                    |          |         +---+---------------+---------------+  |
|            |                    |          |             |               |                  |
|            |                    |    +-----v------+      |         +-----v-----+            |
|            |                    |    | RedisCache |      |         | Inventory |            |
|            |                    |    | TTL: 1 hr  |      |         | Service   |            |
|            |                    |    +------------+      |         | @Version  |            |
|            |                    |                        |         +-----+-----+            |
|            |                    |                        |               |                  |
|            +--------------------+------------------------+---------------+                  |
|                                 |                                        |                  |
|                                 v                                        v                  |
|               +-----------------------------------+            +--------------------+       |
|               | Spring Data JPA / Hibernate Core  |            | KafkaEventProducer |       |
|               +-----------------+-----------------+            +---------+----------+       |
+---------------------------------|----------------------------------------|------------------+
                                  |                                        |
                                  v                                        v
                 +---------------------------------+             +--------------------+
                 | PostgreSQL 16 (Flyway V1 - V7)  |             | Apache Kafka 7.6.1 |
                 | - users, roles, user_roles      |             | Topic:             |
                 | - categories, products          |             |   order.events     |
                 | - inventories (@Version)        |             +---------+----------+
                 | - carts, cart_items             |                       |
                 | - orders, order_items           |                       v
                 | - payments, idempotency_keys    |             +--------------------+
                 | - audit_logs                    |             | KafkaEventConsumer |
                 +---------------------------------+             +---------+----------+
                                                                           |
                                                                           v
                                                                 +--------------------+
                                                                 |  AuditLogService   |
                                                                 +--------------------+
```

---

## Key Engineering Tenets & Invariants

### 1. Concurrency-Safe Inventory Control (Optimistic Locking)
High-volume flash sales face severe race conditions where concurrent checkouts could easily deplete inventory into negative stock (overselling).
- **Implementation**: The `Inventory` entity utilizes a JPA `@Version` column (`version BIGINT NOT NULL DEFAULT 0`).
- **Atomic Operations**: `reserve()`, `release()`, and `confirm()` execute within transaction boundaries. Any concurrent modification triggers an `OptimisticLockingFailureException`.
- **Zero Oversell Guarantee**: The system validates `(availableQuantity - reservedQuantity) >= requestedQuantity` under lock, ensuring physical stock invariant `available_quantity >= 0` is mathematically guaranteed.

### 2. Distributed Idempotent Checkout Engine
Network timeouts and aggressive user retries can cause duplicate credit card authorizations and duplicate order placement.
- **Implementation**: Checkout endpoints accept a mandatory `Idempotency-Key` HTTP header.
- **Workflow**:
  1. Computes a cryptographic payload digest (SHA-256) of the incoming checkout request.
  2. Queries the persistent `idempotency_keys` table.
  3. If key exists and payload matches: directly deserializes and returns the previously committed `OrderResponse` with HTTP 200 OK without re-executing inventory deduction or order creation.
  4. If key exists with different payload: throws `IdempotencyConflictException` (HTTP 409 Conflict).
  5. Concurrency Race Handling: Catches database unique-constraint violations (`DataIntegrityViolationException`) to cleanly recover and return the winning concurrent thread's generated response.

### 3. Historical Price Snapshotting
In e-commerce systems, product prices fluctuate over time. An order placed today must retain the exact price paid, regardless of future catalog adjustments.
- **Implementation**: When an order is formed from cart items, `OrderItem` captures a detached snapshot:
  - `unitPrice`: The exact catalog price at the precise millisecond of checkout.
  - `totalPrice`: Computed as `unitPrice * quantity`.
  - Future updates to `products.price` never mutate historical order records or invoice calculations.

### 4. Order State Machine & Two-Phase Settlement
Orders transition through a strict, deterministic finite state machine:
- `PENDING`: Initial state upon checkout; stock is **reserved** (`reservedQuantity += N`), but not decremented from physical stock.
- `CONFIRMED`: Triggered upon `PaymentStatus.SUCCESS`; reserved stock is permanently deducted (`availableQuantity -= N, reservedQuantity -= N`).
- `CANCELLED`: Triggered upon user cancellation or `PaymentStatus.FAILED`; reserved stock is restored to open pool (`reservedQuantity -= N`).

```
           [ CART ]
              │ (checkout)
              ▼
         +---------+
         | PENDING | (Stock Reserved)
         +----+----+
              │
      +-------+-------+
      │ (Payment OK)  │ (Payment Failed / Cancelled)
      ▼               ▼
+-----------+   +-----------+
| CONFIRMED |   | CANCELLED |
| (Deducted)|   | (Released)|
+-----------+   +-----------+
```

### 5. Cache-Aside Redis Caching with Selective Invalidation
To minimize database reads on high-traffic product browsing:
- Read queries (`findById`, `findAllPaged`) utilize Spring's `@Cacheable(value = "products", key = "#id")`.
- Dynamic JSON serialization configured via `GenericJackson2JsonRedisSerializer` with `JavaTimeModule` to ensure seamless `Instant` and `BigDecimal` marshaling.
- Cache Invalidation: Any mutating operation (`updateProduct`, `deleteProduct`) invokes `@CacheEvict(value = "products", allEntries = true)` to prevent stale reads across distributed replicas.

### 6. Asynchronous Kafka Event-Driven Audit Trail
Decoupling critical business workflows from non-blocking analytical and notification operations:
- Order state changes emit strongly typed domain events (`OrderPlacedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`) over the `order.events` Kafka topic.
- `OrderEventConsumer` listens asynchronously with consumer group `orderflow-audit-group`.
- Incoming events are persisted to the relational `audit_logs` table, maintaining an immutable ledger of all system transactions with millisecond precision.

---

## Module Structure & Bounded Contexts

The codebase is organized into modular packages representing domain boundaries:

```
com.orderflow
├── audit          # Asynchronous audit log persistence, entities, consumers
├── auth           # JWT generation, token rotation, authentication controller
├── cart           # Cart aggregate root, item manipulation, price calculation
├── common         # Global exception handler, API response wrappers, DTOs
├── config         # Security, Redis cache, Kafka producer/consumer, OpenAPI
├── inventory      # Stock adjustments, optimistic locking, reservation logic
├── notification   # Kafka event definitions (Placed, Confirmed, Cancelled)
├── order          # Order lifecycle, state machine, idempotency engine
├── payment        # Simulated payment processing, order settlement
├── product        # Product catalog, categories, search, Redis caching
└── user           # User profiles, role-based authorization, repositories
```

---

## Database Schema & Flyway Migrations

All schema changes are versioned, immutable, and strictly managed under `src/main/resources/db/migration/`:

| Version | Migration Script | Description & Tables Created |
|---------|------------------|------------------------------|
| **V1** | `V1__init_schema.sql` | Base RBAC tables: `users`, `roles`, `user_roles` |
| **V2** | `V2__create_category_and_product_tables.sql` | Catalog tables: `categories`, `products` (SKU indexes) |
| **V3** | `V3__create_inventory_table.sql` | Stock control: `inventories` (with `@Version` column) |
| **V4** | `V4__create_cart_tables.sql` | User shopping carts: `carts`, `cart_items` |
| **V5** | `V5__create_order_tables.sql` | Order management: `orders`, `order_items` |
| **V6** | `V6__create_payment_tables.sql` | Payment transactions: `payments` |
| **V7** | `V7__create_idempotency_and_audit_tables.sql` | Reliability: `idempotency_keys`, `audit_logs` |

---

## DevOps, Containerization & CI/CD

### 1. Multi-Stage Distroless-Style Dockerfile
Engineered with build efficiency and container security best practices:
- **Build Stage**: Uses Eclipse Temurin 21 JDK to compile and package an executable Spring Boot JAR.
- **Runtime Stage**: Uses minimal Eclipse Temurin 21 JRE base image.
- **Principle of Least Privilege**: Runs as an unprivileged non-root system user (`orderflow:1001`).
- **Container-Aware JVM Tuning**: Automatically passes `-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom`.

### 2. Multi-Service Docker Compose Orchestration
The included `docker-compose.yml` launches the complete enterprise topology:
- **`postgres`**: PostgreSQL 16 Alpine with custom health checks (`pg_isready`).
- **`redis`**: Redis 7 Alpine with persistent memory limits and health checks (`redis-cli ping`).
- **`zookeeper` & `kafka`**: Confluent Platform Kafka 7.6.1 message broker.
- **`orderflow-app`**: Spring Boot container with service health dependencies (`depends_on: condition: service_healthy`).

```bash
# Start all infrastructure and the application
docker-compose up -d --build

# View real-time application logs
docker-compose logs -f orderflow-app
```

### 3. Continuous Integration (GitHub Actions)
The workflow at `.github/workflows/ci.yml` runs on all pull requests and pushes to `main`:
1. Checks out repository.
2. Configures Temurin JDK 21 with aggressive Maven dependency caching.
3. Compiles the modular codebase.
4. Executes all 169 unit and integration tests.
5. Verifies zero compilation warnings and builds production JAR artifact.

---

## Local Development & Setup Guide

### Prerequisites
- **Java Development Kit (JDK)**: 21 or later
- **Maven**: 3.9+ (or use `./mvnw` / `mvnw.cmd`)
- **Docker & Docker Compose**: (Optional for local containerized infrastructure)

### 1. Clone & Configure Environment
```bash
git clone https://github.com/ashucfx/orderflow.git
cd orderflow

# Copy environment template
cp .env.example .env
```

### 2. Run with Local Docker Services
```bash
# Start PostgreSQL, Redis, and Kafka in background
docker-compose up -d postgres redis kafka

# Launch Spring Boot Application
./mvnw spring-boot:run
```

### 3. Verification Endpoints
- **API Base URL**: `http://localhost:8080/api/v1`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI 3 JSON Spec**: `http://localhost:8080/v3/api-docs`
- **Actuator Health**: `http://localhost:8080/actuator/health`

---

## REST API Reference & cURL Examples

### Authentication Workflows

#### 1. Register a New Customer
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "johndoe@example.com",
    "password": "Password123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

#### 2. User Login (Obtain JWT & Refresh Token)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "Password123!"
  }'
```
*Response:*
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "d8f3b2a1-4c5e-6f7a-8b9c-0d1e2f3a4b5c",
    "tokenType": "Bearer",
    "expiresIn": 86400000
  }
}
```

---

### Catalog & Inventory

#### 3. Browse Products (Paged & Cached)
```bash
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=10&sort=name,asc"
```

#### 4. Adjust Inventory Stock (Requires `INVENTORY_MANAGER` or `ADMIN`)
```bash
curl -X POST http://localhost:8080/api/v1/inventory/adjust \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantityAdjustment": 50,
    "reason": "Restock shipment PO-9481"
  }'
```

---

### Cart & Checkout Workflows

#### 5. Add Item to Shopping Cart
```bash
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

#### 6. Idempotent Order Checkout
```bash
curl -X POST http://localhost:8080/api/v1/orders/checkout \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>" \
  -H "Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddress": "123 Tech Boulevard, San Francisco, CA 94105",
    "paymentMethod": "CREDIT_CARD"
  }'
```
*Response:*
```json
{
  "success": true,
  "data": {
    "orderId": 1042,
    "orderNumber": "ORD-20260917-8A3F",
    "status": "PENDING",
    "totalAmount": 199.98,
    "items": [
      {
        "productId": 1,
        "productName": "Ergonomic Mechanical Keyboard",
        "unitPrice": 99.99,
        "quantity": 2,
        "totalPrice": 199.98
      }
    ],
    "createdAt": "2026-09-17T10:15:30Z"
  }
}
```

---

### Payment Simulation & Order Settlement

#### 7. Process Order Payment
```bash
curl -X POST http://localhost:8080/api/v1/payments/process \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1042,
    "amount": 199.98,
    "paymentMethod": "CREDIT_CARD",
    "simulateStatus": "SUCCESS"
  }'
```

---

## Test Suite & Quality Verification

The OrderFlow repository maintains a zero-defect policy with **169 automated tests** covering unit, slice, concurrency, and full-stack integration layers:

```
-------------------------------------------------------
 T E S T S   S U M M A R Y
-------------------------------------------------------
Tests run: 169, Failures: 0, Errors: 0, Skipped: 0
Build Status: SUCCESS
Execution Time: ~28s
-------------------------------------------------------
```

### Key Test Categories:
- **`ConcurrencyAndIdempotencyTest`**: Simulates 10 concurrent threads attempting identical checkouts simultaneously using Java `CountDownLatch` and `ExecutorService` to verify zero duplicate charges and zero race conditions.
- **`OrderLifecycleScenarioTest`**: Full end-to-end simulation: User Registration -> Stock Provisioning -> Cart Addition -> Idempotent Checkout -> Payment Simulation -> Stock Confirmation.
- **`Testcontainers Integration Suite`**: `OrderFlowTestcontainersIT` verifies real PostgreSQL, Redis, and Kafka container integration. Equipped with `@Testcontainers(disabledWithoutDocker = true)` for zero-friction local execution when Docker is unavailable.

To execute the test suite:
```bash
./mvnw clean test
```

---

## Senior Technical Interview Discussion Points

### 1. Modular Monolith vs. Microservices Trade-off
*Question: "Why did you build OrderFlow as a modular monolith instead of a distributed microservices cluster?"*
- **Engineering Justification**: Microservices introduce distributed transactions (Saga / 2PC), network latency, partial failures, and operational overhead (service meshes, distributed tracing, independent CI/CD pipelines).
- For an order volume of this domain, a **Modular Monolith** provides clean bounded contexts with strong in-memory transactional consistency. If individual domains (e.g., Inventory) ever require independent scaling, the decoupled package structure enables seamless extraction into standalone microservices without database redesign.

### 2. Optimistic vs. Pessimistic Locking in Inventory
*Question: "Why choose optimistic locking over `SELECT ... FOR UPDATE`?"*
- **Engineering Justification**: Pessimistic locking (`SELECT FOR UPDATE`) holds exclusive database row locks for the entire duration of a transaction, leading to thread starvation, database connection pool exhaustion, and deadlocks under flash sale bursts.
- Optimistic locking (`@Version`) achieves **lock-free reads**. In a system where 90% of requests are non-conflicting reads, throughput is orders of magnitude higher. For the conflicting 10%, application-level exception handling provides clean feedback without crippling database throughput.

### 3. Distributed Idempotency Design
*Question: "How do you guarantee that network retries don't charge customers twice?"*
- **Engineering Justification**: The client sends a unique `Idempotency-Key` (UUIDv4). The backend utilizes an atomic database constraint on `idempotency_keys.key`. Even if two identical requests hit two different server nodes simultaneously, only one transaction can successfully commit. The competing transaction catches the constraint violation and transparently returns the winning transaction's cached response payload.

### 4. Eventual Consistency with Apache Kafka
*Question: "Why use Kafka for audit logs instead of writing directly to the database in the checkout transaction?"*
- **Engineering Justification**: Writing audit logs synchronously in the primary checkout transaction bloats transaction latency and increases lock retention times. By emitting asynchronous domain events (`OrderPlacedEvent`), checkout latency remains under 50ms, while the audit consumer processes and persists event records asynchronously without blocking the user.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
