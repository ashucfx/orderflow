# OrderFlow

Enterprise Order & Inventory Backend — a production-style backend system built with Java 21 and Spring Boot.

---

## Overview

OrderFlow is a modular monolith that handles the full lifecycle of order processing: user authentication, product browsing, cart management, transactional checkout, inventory control, payment simulation, and asynchronous event handling.

The project demonstrates engineering practices expected in backend roles: clean layered architecture, database design with Flyway migrations, JWT authentication with refresh token rotation, role-based access control, optimistic-locking concurrency control for inventory, Redis caching with invalidation, Kafka domain events with idempotent consumption, and thorough integration testing with Testcontainers.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security, JJWT |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL |
| Migrations | Flyway |
| Cache | Redis |
| Messaging | Apache Kafka |
| Validation | Bean Validation (Jakarta) |
| Testing | JUnit 5, Mockito, Testcontainers |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Observability | Spring Boot Actuator |
| Build | Maven |
| CI | GitHub Actions |
| Containers | Docker, Docker Compose |

---

## Modules

| Module | Responsibility |
|--------|---------------|
| `auth` | Registration, login, JWT, refresh tokens, logout |
| `user` | User profiles, role assignment |
| `product` | Product catalogue, categories, search |
| `inventory` | Stock management, reservation, concurrency control |
| `cart` | Cart and cart item management |
| `order` | Checkout, order state machine, idempotency |
| `payment` | Simulated payment workflow |
| `notification` | Kafka-driven downstream event handling |
| `audit` | Action audit trail |
| `common` | Shared exceptions, API response models, utilities |

---

## Architecture

_Detailed architecture documentation: [docs/architecture.md](docs/architecture.md)_

```
Controller → Service → Domain → Repository
```

No JPA entity is exposed directly through a REST endpoint. All HTTP request/response contracts use dedicated DTOs.

---

## Database Design

_Full schema documentation: [docs/database.md](docs/database.md)_

Schema is managed exclusively through Flyway versioned migrations. Hibernate `ddl-auto` is set to `validate` — the application will fail to start if the schema does not match the entity definitions.

---

## Authentication

- Registration with BCrypt-hashed passwords
- Login returns a short-lived JWT access token and a long-lived refresh token
- Refresh token rotation on each renewal
- Logout revokes the active refresh token
- JWT secret loaded from environment variable, never from source code

---

## Authorization

Three roles:

| Role | Permissions |
|------|------------|
| `CUSTOMER` | Browse products, manage own cart, place and view own orders |
| `INVENTORY_MANAGER` | View and update inventory |
| `ADMIN` | Full access including user management, all orders, audit logs |

---

## Order Workflow

```
Authenticate → Retrieve cart → Validate cart → Retrieve product data
→ Calculate total → Validate inventory → Reserve inventory
→ Create order + items → Create payment record → Clear cart
→ Commit transaction → Publish OrderCreated event
```

---

## Inventory Concurrency

Inventory uses **optimistic locking** (`@Version` column). Concurrent reservation attempts that conflict are retried at the application level. A dedicated integration test verifies that stock cannot be oversold under concurrent load.

---

## Redis Strategy

| Use | Key pattern | TTL |
|-----|------------|-----|
| Product cache | `product:{id}` | 5 minutes |
| Idempotency keys | `idempotency:{key}` | 24 hours |

Cache is invalidated on product update or deactivation.

---

## Kafka Events

| Event | Topic | Producer | Consumer |
|-------|-------|----------|----------|
| `OrderCreated` | `order.created` | Order service | Notification service |
| `InventoryReserved` | `inventory.events` | Inventory service | Audit service |
| `OrderCancelled` | `order.cancelled` | Order service | Inventory service |

---

## Local Setup

### Prerequisites

- Java 21
- Docker and Docker Compose
- Maven (or use included `./mvnw`)

### Steps

```bash
git clone https://github.com/ashucfx/orderflow.git
cd orderflow

# Copy and configure environment
cp .env.example .env
# Edit .env with your local values

# Start infrastructure
docker-compose up -d postgres redis kafka

# Run application
./mvnw spring-boot:run
```

API documentation: http://localhost:8080/swagger-ui.html
Health check: http://localhost:8080/actuator/health

---

## Running Tests

```bash
./mvnw test
```

Integration tests use Testcontainers and require Docker to be running.

---

## API Documentation

OpenAPI specification: `GET /api-docs`
Swagger UI: `GET /swagger-ui.html`

---

## CI/CD

GitHub Actions runs on every push to `main` and on pull requests:
- Compile
- Run all tests
- Build application JAR

Workflow: [.github/workflows/ci.yml](.github/workflows/ci.yml)

---

## Environment Variables

See [.env.example](.env.example) for all required variables with descriptions.

---

## Engineering Decisions

_Detailed rationale: [docs/architecture.md](docs/architecture.md)_

- **Modular monolith over microservices** — appropriate scope, avoids distributed systems complexity without proportionate benefit
- **Optimistic locking for inventory** — higher concurrency than pessimistic locking; tolerable retry cost for the expected contention rate
- **Flyway over `ddl-auto`** — explicit, version-controlled schema; production-safe
- **Redis for product caching** — products are read-heavy, update-infrequent; cache hit rate justifies the dependency

---

## Known Limitations

- Payment processing is simulated; no real payment provider is integrated
- Notification delivery is logged; no actual email/SMS transport is configured
- Rate limiting is implemented at the application level, not at a gateway

---

## Future Improvements

- Outbox pattern for guaranteed event delivery
- Distributed rate limiting via Redis
- Metrics export to Prometheus
- Admin dashboard

---

## License

MIT
