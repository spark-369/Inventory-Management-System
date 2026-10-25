<div align="center">

# IMS — Inventory Management System

**A full-stack, event-driven inventory management platform.**

Spring Boot 4 REST API · React 19 SPA · PostgreSQL · Redis · JobRunr

</div>

---

## Table of Contents

- [Overview](#overview)
- [Feature Highlights](#feature-highlights)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)
- [API Reference](#api-reference)
- [Domain Model](#domain-model)
- [Messaging & Background Jobs](#messaging--background-jobs)
- [Caching (Redis)](#caching-redis)
- [Rate Limiting (Redis)](#rate-limiting-redis)
- [Error Handling](#error-handling)
- [Security Notes](#security-notes)
- [Testing](#testing)
- [Docker & Deployment](#docker--deployment)
- [Troubleshooting](#troubleshooting)
- [Conventions](#conventions)

---

## Overview

IMS is a two-application monorepo:

| Application | Path | Responsibility |
|-------------|------|----------------|
| **Backend** | `backend/` | Spring Boot 4 REST API, JPA persistence, JobRunr message broker, Redis cache + rate limiter |
| **Frontend** | `frontend/` | React 19 + Vite single-page application (Redux Toolkit, React Router 7, Tailwind CSS 4) |

The system covers the full inventory lifecycle: catalogue (products, categories,
brands), master data (warehouses, customers, suppliers), operations (purchases,
sales, returns), inventory control (stock batches, stock movements) and
administration (alerts, background-job console).

The backend is **event-driven**: domain services publish typed messages to an
in-process broker (JobRunr, persisted in the same PostgreSQL database) and
dedicated consumers perform derived work — alert sweeps, expiry scans, inventory
snapshots, order confirmations, daily reports — off the HTTP request thread.

---

## Feature Highlights

**Inventory & catalogue**
- Products with SKU, barcode, QR code, pricing (purchase / selling / MRP), unit and reorder level.
- Categories and brands for grouping and filtering.
- Per-product flags for batch tracking and expiry tracking.
- Low-stock detection against each product's reorder level.

**Operations**
- Purchases with supplier, warehouse, line items and a receive workflow that increments stock.
- Sales with customer, warehouse, line items, discount/tax/grand totals, and a completion workflow that validates and decrements stock.
- Returns (purchase and sale) with an approval workflow that adjusts stock.

**Inventory control**
- Stock batches with batch number, expiry date, quantity and cost price.
- Stock movements as an append-only audit trail (`IN`, `OUT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`, `RETURN`, `SALE`, `PURCHASE`) with a reference type/id back to the originating document.
- Multi-warehouse support throughout.

**Alerts**
- `LOW_STOCK`, `EXPIRY_WARNING` and `EXPIRY_ALERT` alerts, deduplicated so a condition raises only one unresolved alert.
- Sweeps run on background workers; alerts can also be resolved or created manually.

**Platform**
- **Background Jobs console** in the UI: live broker counters, recurring schedules, a manual message publisher and the full message audit trail (auto-refreshing).
- **Redis caching** with per-region TTLs and explicit cross-service invalidation.
- **Redis rate limiting** on every `/api/**` request using an atomic Lua script.
- **Structured error responses** consumed directly by the UI.
- Responsive, accessible UI with skeleton/empty/error states, toasts and styled confirm dialogs.

---

## Architecture

### System view

```
┌────────────────────────────┐        ┌──────────────────────────────────────┐
│        Browser (SPA)       │        │           Spring Boot API            │
│  React 19 · Redux Toolkit  │        │                                      │
│  React Router · Tailwind   │  HTTP  │  RateLimitFilter ──▶ Controllers     │
│                            │ ─────▶ │         │                            │
│  Vite dev proxy:  /api     │  /api  │         ▼                            │
│  nginx (prod):    /api     │        │      Services (@Transactional)       │
└────────────────────────────┘        │         │            │               │
                                      │         ▼            ▼               │
                                      │   Repositories   DomainMessage       │
                                      │   (JPA)          Publisher           │
                                      └────┬──────┬───────────┬──────────────┘
                                           │      │           │ after commit
                                           ▼      ▼           ▼
                                    ┌──────────┐ ┌────────┐ ┌─────────────────┐
                                    │PostgreSQL│ │ Redis  │ │ JobRunr broker  │
                                    │  domain  │ │ cache +│ │ (PostgreSQL-    │
                                    │  data +  │ │ limiter│ │  backed)        │
                                    │ jobrunr_*│ │        │ │  ▼ workers      │
                                    │  tables  │ │        │ │  *JobConsumer   │
                                    └──────────┘ └────────┘ └─────────────────┘
```

### Request lifecycle

1. `RateLimitFilter` (servlet filter, highest precedence) checks the Redis counter
   for `/api/**` and writes `X-RateLimit-*` headers. Over-limit requests
   short-circuit with `429`.
2. The controller validates the request body (`@Valid`, Jakarta Bean Validation)
   and delegates to a service.
3. The service runs inside `@Transactional`, reads/writes through Spring Data JPA
   repositories and evicts affected cache regions.
4. If the change has a downstream consequence, the service calls
   `DomainMessagePublisher.publishAfterCommit(...)` / `scheduleAfterCommit(...)`.
5. After the transaction commits, `JobRunrMessageBroker` writes a
   `job_message_log` audit row and enqueues a persistent JobRunr job.
6. A background worker invokes the bound `JobRequestHandler` consumer, which flips
   the audit row `ENQUEUED → RUNNING → SUCCEEDED|FAILED`.

### Why JobRunr instead of RabbitMQ/Kafka

JobRunr stores jobs in the **same PostgreSQL database** as the domain data, so the
project needs no additional broker to provision or operate, and messages survive
application restarts. It provides at-least-once delivery, automatic retries with
exponential back-off, retention policies and a dashboard out of the box.

> **Note:** JobRunr OSS ships SQL (PostgreSQL, MySQL, H2, …), MongoDB (Pro),
> in-memory and custom `StorageProvider`s — **there is no Redis storage
> provider**. Redis is therefore used for caching and rate limiting, *not* as the
> job store.

### Backend layering

| Layer | Package | Responsibility |
|-------|---------|----------------|
| Entry point | `com.unique.ims.backend` | `BackendApplication` (`@SpringBootApplication`) |
| Web | `controller` | Thin REST endpoints; validation + delegation only |
| Business logic | `service` (interfaces) / `service.impl` | Transactions, cache annotations, message publishing, entity↔DTO mapping |
| Persistence | `repository` | Spring Data JPA interfaces (`JpaRepository`) |
| Domain | `model` | JPA entities and their status enums |
| Contracts | `dto` | Request/response DTOs — entities never leave the service layer |
| Cross-cutting | `config` | Caching, CORS, Redis value serialization |
| Messaging | `messaging` (+ `request`, `consumer`) | Broker facade, message payloads, consumers, schedules |
| Errors | `exception` | `@RestControllerAdvice` + structured error body |
| Throttling | `ratelimit` | Servlet filter, atomic Lua limiter, properties |

Design rules enforced throughout:

- **DTOs at the boundary.** Controllers accept and return DTOs; `mapToDto` runs
  inside the service so `spring.jpa.open-in-view=false` is safe.
- **Constructor injection** everywhere — no field injection, no `@Autowired`.
- **Read-heavy reads are cached, writes evict** — see [Caching](#caching-redis).

---

## Technology Stack

### Backend

| Concern | Choice | Version |
|---------|--------|---------|
| Framework | Spring Boot | 4.0.6 |
| Language | Java | 17 |
| Build | Maven (wrapper included) | `./mvnw` |
| Persistence | Spring Data JPA / Hibernate | Boot-managed |
| Database | PostgreSQL | 12+ |
| JSON | Jackson 3 (`tools.jackson.*`) | Boot-managed |
| Cache / limiter | Redis + Lettuce (+ `commons-pool2`) | Boot-managed |
| Messaging | JobRunr (`jobrunr-spring-boot-4-starter`) | 9.0.2 |
| Barcodes / QR | ZXing (`core`, `javase`) | 3.5.3 |
| Boilerplate | Lombok | Boot-managed |
| Tests | JUnit 5, Mockito, AssertJ, H2 | Boot-managed |

Declared starters: `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`,
`spring-boot-starter-web`, `spring-boot-starter-validation`,
`spring-boot-starter-mail`, `spring-boot-starter-data-redis`.

### Frontend

| Concern | Choice | Version |
|---------|--------|---------|
| UI library | React | 19 |
| Build tool | Vite | 8 |
| Language | TypeScript | 6 |
| State | Redux Toolkit + React Redux | 2 / 9 |
| Routing | React Router DOM | 7 |
| Styling | Tailwind CSS (via `@tailwindcss/vite`) | 4 |
| Components | Headless UI | 2 |
| Toasts | react-hot-toast | 2 |
| Lint | ESLint + typescript-eslint | 10 / 8 |

---

## Project Structure

```
.
├── backend/                     # Spring Boot REST API
│   ├── pom.xml                  # Dependencies + build config
│   ├── mvnw / mvnw.cmd          # Maven wrapper
│   ├── Dockerfile               # Multi-stage: JDK build → JRE runtime
│   └── src/
│       ├── main/java/com/unique/ims/backend/
│       │   ├── BackendApplication.java
│       │   ├── config/          # CacheConfig, CacheNames, CorsConfig,
│       │   │                    # GenericJackson3RedisSerializer
│       │   ├── controller/      # 13 REST controllers (incl. BackgroundJobController)
│       │   ├── dto/             # Request/response DTOs + job console DTOs
│       │   ├── exception/       # GlobalExceptionHandler, ApiErrorResponse
│       │   ├── messaging/
│       │   │   ├── JobMessageType.java         # Routing keys (single source of truth)
│       │   │   ├── MessageBroker.java          # Facade: publish / schedule / inspect
│       │   │   ├── JobRunrMessageBroker.java   # JobRunr-backed implementation
│       │   │   ├── DomainMessagePublisher.java # Publishes after commit
│       │   │   ├── JobSchedules.java           # Recurring schedule declarations
│       │   │   ├── request/     # AbstractJobMessage + typed payloads
│       │   │   └── consumer/    # AbstractJobConsumer + 5 consumers + RecurringJobPublisher
│       │   ├── model/           # JPA entities
│       │   ├── ratelimit/       # RateLimitFilter, RateLimiterService,
│       │   │                    # RateLimitProperties, RateLimitDecision
│       │   ├── repository/      # Spring Data JPA repositories
│       │   └── service/         # Interfaces + impl/ (business logic)
│       ├── main/resources/
│       │   └── application.properties
│       └── test/
│           ├── java/...         # 9 test classes (unit + context)
│           └── resources/application.properties   # H2 + JobRunr/Redis disabled
│
└── frontend/                    # React + Vite SPA
    ├── package.json
    ├── vite.config.ts           # React + Tailwind plugins, /api dev proxy
    ├── Dockerfile               # Multi-stage: Node build → nginx runtime
    ├── index.html
    └── src/
        ├── main.tsx             # React root + Redux Provider
        ├── App.tsx              # Routes, ErrorBoundary, Toaster
        ├── index.css            # Tailwind entry + design tokens
        ├── components/
        │   ├── ui/              # Design-system primitives (see below)
        │   ├── CrudPage.tsx     # Generic list+CRUD screen
        │   ├── CrudField.tsx    # Field renderer driven by config
        │   ├── DispatchPanel.tsx# Manual publisher + schedule list
        │   ├── LineItemsEditor.tsx
        │   ├── Layout.tsx / Sidebar.tsx / Header.tsx
        │   ├── ErrorBoundary.tsx
        │   └── navigation.ts    # Single source of truth for routes/labels
        ├── hooks/               # redux, useConfirm, useMutation, useTableData
        ├── pages/               # One component per route
        ├── services/api.ts      # fetch wrapper + ApiError
        ├── store/               # One Redux slice per resource
        ├── types/api.ts         # Shared DTOs and status unions
        └── utils/format.ts      # Currency / date / number formatters
```

### UI design-system primitives (`components/ui`)

| Component | Purpose |
|-----------|---------|
| `Button` | Variants, sizes, loading state, optional icon |
| `Form` | `TextField` / `SelectField` / `TextAreaField` / `CheckboxField` with ARIA error wiring |
| `DataTable` | Responsive, sortable table with typed columns |
| `Modal` | Accessible dialog (Headless UI focus trap, Escape-to-close) |
| `ConfirmDialog` | Styled replacement for `window.confirm` |
| `Pagination` | Table paginator |
| `Badge` / `StatusBadge` | Status pills with semantic tones |
| `StatCard` | Dashboard KPI tiles |
| `SearchBar` | Search input with clear action |
| `States` | `EmptyState` / `ErrorState` / `TableSkeleton` |
| `PageHeader`, `Toolbar` | Page chrome |
| `icons.tsx` | Dependency-free SVG icon set |

### Frontend routes

| Path | Page |
|------|------|
| `/dashboard` | Dashboard (stock health, alerts, KPIs) |
| `/products`, `/categories`, `/brands` | Catalogue |
| `/purchases`, `/sales`, `/returns` | Operations |
| `/stock-batches`, `/stock-movements`, `/warehouses` | Inventory |
| `/alerts`, `/background-jobs`, `/customers`, `/suppliers` | Administration |
| `*` | `NotFound` |

---

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| JDK | 17+ | Required by the backend |
| Maven | 3.9+ | Optional — the wrapper `./mvnw` is bundled |
| Node.js | 20+ | Vite 8 requires a modern LTS (Node 22 used in the Docker build) |
| PostgreSQL | 12+ | Domain data **and** JobRunr job tables |
| Redis | 6+ | Cache regions + rate-limiter counters |

### 1. Start the infrastructure

The quickest path is Docker:

```bash
docker run -d --name ims-postgres \
  -e POSTGRES_DB=ims_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=mysecretpassword \
  -p 5432:5432 postgres:16-alpine

docker run -d --name ims-redis -p 6379:6379 redis:7-alpine
```

Or create the database manually in an existing PostgreSQL instance:

```sql
CREATE DATABASE ims_db;
```

Hibernate creates/updates the schema on startup
(`spring.jpa.hibernate.ddl-auto=update`), and JobRunr creates its own
`jobrunr_jobs` / `jobrunr_recurring_jobs` tables on first run. No migration
scripts are required.

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run
# or, with a local Maven installation:
mvn spring-boot:run
```

| Service | URL |
|---------|-----|
| REST API | <http://localhost:8080/api> |
| JobRunr dashboard | <http://localhost:8000/dashboard> |

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app is served at <http://localhost:5173>. Vite proxies `/api` to
`http://localhost:8080` (`changeOrigin: true`), so the browser stays on a single
origin and no CORS preflight is needed during development.

### 4. Verify

```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/jobs/stats
```

Open <http://localhost:5173>, then check **Administration → Background Jobs** to
watch the broker counters, the two recurring schedules and the message audit
trail.

### Common commands

| Task | Command |
|------|---------|
| Backend — run | `cd backend && ./mvnw spring-boot:run` |
| Backend — test | `cd backend && ./mvnw test` |
| Backend — package | `cd backend && ./mvnw clean package` |
| Frontend — dev server | `cd frontend && npm run dev` |
| Frontend — production build | `cd frontend && npm run build` |
| Frontend — type-check | `cd frontend && npx tsc -b` |
| Frontend — lint | `cd frontend && npm run lint` |

---

## Configuration Reference

### Backend — `backend/src/main/resources/application.properties`

**Datasource & JPA**

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/ims_db` | JDBC URL |
| `spring.datasource.username` | `postgres` | Database user |
| `spring.datasource.password` | `mysecretpassword` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management |
| `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.PostgreSQLDialect` | Dialect |
| `spring.jpa.show-sql` | `false` | SQL logging |
| `spring.jpa.open-in-view` | `false` | Associations are fetched eagerly and mapping happens in the service layer |
| `spring.jackson.default-property-inclusion` | `non_null` | Omits null fields from JSON responses |

**CORS**

| Property | Default | Description |
|----------|---------|-------------|
| `app.cors.allowed-origins` | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated allowed origins |

**Redis**

| Property | Default | Description |
|----------|---------|-------------|
| `spring.data.redis.host` | `${REDIS_HOST:localhost}` | Redis host |
| `spring.data.redis.port` | `${REDIS_PORT:6379}` | Redis port |
| `spring.data.redis.timeout` | `2s` | Command timeout |
| `spring.data.redis.connect-timeout` | `2s` | Connect timeout |
| `spring.data.redis.lettuce.pool.max-active` | `16` | Pool ceiling |
| `spring.data.redis.lettuce.pool.max-idle` | `8` | Max idle connections |
| `spring.data.redis.lettuce.pool.min-idle` | `2` | Min idle connections |
| `spring.data.redis.lettuce.pool.max-wait` | `1s` | Pool acquisition timeout |
| `spring.cache.type` | `redis` | Selects Redis for the cache abstraction |

**Rate limiting**

| Property | Default | Description |
|----------|---------|-------------|
| `app.rate-limit.enabled` | `${RATE_LIMIT_ENABLED:true}` | Master switch |
| `app.rate-limit.requests-per-window` | `${RATE_LIMIT_REQUESTS:120}` | Requests allowed per window |
| `app.rate-limit.window-seconds` | `${RATE_LIMIT_WINDOW_SECONDS:60}` | Fixed window length |
| `app.rate-limit.trust-forwarded-for` | `true` | Honour `X-Forwarded-For` (only behind a trusted proxy) |

**JobRunr**

| Property | Default | Description |
|----------|---------|-------------|
| `jobrunr.database.skip-create` | `false` | Allow JobRunr to create its tables |
| `jobrunr.database.table-prefix` | `jobrunr_` | Table prefix |
| `jobrunr.jobs.default-number-of-retries` | `5` | Retry attempts for a failing consumer |
| `jobrunr.jobs.retry-back-off-time-seed` | `3` | Exponential back-off seed |
| `jobrunr.jobs.delete-succeeded-jobs-after` | `36h` | Retention for successful jobs |
| `jobrunr.jobs.permanently-delete-deleted-jobs-after` | `168h` | Retention for deleted jobs |
| `jobrunr.dashboard.enabled` | `true` | JobRunr dashboard |
| `jobrunr.dashboard.port` | `8000` | Dashboard port |
| `jobrunr.background-job-server.enabled` | `true` | Run a worker pool in this process |
| `jobrunr.background-job-server.poll-interval-in-seconds` | `15` | Poll interval |
| `jobrunr.background-job-server.worker-count` | `4` | Parallel workers |
| `app.messaging.dashboard-url` | `http://localhost:8000/dashboard` | Link shown in the UI console |

> **Spring Boot 4 / Jackson 3 note:** do **not** set
> `spring.jackson.serialization.write-dates-as-timestamps`. Jackson 3 moved that
> flag out of `SerializationFeature` (into `DateTimeFeature`), so setting it fails
> property binding and aborts startup. Jackson 3 already serializes `java.time`
> values as ISO-8601 strings.

### Frontend environment

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `/api` | Explicit API base URL (trailing slash stripped). Leave unset to use the dev proxy / nginx proxy |
| `VITE_PROXY_TARGET` | `http://localhost:8080` | Dev-proxy target for `/api` (read by `vite.config.ts`) |

Example override for a deployed API:

```bash
# frontend/.env.local
VITE_API_BASE_URL=https://api.example.com/api
```

### Docker / environment variables

The backend reads configuration from environment variables, which Spring Boot
binds with relaxed rules:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ims_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=mysecretpassword
REDIS_HOST=redis
REDIS_PORT=6379
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS=120
RATE_LIMIT_WINDOW_SECONDS=60
```

### Test configuration

`backend/src/test/resources/application.properties` shadows the main file on the
test classpath, so tests never touch the developer's PostgreSQL/Redis:

- **H2 in-memory** (`MODE=PostgreSQL`, `H2Dialect`, `ddl-auto=create-drop`).
- JobRunr `StorageProvider` stays enabled (it creates its tables in H2) but the
  worker pool and dashboard are **disabled** — a test JVM must not poll for jobs or
  bind port 8000.
- `app.rate-limit.enabled=false` keeps context-level requests independent of a
  live Redis; nothing connects to Redis at startup (the Lettuce factory and cache
  manager are lazy).

---

## API Reference

All endpoints are served under the `/api` prefix and accept/return JSON. Request
bodies are validated with Jakarta Bean Validation; failures return a structured
`400` (see [Error Handling](#error-handling)). Deletes return `204 No Content`.

### Products — `/api/products`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Create a product |
| `PUT` | `/{id}` | Update a product |
| `GET` | `/{id}` | Get a product by ID |
| `GET` | `/sku/{sku}` | Get a product by SKU |
| `GET` | `/barcode/{barcode}` | Get a product by barcode |
| `GET` | `/` | List all products |
| `GET` | `/low-stock` | List products at or below their reorder level |
| `DELETE` | `/{id}` | Delete a product |

**Product payload** (`ProductDto`)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `sku` | string | ✅ | Unique |
| `name` | string | ✅ | |
| `description` | string | | |
| `categoryId` / `brandId` / `supplierId` | number | | Associations; `*Name` fields are read-only in responses |
| `barcode`, `qrCode`, `imageUrl` | string | | |
| `purchasePrice`, `sellingPrice`, `mrp` | number | | |
| `reorderLevel` | integer | | Threshold for low-stock detection |
| `currentStock` | integer | | Maintained by the operations workflows |
| `unit` | string | | e.g. `pcs`, `kg`, `box` |
| `active` | boolean | | Defaults to `true` |
| `trackBatch`, `trackExpiry` | boolean | | Default to `false` |

```bash
curl -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1001","name":"Coffee Beans 1kg","categoryId":1,"reorderLevel":10,"currentStock":40,"unit":"bag"}'
```

### Categories, Brands, Warehouses, Customers, Suppliers

These resources share an identical CRUD shape (no bulk or filter endpoints):

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Create |
| `PUT` | `/{id}` | Update |
| `GET` | `/{id}` | Get by ID |
| `GET` | `/` | List all |
| `DELETE` | `/{id}` | Delete |

Base paths:

| Resource | Base path |
|----------|-----------|
| Categories | `/api/categories` |
| Brands | `/api/brands` |
| Warehouses | `/api/warehouses` |
| Customers | `/api/customers` |
| Suppliers | `/api/suppliers` |

> Categories and brands are cached for 30 minutes, customers/suppliers for 15.
> Writes evict their whole region — see [Caching](#caching-redis).

### Sales — `/api/sales`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Create a sale (`PENDING`, invoice number generated, `ORDER_CONFIRMATION` published) |
| `PUT` | `/{id}` | Update a sale |
| `POST` | `/{id}/complete` | Complete a sale — validates then decrements stock, publishes `LOW_STOCK_SCAN` per line |
| `GET` | `/{id}` | Get a sale by ID |
| `GET` | `/invoice/{invoiceNumber}` | Get a sale by invoice number |
| `GET` | `/` | List all sales |
| `DELETE` | `/{id}` | Delete a sale |

**Sale payload** (`SaleDto`)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `warehouseId` | number | ✅ | `Warehouse ID is required` |
| `customerId` | number | | Optional walk-in sale |
| `saleDate`, `status` | | | Server-managed; `status` ∈ `DRAFT`, `PENDING`, `COMPLETED`, `CANCELLED`, `RETURNED` |
| `totalAmount`, `discount`, `taxAmount`, `grandTotal` | number | | Computed from line items; `grandTotal = totalAmount − discount + taxAmount` |
| `notes` | string | | |
| `items[]` | array | | Each: `{ productId, quantity, unitPrice?, batchNumber? }` |

`POST /{id}/complete` returns `400` when any line exceeds available stock, e.g.
`Insufficient stock for 'Coffee Beans 1kg'. Available: 3, requested: 5.`

### Purchases — `/api/purchases`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Create a purchase |
| `PUT` | `/{id}` | Update a purchase |
| `POST` | `/{id}/receive` | Receive a purchase — increments stock, publishes `INVENTORY_SYNC` per line |
| `GET` | `/{id}` | Get a purchase by ID |
| `GET` | `/order/{orderNumber}` | Get a purchase by order number |
| `GET` | `/` | List all purchases |
| `DELETE` | `/{id}` | Delete a purchase |

Statuses: `DRAFT`, `PENDING`, `RECEIVED`, `COMPLETED`, `CANCELLED`.
Line items: `{ productId, quantity, unitPrice?, batchNumber? }`.

### Returns — `/api/returns`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Create a return |
| `PUT` | `/{id}` | Update a return |
| `POST` | `/{id}/approve` | Approve a return — adjusts stock, publishes `INVENTORY_SYNC` |
| `GET` | `/{id}` | Get a return by ID |
| `GET` | `/number/{returnNumber}` | Get a return by return number |
| `GET` | `/` | List all returns |
| `DELETE` | `/{id}` | Delete a return |

`type` ∈ `PURCHASE_RETURN`, `SALE_RETURN`; `status` ∈ `PENDING`, `APPROVED`,
`COMPLETED`, `REJECTED`.

### Stock batches — `/api/stock-batches`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Create a batch (schedules a delayed `EXPIRY_SCAN`) |
| `PUT` | `/{id}` | Update a batch (schedules a delayed `EXPIRY_SCAN`) |
| `GET` | `/{id}` | Get a batch by ID |
| `GET` | `/product/{productId}` | List batches for a product |
| `GET` | `/` | List all batches |
| `DELETE` | `/{id}` | Delete a batch |

Fields: `productId`, `warehouseId`, `batchNumber`, `expiryDate`, `quantity`,
`costPrice`, `active`.

### Stock movements — `/api/stock-movements`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Record a movement — applies the stock effect, publishes `LOW_STOCK_SCAN` |
| `GET` | `/product/{productId}` | Movements for a product |
| `GET` | `/warehouse/{warehouseId}` | Movements for a warehouse |
| `GET` | `/` | List all movements |

`movementType` ∈ `IN`, `OUT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`,
`RETURN`, `SALE`, `PURCHASE`. `referenceType` ∈ `PURCHASE`, `SALE`, `TRANSFER`,
`ADJUSTMENT`, `RETURN`, `SYSTEM` (blank defaults to `SYSTEM`).

Stock effect: `IN`/`TRANSFER_IN`/`RETURN`/`PURCHASE` increase, `OUT`/`TRANSFER_OUT`/
`SALE` decrease (clamped at zero), `ADJUSTMENT` sets an absolute value. A
non-positive quantity is rejected with `400`.

### Alerts — `/api/alerts`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | All alerts (resolved and unresolved), newest first |
| `GET` | `/unresolved` | Only unresolved alerts |
| `GET` | `/product/{productId}` | Alerts for a product |
| `POST` | `/{id}/resolve` | Resolve an alert (stamps `resolvedAt`) |
| `POST` | `/` | Create an alert manually |
| `POST` | `/generate` | Publish an `ALERT_SCAN` message → `202 Accepted` |

`type` ∈ `LOW_STOCK`, `EXPIRY_WARNING`, `EXPIRY_ALERT`. Expiry warnings use a
7-day window (`AlertService.DEFAULT_EXPIRY_WARNING_WINDOW_DAYS`).

### Background jobs — `/api/jobs`

Observability and manual dispatch for the JobRunr broker. Backs the frontend
**Background Jobs** console.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/stats` | Live broker counters (total, awaiting, scheduled, enqueued, processing, failed, succeeded, recurring jobs, worker servers, `pending`) |
| `GET` | `/schedules` | The recurring schedules registered with the broker |
| `GET` | `/messages?limit=25` | Recent message history (audit trail), newest first |
| `GET` | `/types` | Message types with `value`, `label`, `description`, `dispatchable` |
| `POST` | `/dispatch` | Publish a message on demand |

**Dispatch body**

```json
{ "type": "INVENTORY_SYNC", "productId": 42 }
```

Response:

```json
{ "correlationId": "3f1c…", "messageType": "INVENTORY_SYNC", "status": "ENQUEUED" }
```

`LOW_STOCK_SCAN` and `INVENTORY_SYNC` require `productId`; `ORDER_CONFIRMATION` is
rejected because only the sales workflow may produce it.

---

## Domain Model

### Entities

| Entity | Table | Key relationships |
|--------|-------|-------------------|
| `Product` | `products` | → `Category`, `Brand`, `Supplier` |
| `Category` | `categories` | — |
| `Brand` | `brands` | — |
| `Supplier` | `suppliers` | — |
| `Customer` | `customers` | — |
| `Warehouse` | `warehouses` | — |
| `Sale` | `sales` | → `Customer`, `Warehouse`; ⇒ `SaleItem` (cascade ALL, eager) |
| `SaleItem` | `sale_items` | → `Sale`, `Product` |
| `Purchase` | `purchases` | → `Supplier`, `Warehouse`; ⇒ `PurchaseItem` (cascade ALL, eager) |
| `PurchaseItem` | `purchase_items` | → `Purchase`, `Product` |
| `Return` | `returns` | → `Product`, `Warehouse` |
| `StockBatch` | `stock_batches` | → `Product`, `Warehouse` |
| `StockMovement` | `stock_movements` | → `Product`, `Warehouse` |
| `Alert` | `alerts` | → `Product` |
| `JobMessageLog` | `job_message_log` | Audit trail for the broker (no FK) |

All entities use Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`,
`@AllArgsConstructor`, `@Builder`) and `GenerationType.IDENTITY` surrogate keys.

### Status enums

| Enum | Values |
|------|--------|
| `Sale.SaleStatus` | `DRAFT`, `PENDING`, `COMPLETED`, `CANCELLED`, `RETURNED` |
| `Purchase.PurchaseStatus` | `DRAFT`, `PENDING`, `RECEIVED`, `COMPLETED`, `CANCELLED` |
| `Return.ReturnType` | `PURCHASE_RETURN`, `SALE_RETURN` |
| `Return.ReturnStatus` | `PENDING`, `APPROVED`, `COMPLETED`, `REJECTED` |
| `StockMovement.MovementType` | `IN`, `OUT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`, `RETURN`, `SALE`, `PURCHASE` |
| `StockMovement.ReferenceType` | `PURCHASE`, `SALE`, `TRANSFER`, `ADJUSTMENT`, `RETURN`, `SYSTEM` |
| `Alert.AlertType` | `LOW_STOCK`, `EXPIRY_WARNING`, `EXPIRY_ALERT` |
| `JobMessageLog.JobMessageStatus` | `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` |

Enums are persisted as strings (`@Enumerated(EnumType.STRING)`), so the database
stays readable and reordering a Java enum can never corrupt existing rows.

### Key invariants

- `Product.sku`, `Sale.invoiceNumber`, `Purchase.purchaseOrderNumber`,
  `Return.returnNumber` and `JobMessageLog.correlationId` are unique.
- Low-stock condition: `currentStock <= reorderLevel` (both must be non-null).
- A product raises at most one **unresolved** alert of a given type — sweeps are
  idempotent, so running them repeatedly does not create duplicates.
- Stock is never negative: sale completion validates availability, and `OUT`
  movements are clamped at zero.

---

## Messaging & Background Jobs

The backend is **event-driven**: domain services publish typed messages to an
in-process broker and dedicated consumers do the derived work off the HTTP request
thread.

```
REST controller ──▶ domain service ──▶ DomainMessagePublisher
                                            │  (publishes after commit)
                                            ▼
                              JobRunr broker (PostgreSQL-backed)
                                            │
                                            ▼
                                   JobRequestHandler consumers
                                   (background worker pool)
```

### Guarantees & design

- **Broker**: JobRunr (`jobrunr-spring-boot-4-starter`). Jobs live in the same
  PostgreSQL database as the domain data — no separate broker required. JobRunr
  creates its own `jobrunr_jobs` and `jobrunr_recurring_jobs` tables on first start.
- **At-least-once delivery** with automatic retries and exponential back-off
  (`jobrunr.jobs.default-number-of-retries`, `retry-back-off-time-seed`), plus
  retention policies for succeeded/deleted jobs.
- **Dual-write safety**: producers call `DomainMessagePublisher.publishAfterCommit`,
  which registers a transaction synchronization so the message is only enqueued
  once the business change has committed. Called outside a transaction, it
  publishes immediately.
- **Debouncing**: `scheduleAfterCommit(message, delay)` coalesces a burst of
  related writes into one delayed scan — stock-batch create/update uses a 5-second
  delay so importing many batches triggers a single expiry sweep.
- **Audit trail**: every publish writes a `job_message_log` row
  (`ENQUEUED → RUNNING → SUCCEEDED|FAILED`) that the UI console renders.
  `AbstractJobConsumer.handle(...)` performs the state transitions and rethrows so
  JobRunr can retry.
- **Payload rules**: messages extend `AbstractJobMessage` and carry a denormalised
  snapshot plus ids — never JPA entities — so they stay small, JSON-serialisable
  and survive restarts. Subclasses need a no-arg constructor and bind themselves
  to a consumer via `getJobRequestHandler()`.

### Message catalogue

| Routing key (`JobMessageType`) | Produced by | Consumer |
|--------------------------------|-------------|----------|
| `ALERT_SCAN` | nightly schedule, console, `POST /api/alerts/generate` | `AlertJobConsumer` |
| `LOW_STOCK_SCAN` | sale completion, stock movements, console | `AlertJobConsumer` |
| `EXPIRY_SCAN` | stock batch create/update (5s delay), console | `ExpiryJobConsumer` |
| `INVENTORY_SYNC` | purchase receipt, return approval, console | `InventoryJobConsumer` |
| `ORDER_CONFIRMATION` | sale creation | `NotificationJobConsumer` |
| `DAILY_INVENTORY_REPORT` | morning schedule, console | `InventoryReportJobConsumer` |

`JobMessageType` is the single source of truth for the messaging contract: adding
a value automatically exposes it to `GET /api/jobs/types`, and each value declares
a `label`, a `description` and whether it is `dispatchable` from the console
(`ORDER_CONFIRMATION` is not).

### Recurring schedules

Declared once in `JobSchedules` and registered by `RecurringJobPublisher` via
`@Recurring`:

| Schedule id | Cron | Purpose |
|-------------|------|---------|
| `ims-inventory-alert-sweep` | `0 0 2 * * *` (02:00 daily) | Full low-stock + expiry sweep |
| `ims-daily-inventory-report` | `0 30 7 * * *` (07:30 daily) | Inventory valuation digest |

Each `@Recurring` method only publishes a message, so the schedule survives
restarts, is visible in the dashboard, and can be triggered on demand from the
console.

The raw JobRunr dashboard is served at <http://localhost:8000/dashboard>
(no authentication, consistent with the rest of the application).

---

## Caching (Redis)

Read-heavy endpoints are cached through Spring's cache abstraction with a
`RedisCacheManager` (`CacheConfig`). Each region has its own TTL, tuned to how
volatile the underlying table is:

| Cache (`CacheNames`) | TTL | Populated by |
|----------------------|-----|--------------|
| `categories`, `brands`, `warehouses` | 30 min | `getAll` / `getById` |
| `customers`, `suppliers` | 15 min | `getAll` / `getById` |
| `products` | 5 min | `getAll` / `getById` / `getBySku` / `getByBarcode` |
| `lowStockProducts` | 2 min | `getLowStockProducts` (full scan) |

Unlisted regions fall back to a 10-minute default TTL, and null values are never
cached.

Writes evict the affected region with `@CacheEvict(allEntries = true)`, so a
create/update/delete can never leave a stale list behind. Two details are easy to
get wrong and are handled explicitly:

- **Cross-service invalidation.** Sales, purchases, returns and stock movements
  all mutate `Product.currentStock`, so each of those methods carries a
  `@Caching` block that evicts `products` *and* `lowStockProducts` — otherwise
  cached stock levels would drift from the database.
- **Value serialization.** `GenericJackson3RedisSerializer` records the concrete
  type alongside the JSON, so a cached `List<ProductDto>` deserializes back to
  `List<ProductDto>` instead of a list of untyped maps. Keys use the prefix
  `ims::<cache>::`, so entries are readable in `redis-cli`.

```bash
redis-cli KEYS 'ims::*'
redis-cli GET 'ims::products::all'
```

> Redis is **not** the job store — JobRunr keeps its jobs in PostgreSQL. If Redis
> is unavailable, reads fall through to the database; the cache is a performance
> optimisation, not a correctness dependency.

---

## Rate Limiting (Redis)

`RateLimitFilter` limits every `/api/**` request to
`app.rate-limit.requests-per-window` per `app.rate-limit.window-seconds`
(default 120 per 60s), keyed by client IP.

- Implemented as a **servlet filter** (`@Order(HIGHEST_PRECEDENCE + 10)`), not an
  MVC interceptor, so it also covers requests that never reach a handler (404s,
  static lookups) and rejects them before any database or cache work happens.
- The counter increment and TTL assignment run in a **single Lua script**
  (`RateLimiterService`), so the check is atomic. A plain `INCR` + `EXPIRE` pair
  can leave a key with no expiry if the process dies between the two calls,
  permanently locking a client out.
- Successful responses carry `X-RateLimit-Limit`, `X-RateLimit-Remaining` and
  `X-RateLimit-Reset`.
- Rejected requests get **`429 Too Many Requests`** with `Retry-After` plus the
  same `X-RateLimit-*` headers. The filter writes its own JSON body (it sits
  outside the `@RestControllerAdvice` path); `RateLimitExceededException` is also
  mapped in `GlobalExceptionHandler` for service-layer usage.
- `X-Forwarded-For` is only honoured when `app.rate-limit.trust-forwarded-for=true`
  (the default), since the header is client-controlled and would otherwise let a
  caller bypass the limit by inventing a new address per request.
- If Redis is unreachable the limiter **fails open** and logs a warning:
  availability of the inventory API takes precedence over strictness.

```bash
# Fire 130 requests and watch the tail get a 429
for i in $(seq 1 130); do
  curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/products
done | sort | uniq -c
```

---

## Error Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) translates exceptions into a
consistent `ApiErrorResponse` envelope so the UI can always show an actionable
message instead of raw text:

```json
{
  "timestamp": "2026-03-12T14:05:31.221",
  "status": 400,
  "error": "Bad Request",
  "message": "Warehouse ID is required",
  "path": "/api/sales",
  "details": { "warehouseId": "Warehouse ID is required" }
}
```

| Exception | Status | Message |
|-----------|--------|---------|
| `MethodArgumentNotValidException` | `400` | Joined field errors; `details` maps field → message |
| `IllegalArgumentException` | `400` | The exception message (e.g. `Product not found: 42`) |
| `HttpMessageNotReadableException` | `400` | `Malformed request body or unsupported value.` |
| `NoSuchElementException` | `404` | `The requested record could not be found.` |
| `DataIntegrityViolationException` | `409` | `This record is referenced by other data or violates a uniqueness constraint.` |
| `RateLimitExceededException` | `429` | Rate-limit message + `Retry-After` header |
| `Exception` | `500` | `An unexpected error occurred. Please try again.` |

The frontend's `services/api.ts` parses this envelope into a typed `ApiError`
(`status`, `message`, `details`) and `getErrorMessage()` normalizes anything else
into a display-safe string. A network failure surfaces as
`Cannot reach the server. Is the backend running?`.

---

## Security Notes

- **No authentication or authorization is enforced.** Every API endpoint is
  publicly accessible, as are the JobRunr dashboard and the background-job
  console. Do not expose this application to an untrusted network as-is.
- The `users`, `roles` and `user_roles` tables were removed along with their
  entities, since nothing performed authentication and the `password` column was
  never read. Re-introduce them together with Spring Security if authentication is
  added later.
- The `created_by` audit columns on sales/purchases/returns/stock movements were
  also removed — no code ever populated them.
- Message payloads carry a `requestedBy` label that is **informational only**; it
  is not an authenticated identity.
- **CORS** is restricted to the origins in `app.cors.allowed-origins`. In
  production the SPA is served by nginx which proxies `/api` to the backend, so the
  browser stays on a single origin and CORS is not exercised.
- **Rate limiting** provides baseline abuse protection (see above), but it is not
  a substitute for authentication.
- **Secrets**: database credentials currently live in
  `application.properties` for local development. Supply them via environment
  variables (`SPRING_DATASOURCE_*`) in any real deployment.

---

## Testing

### Backend — `cd backend && ./mvnw test`

Tests run against **H2 in PostgreSQL mode** with the JobRunr worker pool and
dashboard disabled (see [Test configuration](#test-configuration)). No external
PostgreSQL or Redis is required.

| Test class | Coverage |
|------------|----------|
| `BackendApplicationTests` | `@SpringBootTest` context load — catches bean-wiring, cache-manager and JobRunr configuration mistakes |
| `config/GenericJackson3RedisSerializerTest` | Round-trips DTOs and lists, restores element types, handles null/empty, rejects malformed or unknown-type payloads |
| `messaging/DomainMessagePublisherTest` | Publishes immediately without a transaction; defers until commit; schedules with a delay |
| `messaging/consumer/AbstractJobConsumerTest` | Marks the audit row `RUNNING → SUCCEEDED`, and `FAILED` + rethrow on error |
| `ratelimit/RateLimiterServiceTest` | Atomic Lua limiter decisions and fail-open behaviour |
| `ratelimit/RateLimitFilterTest` | Header emission, 429 body, non-API paths skipped, forwarded-header handling |
| `service/impl/AlertServiceImplTest` | Low-stock and expiry sweeps, boundary day, default window fallback, deduplication |
| `service/impl/StockMovementServiceImplTest` | Stock arithmetic per movement type, zero-clamp, `ADJUSTMENT` semantics, validation, `LOW_STOCK_SCAN` publication |
| `service/impl/BackgroundJobServiceImplTest` | Manual dispatch per message type, required `productId`, refusal of `ORDER_CONFIRMATION` |

Unit tests use Mockito (`@ExtendWith(MockitoExtension.class)`) and AssertJ, with
descriptive `@DisplayName` annotations.

### Frontend — `cd frontend`

```bash
npm run lint        # ESLint + react-hooks + react-refresh rules
npx tsc -b          # Type-check (also run as part of `npm run build`)
npm run build       # Production bundle; fails on any type error
```

---

## Docker & Deployment

Each application has a multi-stage Dockerfile with a non-root runtime.

### Backend — `backend/Dockerfile`

- **Build stage**: `eclipse-temurin:17-jdk` + the Maven wrapper. Dependencies are
  resolved in a separate layer (`dependency:go-offline`) so a source edit does not
  re-download the world.
- **Runtime stage**: `eclipse-temurin:17-jre`, unprivileged `ims` user (uid 1001),
  `JAVA_OPTS=-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport` so the JVM respects
  the container memory limit.
- Exposes `8080` (API) and `8000` (dashboard). Configuration comes from
  environment variables.

```bash
docker build -t ims-backend ./backend
docker run -p 8080:8080 -p 8000:8000 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/ims_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=mysecretpassword \
  -e REDIS_HOST=host.docker.internal \
  ims-backend
```

### Frontend — `frontend/Dockerfile`

- **Build stage**: `node:22-alpine`, `npm ci` then `npm run build` (which runs
  `tsc -b && vite build`, so a type error fails the image build).
- **Runtime stage**: `nginx:1.27-alpine` serving the SPA with:
  - history fallback (`try_files $uri $uri/ /index.html`) so client-side routes
    never 404;
  - `/api/` proxied to `http://backend:8080` on the shared Docker network;
  - content-hashed assets cached for a year (`immutable`), `index.html` never cached;
  - gzip for text assets;
  - a `HEALTHCHECK` that probes `/`.

```bash
docker build -t ims-frontend ./frontend
docker run -p 80:80 ims-frontend
```

### Topology notes

- PostgreSQL and Redis are **external dependencies**: point the app at them with
  `SPRING_DATASOURCE_*`, `REDIS_HOST` and `REDIS_PORT`.
- The frontend's nginx expects a backend container reachable as **`backend`** on
  the same Docker network — name the service accordingly (or adjust the
  `proxy_pass` in `frontend/Dockerfile`).
- The backend also runs the JobRunr worker pool, so scaling it horizontally adds
  workers automatically; JobRunr coordinates them through the shared database.
- `spring.jpa.hibernate.ddl-auto=update` is convenient for development; switch to
  a migration tool (Flyway/Liquibase) plus `validate` for production.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Backend fails at startup with a property-binding error | `spring.jackson.serialization.write-dates-as-timestamps` was set | Remove it — Jackson 3 moved the flag to `DateTimeFeature` |
| `Cannot reach the server. Is the backend running?` in the UI | Backend not running, or the Vite proxy target is wrong | Start the backend; check `VITE_PROXY_TARGET` (default `http://localhost:8080`) |
| `429 Too Many Requests` during manual testing | The rate limiter is working | Raise `RATE_LIMIT_REQUESTS` / `RATE_LIMIT_WINDOW_SECONDS`, or set `RATE_LIMIT_ENABLED=false` locally |
| Cached values look stale in `redis-cli` | Entries outlive a write that did not evict them | Check the service method carries the right `@CacheEvict` / `@Caching`, then flush: `redis-cli --scan --pattern 'ims::*' \| xargs redis-cli DEL` |
| Cached lists come back as maps/`LinkedHashMap` | Custom serializer not wired | `CacheConfig` must serialize values with `GenericJackson3RedisSerializer` |
| JobRunr jobs never run | Worker pool disabled, or no consumer bound | Ensure `jobrunr.background-job-server.enabled=true` and the payload implements `getJobRequestHandler()` |
| Job stuck in `ENQUEUED` in the console | No `BackgroundJobServer` polling | Check the `backgroundJobServers` counter on `GET /api/jobs/stats` |
| Duplicate alerts for one product | Deduplication bypassed by a change to the unresolved-alert query | Sweeps skip when an unresolved alert of the same type/product already exists |
| Redis is down | Cache and limiter degraded | Reads fall through to the database and the limiter fails open (warnings logged); restart Redis to restore caching |
| `relation "products" does not exist` | Wrong database, or `ddl-auto` not `update` | Verify `spring.datasource.url` points at `ims_db` |
| Frontend build fails on a type error | `npm run build` runs `tsc -b` | Fix the reported type error; run `npx tsc -b` for the full list |

---

## Conventions

**Backend**

- Package-by-layer inside `com.unique.ims.backend`; controllers stay thin and
  services own transactions, mapping and messaging.
- Interfaces in `service/`, implementations in `service/impl/` suffixed
  `*ServiceImpl`.
- Constructor injection only; no field injection or `@Autowired`.
- DTOs cross the HTTP boundary — JPA entities never leave the service layer
  (`spring.jpa.open-in-view=false` makes this mandatory, not optional).
- Enums persisted as strings; unique business keys declared on the column.
- Cache region names live only in `CacheNames`; message routing keys only in
  `JobMessageType`; recurring schedules only in `JobSchedules` — one source of
  truth per concern.
- Every new `JobMessageType` gets a payload class in `messaging/request/`, a
  consumer in `messaging/consumer/`, and (if console-triggerable) a `dispatch`
  branch in `BackgroundJobServiceImpl`.
- Javadoc explains *why* a non-obvious decision was made (dual-write safety,
  atomic Lua limiting, cross-service cache invalidation).

**Frontend**

- TypeScript strict mode; shared DTOs and status unions live in `types/api.ts`.
- One Redux slice per resource in `store/`, with thunks named
  `fetch*` / `create*` / `update*` / `delete*` (plus `complete*`, `approve*`,
  `receive*`, `resolve*`, `generate*` for workflow actions).
- All HTTP calls go through `services/api.ts`; errors surface via `ApiError` +
  `getErrorMessage`.
- Reuse the design-system primitives in `components/ui` and the generic
  `CrudPage` / `CrudField` scaffolding rather than hand-rolling new screens.
- Routes and labels are declared once in `components/navigation.ts`.
- Accessibility is expected: labelled controls, ARIA error wiring, focus-trapped
  dialogs and keyboard-operable destructive actions.

---

<div align="center">

**IMS — Inventory Management System**

Built with Spring Boot 4, React 19, PostgreSQL, Redis and JobRunr.

</div>

