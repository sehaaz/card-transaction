# Transit Card API

A microservices-based REST API built with Spring Boot for managing transit cards and recording financial transactions (top-ups and payments). The system is fully containerized with Docker Compose.

---

## Architecture Overview

<!-- Architecture diagram: Replace this comment with your architecture diagram image -->
<!-- ![Architecture Diagram](docs/architecture.png) -->

The project follows a **microservices architecture** with the **Database-per-Service** pattern. There are two independently deployable Spring Boot services, each owning its own PostgreSQL database. All four components (two services + two databases) run inside a shared Docker Compose network.

**card-service** is responsible for the card domain. It exposes REST endpoints on port 8081 for creating transit cards, retrieving card details, listing all cards, and updating card balances (top-up). It connects to its own PostgreSQL database called `carddb`, exposed externally on port 5433. This service has no knowledge of transactions; it only manages card state.

**transaction-service** is responsible for the transaction domain. It exposes REST endpoints on port 8082 for recording new transactions (TOPUP or PAYMENT), listing all transactions, and retrieving transactions filtered by card ID. It connects to its own PostgreSQL database called `transactiondb`, exposed externally on port 5434.

The critical inter-service communication happens when `transaction-service` records a new transaction. After persisting the transaction record in its own database, it makes a synchronous REST call to `card-service` via `RestTemplate` to update the card balance. For a TOPUP, it sends the amount as-is to be added. For a PAYMENT, it first fetches the card's current balance from `card-service` to validate that the card has sufficient funds, then sends the negated amount to be subtracted. Inside Docker, this communication uses the Docker DNS service name (`http://card-service:8081`), so the services resolve each other without hardcoded IPs.

Both services follow a **layered architecture** internally: Controller (handles HTTP and Swagger documentation) -> Service (business logic and validation) -> Repository (Spring Data JPA for database access). Request bodies are received as typed DTOs rather than raw maps, decoupling the API contract from the internal entity structure. Each service has a centralized `GlobalExceptionHandler` using `@RestControllerAdvice` that catches specific custom exceptions and returns a consistent `ErrorResponse` JSON structure with an error code, HTTP status, message, and timestamp.

Write operations (`createCard`, `topUp`, `recordTransaction`) are annotated with `@Transactional` to ensure atomicity — if any step fails, the database changes roll back. Read operations (`getCardById`, `getAllCards`, `getAllTransactions`, `getTransactionsByCardId`) use `@Transactional(readOnly = true)`, which hints the underlying JDBC connection and Hibernate session to skip dirty checking and optimize for read-only access.

Schema management is handled automatically by Hibernate with `ddl-auto=update`, which creates or alters tables on startup to match the entity definitions. Each service is packaged as a fat JAR via Spring Boot Maven Plugin and containerized using a lightweight `eclipse-temurin:24-jdk-alpine` Docker image.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 24 |
| Framework | Spring Boot 3.4.1 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Inter-service Communication | RestTemplate |
| API Documentation | springdoc-openapi (Swagger UI) |
| Containerization | Docker + Docker Compose |
| Build Tool | Maven |
| Boilerplate Reduction | Lombok |

---

## Data Models

### Card Entity (card-service)

| Field | Type | Description |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `cardNumber` | String | Auto-generated UUID, unique |
| `ownerName` | String | Full name of the card owner |
| `balance` | BigDecimal | Current balance (default: 0) |
| `createdAt` | LocalDateTime | Card creation timestamp |

### Transaction Entity (transaction-service)

| Field | Type | Description |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `cardId` | Long | Reference to the card |
| `amount` | BigDecimal | Transaction amount |
| `type` | String | `TOPUP` or `PAYMENT` |
| `createdAt` | LocalDateTime | Transaction timestamp |

---

## DTOs (Data Transfer Objects)

### CreateCardRequest

Used by `POST /api/cards` to create a new transit card.

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `ownerName` | String | Yes | Full name of the card owner | `"John Doe"` |

### TopUpRequest

Used by `PUT /api/cards/{id}/topup` to add balance to a card.

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `amount` | BigDecimal | Yes | Amount to add to the card balance | `50.00` |

### TransactionRequest

Used by `POST /api/transactions` to record a new transaction.

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `cardId` | Long | Yes | ID of the card associated with this transaction | `1` |
| `amount` | BigDecimal | Yes | Transaction amount (must be greater than zero) | `25.50` |
| `type` | String | Yes | Transaction type. Allowed values: `TOPUP`, `PAYMENT` | `"TOPUP"` |

### ErrorResponse

Returned by both services for all error responses.

| Field | Type | Description | Example |
|---|---|---|---|
| `timestamp` | String | Timestamp when the error occurred | `"2026-05-01T14:30:00"` |
| `status` | int | HTTP status code | `404` |
| `error` | String | Error type identifier code | `"CARD_NOT_FOUND"` |
| `message` | String | Human-readable error description | `"Card not found with id: 99"` |

---

## API Endpoints

### Card Service (http://localhost:8081)

