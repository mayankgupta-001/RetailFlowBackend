# RetailFlow — Backend

**RetailFlow** is a barcode-based inventory and billing system for small retail businesses. The core idea: a shop owner generates a barcode for each product, then uses a barcode scanner or their phone's camera to scan items and generate a bill at checkout, with stock levels updating automatically behind the scenes.

This repository is the backend service — a Spring Boot REST API that currently handles product management and inventory tracking. Billing, barcode generation, and analytics are actively being built out next.

> 🚧 **Status:** Early-stage, in active development. This README reflects what's implemented today, not the full end-state vision.

## Tech Stack

- **Java 25**
- **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Security)
- **PostgreSQL** (primary datastore)
- **H2** (runtime, for local/testing use)
- **Docker Compose** (Postgres container for local dev)
- **Maven** (build tool, via Maven Wrapper)
- **Lombok**

## Features (current)

- **Product Management**
  - Create, read, update, delete products
  - Look up a product by ID or by barcode
  - Tracks name, barcode, selling price, cost price, stock, minimum stock threshold, and category
- **Inventory Tracking**
  - Adjust stock via `STOCK_IN`, `STOCK_OUT`, or `ADJUSTMENT` transactions
  - Every adjustment is logged with previous stock, new stock, quantity, and an optional reason
  - View a product's full inventory/stock history

## Roadmap

- [ ] Barcode generation for new products (auto-generate + render a scannable barcode)
- [ ] Billing module — scan items, generate a bill, auto-decrement stock
- [ ] Bill history (view/list past bills)
- [ ] Analytics dashboard (sales trends, low-stock alerts, top products)
- [ ] Authentication & role-based access (currently all endpoints are open)

## Project Structure

```
src/main/java/com/creator/RetailFlow/
├── product/
│   ├── controller/     # ProductController — /api/products
│   ├── service/        # ProductService
│   ├── repository/     # ProductRepository
│   ├── entity/         # Product
│   └── dto/             # CreateProductRequest, UpdateProductRequest, ProductResponse
├── inventory/
│   ├── controller/     # InventoryController — /api/inventory
│   ├── service/        # InventoryService
│   ├── repository/     # InventoryLogRepository
│   ├── entity/         # InventoryLog, InventoryTransactionType
│   └── dto/             # StockAdjustmentRequest, InventoryLogResponse
├── config/              # SecurityConfig
└── exception/           # GlobalExceptionHandler, ResourceNotFoundException

src/main/resources/
├── application.properties
└── migration/           # SQL schema (products, inventory_logs)
```

## API Endpoints

### Products — `/api/products`

| Method | Endpoint                  | Description                  |
|--------|----------------------------|-------------------------------|
| POST   | `/api/products`            | Create a new product         |
| GET    | `/api/products`            | Get all products              |
| GET    | `/api/products/{id}`       | Get a product by ID          |
| GET    | `/api/products/barcode/{barcode}` | Get a product by barcode |
| PUT    | `/api/products/{id}`       | Update a product              |
| DELETE | `/api/products/{id}`       | Delete a product              |

### Inventory — `/api/inventory`

| Method | Endpoint                              | Description                          |
|--------|-----------------------------------------|----------------------------------------|
| POST   | `/api/inventory/adjust`                | Adjust stock (`STOCK_IN` / `STOCK_OUT` / `ADJUSTMENT`) |
| GET    | `/api/inventory/logs/product/{productId}` | Get inventory history for a product |

## Database Schema

**`products`**
`id, name, barcode (unique), selling_price, cost_price, stock, min_stock, category, created_at, updated_at`

**`inventory_logs`**
`id, product_id (FK → products), type, quantity, previous_stock, new_stock, reason, created_at`

## Getting Started

### Prerequisites
- Java 25
- Docker (for Postgres via Compose)

### 1. Set environment variables
The datasource reads credentials from the environment:
```
DATABASE_USER=your_username
DATABASE_PASS=your_password
```

### 2. Start Postgres
```bash
docker compose up -d
```
This spins up a Postgres 16 container (`retailflow-postgres`) with a `retailflow` database on port `5432`.

### 3. Run the app
```bash
./mvnw spring-boot:run
```

The app applies schema changes automatically (`spring.jpa.hibernate.ddl-auto=update`); the SQL files under `src/main/resources/migration` document the schema.

## Notes

- Security is currently wide open (`permitAll()` on all requests) — auth is not implemented yet; see Roadmap.
- The SQL files under `src/main/resources/migration` document the schema; the app currently applies schema changes via Hibernate (`ddl-auto=update`) rather than a migration tool like Flyway.

## Related

- Frontend repo: _link here once available_
