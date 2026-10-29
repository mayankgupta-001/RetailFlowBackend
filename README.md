# RetailFlow Backend

A production-oriented **Spring Boot REST API for retail inventory, billing, sales, and stock management**.

RetailFlow is a full-stack retail management system designed for small and medium-sized businesses to manage products, track inventory, process sales, generate invoices, and maintain reliable stock records.

This repository contains the **backend service**, including business logic, authentication, database access, inventory management, checkout, sales processing, and REST APIs consumed by the RetailFlow frontend.

---

## 🏗️ Project Repositories

RetailFlow is divided into separate frontend and backend repositories.

| Component | Repository | Technology |
|---|---|---|
| Frontend | [RetailFlowFrontend](https://github.com/mayankgupta-001/RetailFlowFrontend) | React, Vite, Tailwind CSS |
| Backend | **RetailFlowBackend** | Java, Spring Boot, PostgreSQL |

---

## ✨ Features

### 🔐 Authentication & Security

- JWT-based authentication
- User authentication and authorization
- Secure password handling
- Protected API endpoints
- Role-based endpoint authorization

### 📦 Product Management

- Create products
- Update products
- Delete products
- Retrieve product details
- Product listing
- Search products by barcode
- Selling price and cost price management
- Stock tracking
- Minimum stock thresholds
- Product categories
- Automatic barcode generation

### 📊 Inventory Management

- Stock-in transactions
- Stock-out transactions
- Manual stock adjustments
- Inventory transaction history
- Previous and new stock tracking
- Low-stock threshold support
- Transaction reasons
- Automatic inventory updates during checkout
- Inventory restoration when a sale is voided

### 🛒 Sales & Checkout

- Cart-based checkout
- Multiple products per sale
- Duplicate product aggregation
- Product and quantity validation
- Stock availability validation
- Automatic inventory deduction
- Multiple payment methods
- Transactional checkout
- Unique invoice generation
- Subtotal calculation
- Discount handling
- Final total calculation

### 🧾 Invoice & Sale Management

- Unique invoice number generation
- Invoice lookup
- Retrieve individual sales
- Historical sale lookup
- Invoice reprinting support
- Sale voiding
- Prevention of duplicate sale voids
- Automatic inventory restoration after voiding

### 📈 Sales Reports

- Sales reports by date range
- Latest sales ordering
- Historical sales lookup
- Sale subtotal, discount, and total information
- Payment method information

### 🏷️ Barcode Support

- Automatic barcode generation
- Barcode storage with product records
- Barcode lookup
- Barcode image generation using ZXing
- Barcode-scanner-oriented product workflows

---

## 🧠 Backend Engineering

RetailFlow is designed to go beyond basic CRUD operations by implementing important retail business rules directly in the backend.

### Transactional Checkout

Checkout is executed inside a database transaction.

The general flow is:

```text
Receive Cart
     ↓
Validate Request
     ↓
Aggregate Product Quantities
     ↓
Lock Products
     ↓
Validate Stock
     ↓
Calculate Subtotal
     ↓
Validate Discount
     ↓
Create Sale Items
     ↓
Reduce Inventory
     ↓
Calculate Final Total
     ↓
Save Sale
```

If a critical operation fails, the transaction can roll back instead of leaving inventory and sales data in an inconsistent state.

### Concurrent Stock Protection

During checkout, products are locked before stock is modified.

Products are also processed in a consistent order to reduce the possibility of database deadlocks when multiple checkouts operate on overlapping products.

### Sale Voiding

Voiding a completed sale automatically restores the associated inventory.

```text
Completed Sale
      ↓
   Void Sale
      ↓
Restore Inventory
      ↓
Mark Sale as Voided
```

This keeps inventory records consistent with the sale history.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java 25 | Backend language |
| Spring Boot 4.1 | Application framework |
| Spring Web MVC | REST API development |
| Spring Data JPA | Database access |
| Hibernate | ORM |
| Spring Security | Authentication and authorization |
| JWT | Stateless authentication |
| PostgreSQL | Primary database |
| H2 | Local/testing database |
| Maven | Build and dependency management |
| Docker Compose | Local development environment |
| ZXing | Barcode generation |
| Jakarta Validation | Request validation |
| Lombok | Boilerplate reduction |

---

## 🏗️ Architecture

RetailFlow follows a layered backend architecture.

```text
                    ┌──────────────────────┐
                    │   RetailFlow         │
                    │      Frontend        │
                    │ React + Vite         │
                    └──────────┬───────────┘
                               │
                            REST API
                               │
                               ▼
                    ┌──────────────────────┐
                    │   RetailFlow         │
                    │      Backend         │
                    │   Spring Boot        │
                    ├──────────────────────┤
                    │ Controllers          │
                    │ DTOs                 │
                    │ Services             │
                    │ Repositories         │
                    │ Security             │
                    │ Exception Handling   │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      PostgreSQL      │
                    └──────────────────────┘
```

### Request Flow

```text
HTTP Request
     ↓
Controller
     ↓
Validation / DTO
     ↓
Service
     ↓
Repository
     ↓
Database
     ↓
Response
```

Business rules are primarily handled in the service layer rather than being implemented directly in controllers.

---

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/com/creator/RetailFlow/
│   │
│   ├── config/
│   │   └── SecurityConfig.java
│   │
│   ├── auth/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── product/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── util/
│   │
│   ├── inventory/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── sale/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   │
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── ResourceNotFoundException.java
│
└── resources/
    ├── application.properties
    └── migration/
```

---

## 🔌 API Overview

### Authentication

Base path:

```text
/api/auth
```

Responsible for user authentication and JWT token generation.

### Products

Base path:

```text
/api/products
```

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/products` | Create product |
| `GET` | `/api/products` | Get products |
| `GET` | `/api/products/{id}` | Get product |
| `GET` | `/api/products/{id}/barcode-image` | Get barcode image |
| `GET` | `/api/products/barcode/{barcode}` | Find product by barcode |
| `PUT` | `/api/products/{id}` | Update product |
| `DELETE` | `/api/products/{id}` | Delete product |

### Inventory

Base path:

```text
/api/inventory
```

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/inventory/adjust` | Adjust stock |
| `GET` | `/api/inventory/logs/product/{productId}` | Get product inventory history |

### Sales

Base path:

```text
/api/sales
```

The sales module provides:

- Checkout
- Invoice generation
- Invoice lookup
- Sale retrieval
- Sales reports
- Sale voiding

---

## 🗄️ Database Model

PostgreSQL is used as the primary database.

Core entities include:

```text
users
products
inventory_logs
sales
sale_items
```

### Product

```text
id
name
barcode
selling_price
cost_price
stock
min_stock
category
created_at
updated_at
```

### Inventory Log

```text
id
product_id
type
quantity
previous_stock
new_stock
reason
created_at
```

### Sale

```text
id
invoice_number
subtotal
discount
total
payment_method
created_at
voided
```

### Sale Item

```text
id
sale_id
product_id
product_name
barcode
price
quantity
subtotal
```

---

## 🚀 Getting Started

### Prerequisites

Make sure you have the following installed:

- Java 25
- Maven
- Docker
- Docker Compose
- PostgreSQL if running without Docker

### 1. Clone the Repository

```bash
git clone https://github.com/mayankgupta-001/RetailFlowBackend.git

cd RetailFlowBackend
```

### 2. Configure Environment Variables

Create a `.env` file containing the required configuration.

Example:

```env
DATABASE_USER=your_username
DATABASE_PASS=your_password
JWT_SECRET=your_secure_jwt_secret
```

> **Important:** Never commit real credentials, passwords, tokens, or other secrets to GitHub.

### 3. Start PostgreSQL

Using Docker Compose:

```bash
docker compose up -d
```

### 4. Run the Application

Using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend will start on the configured Spring Boot port.

---

## 🧪 Testing

Run the complete test suite with:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Current tests cover important checkout scenarios including:

- Successful checkout
- Empty cart
- Missing product
- Insufficient stock
- Invalid product ID
- Invalid quantity
- Negative discount
- Discount greater than subtotal

Additional integration and API-level tests are planned as the project evolves.

---

## 🛒 Checkout Example

Example checkout request:

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "discount": 25.00,
  "paymentMethod": "CASH"
}
```

The backend processes the request as:

```text
Product Price × Quantity
          ↓
       Subtotal
          ↓
        Discount
          ↓
      Final Total
```

Inventory is updated and the sale is persisted as part of the checkout transaction.

---

## Development

RetailFlow is actively developed and maintained through GitHub Issues and pull requests.

Current development work, bug fixes, feature proposals, and improvements are tracked through the repository's issue tracker.

Before implementing a significant change:

1. Check existing issues.
2. Open or discuss an issue for larger changes.
3. Create a feature or fix branch.
4. Implement and test the change.
5. Open a pull request with a clear description of the changes.

The README focuses on the current capabilities and architecture of RetailFlow rather than maintaining a separate feature roadmap.

---

## 🎯 Project Goals

RetailFlow is being developed with a focus on practical backend engineering and real-world business requirements rather than only basic CRUD functionality.

Key engineering goals include:

- Secure authentication
- Reliable transactional operations
- Consistent inventory management
- Backend-controlled business rules
- Database integrity
- Testable service logic
- Clear separation of responsibilities
- Maintainable REST APIs
- Reliable error handling
- Production-oriented development practices

---

## 🤝 Contributing

Contributions, bug reports, feature requests, and improvements are welcome.

Before contributing, please review the project's contribution guidelines.

If you find a bug or want to propose a feature, open a GitHub Issue with enough information to reproduce or understand the request.

---

## 🔗 Related Project

### RetailFlow Frontend

The frontend provides the user interface for interacting with the RetailFlow backend.

**Repository:**  
https://github.com/mayankgupta-001/RetailFlowFrontend

---

## 👨‍💻 Author

**Mayank Gupta**

B.Tech CSE (Hons) — Artificial Intelligence & Analytics  
GLA University, Mathura

---

## 📄 License

This project is currently under development and is intended for educational, portfolio, and open-source development purposes.