Swagger UI: http://localhost:8081/swagger-ui.html

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/cards` | Create a new card |
| `GET` | `/api/cards` | List all cards |
| `GET` | `/api/cards/{id}` | Get card by ID |
| `PUT` | `/api/cards/{id}/topup` | Top up card balance |

### Transaction Service (http://localhost:8082)

Swagger UI: http://localhost:8082/swagger-ui.html

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/transactions` | Record a transaction |
| `GET` | `/api/transactions` | List all transactions |
| `GET` | `/api/transactions/{cardId}` | Get transactions by card ID |

---

## Error Handling

Both services return a consistent error response structure:

```json
{
  "timestamp": "2026-05-01T14:30:00",
  "status": 400,
  "error": "INVALID_AMOUNT",
  "message": "Invalid amount: -5. Amount must be greater than zero"
}
```

### card-service Error Codes

| Error Code | HTTP Status | Trigger |
|---|---|---|
| `CARD_NOT_FOUND` | 404 | Card ID does not exist |
| `INVALID_OWNER_NAME` | 400 | Owner name is null or blank |
| `INVALID_AMOUNT` | 400 | Amount is null or zero |
| `INVALID_PARAMETER` | 400 | Path parameter type mismatch (e.g., non-numeric ID) |
| `MALFORMED_REQUEST` | 400 | Request body is missing or invalid JSON |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

### transaction-service Error Codes

| Error Code | HTTP Status | Trigger |
|---|---|---|
| `INVALID_CARD_ID` | 400 | Card ID is null |
| `INVALID_AMOUNT` | 400 | Amount is null, zero, or negative |
| `INVALID_TRANSACTION_TYPE` | 400 | Type is not `TOPUP` or `PAYMENT` |
| `INSUFFICIENT_BALANCE` | 422 | Card balance is less than payment amount |
| `CARD_SERVICE_ERROR` | 502 | card-service is unreachable or returned an error |
| `INVALID_PARAMETER` | 400 | Path parameter type mismatch |
| `MALFORMED_REQUEST` | 400 | Request body is missing or invalid JSON |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## How to Run

### Prerequisites

- Docker & Docker Compose
- Java 24
- Maven

### Build & Start

```bash
# Build card-service
cd card-service
mvn clean package -DskipTests
cd ..

# Build transaction-service
cd transaction-service
mvn clean package -DskipTests
cd ..

# Start all services
docker-compose up --build
```

### Stop

```bash
docker-compose down
```

### Stop & Remove Volumes (reset databases)

```bash
docker-compose down -v
```

---

## Usage Examples (curl)

### Create a card

```bash
curl -X POST http://localhost:8081/api/cards \
  -H "Content-Type: application/json" \
  -d '{"ownerName": "John Doe"}'
```

### List all cards

```bash
curl http://localhost:8081/api/cards
```

### Get card by ID

```bash
curl http://localhost:8081/api/cards/1
```

### Top up card balance

```bash
curl -X PUT http://localhost:8081/api/cards/1/topup \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00}'
```

### Record a TOPUP transaction

```bash
curl -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"cardId": 1, "amount": 50.00, "type": "TOPUP"}'
```

### Record a PAYMENT transaction

```bash
curl -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"cardId": 1, "amount": 10.00, "type": "PAYMENT"}'
```

### List all transactions

```bash
curl http://localhost:8082/api/transactions
```

### Get transactions for a specific card

```bash
curl http://localhost:8082/api/transactions/1
```

---

## Docker Services

| Service | Image / Build | Internal Port | External Port |
|---|---|---|---|
| `card-db` | `postgres:15` | 5432 | 5433 |
| `transaction-db` | `postgres:15` | 5432 | 5434 |
| `card-service` | `./card-service` | 8081 | 8081 |
| `transaction-service` | `./transaction-service` | 8082 | 8082 |

graph TD
    Client([Kentkart Mobil Uygulama / Validatör]) -->|REST API :8082| TS[Transaction Service]
    Client -->|REST API :8081| CS[Card Service]
    
    TS -->|Senkron REST Çağrısı| CS
    
    TS -->|Spring Data JPA| TDB[(Transaction DB :5434)]
    CS -->|Spring Data JPA| CDB[(Card DB :5433)]
    
    classDef service fill:#f9f,stroke:#333,stroke-width:2px;
    classDef db fill:#bbf,stroke:#333,stroke-width:2px;
    class TS,CS service;
    class TDB,CDB db;

sequenceDiagram
    participant V as Validatör (Client)
    participant TS as Transaction Service
    participant CS as Card Service
    participant DB as Databases
    
    V->>TS: POST /api/transactions (Kart No, Tutar, PAYMENT)
    Note over TS: İşlem tipi kontrolü
    TS->>CS: GET /api/cards/{id} 
    CS-->>TS: Güncel Bakiye Bilgisi
    
    alt Yetersiz Bakiye
        TS-->>V: 422 INSUFFICIENT_BALANCE
    else Yeterli Bakiye
        TS->>DB: İşlemi "Transaction DB"ye Kaydet
        TS->>CS: PUT /api/cards/{id}/topup (Negatif Tutar)
        CS->>DB: Bakiyeyi "Card DB"de Güncelle
        CS-->>TS: 200 OK (Bakiye Düşüldü)
        TS-->>V: 200 OK (Ödeme Başarılı)
    end