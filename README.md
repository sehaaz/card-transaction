# Transit Card API

A microservices-based REST API built with Spring Boot for managing transit cards and recording financial transactions (top-ups and payments). Fully containerized with Docker Compose.

---

## Architecture Overview

- **Microservices:** Two independent Spring Boot services (`card-service` and `transaction-service`).
- **Database-per-Service:** Each service has its own PostgreSQL database (`carddb`, `transactiondb`).
- **Inter-service Communication:** `transaction-service` communicates synchronously via `RestTemplate` with `card-service` to verify and update card balances.
- **Layered Design:** Controller -> Service -> Repository pattern using DTOs for data transfer.
- **Error Handling:** Centralized `@RestControllerAdvice` provides consistent error responses.
- **API Documentation:** Interactive API documentation provided via `Swagger` for easy testing and exploration.
- **Dockerized:** All components run seamlessly in a shared Docker Compose network.

### High-Level Architecture

```mermaid
graph TB
    Client([Client / Mobile App / Validator])

    Client -->|"POST /api/cards/createCard\nGET /api/cards\nGET /api/cards/{id}\nPUT /api/cards/{id}/topup\n:8081"| CS_CTRL

    Client -->|"POST /api/transactions\nGET /api/transactions\nGET /api/transactions/{cardId}\n:8082"| TS_CTRL

    subgraph DOCKER["Docker Compose Network"]
        direction TB

        subgraph CS_BOX["card-service :8081"]
            direction TB
            CS_CTRL["CardController\n@RestController"]
            CS_DTO["CreateCardRequest | TopUpRequest\nErrorResponse"]
            CS_SVC["CardService\n@Transactional"]
            CS_REPO["CardRepository\nJpaRepository"]
            CS_EXC["GlobalExceptionHandler\nCardNotFoundException\nInvalidOwnerNameException\nInvalidAmountException"]

            CS_CTRL -->|"DTO"| CS_DTO
            CS_CTRL --> CS_SVC
            CS_SVC --> CS_REPO
            CS_CTRL -.->|"throws"| CS_EXC
            CS_SVC -.->|"throws"| CS_EXC
        end

        subgraph TS_BOX["transaction-service :8082"]
            direction TB
            TS_CTRL["TransactionController\n@RestController"]
            TS_DTO["TransactionRequest\nErrorResponse"]
            TS_SVC["TransactionService\n@Transactional"]
            TS_REST["RestTemplate"]
            TS_REPO["TransactionRepository\nJpaRepository"]
            TS_EXC["GlobalExceptionHandler\nInvalidCardIdException\nInvalidAmountException\nInvalidTransactionTypeException\nInsufficientBalanceException\nCardServiceException"]

            TS_CTRL -->|"DTO"| TS_DTO
            TS_CTRL --> TS_SVC
            TS_SVC --> TS_REPO
            TS_SVC --> TS_REST
            TS_CTRL -.->|"throws"| TS_EXC
            TS_SVC -.->|"throws"| TS_EXC
        end

        TS_REST -->|"GET /api/cards/{id}\n(balance check)\nPUT /api/cards/{id}/topup\n(update balance)"| CS_CTRL

        subgraph DB_LAYER["PostgreSQL 15 Databases"]
            direction LR
            CDB[("card-db\ncarddb :5433\n\nCards Table:\nid · cardNumber · ownerName\nbalance · createdAt")]
            TDB[("transaction-db\ntransactiondb :5434\n\nTransactions Table:\nid · cardId · amount\ntype · createdAt")]
        end

        CS_REPO -->|"JDBC"| CDB
        TS_REPO -->|"JDBC"| TDB
    end

    classDef client fill:#ffeaa7,stroke:#fdcb6e,stroke-width:2px,color:#2d3436
    classDef controller fill:#74b9ff,stroke:#0984e3,stroke-width:2px,color:#2d3436
    classDef service fill:#a29bfe,stroke:#6c5ce7,stroke-width:2px,color:#fff
    classDef repo fill:#55efc4,stroke:#00b894,stroke-width:2px,color:#2d3436
    classDef dto fill:#ffeaa7,stroke:#fdcb6e,stroke-width:1px,color:#2d3436
    classDef exc fill:#ff7675,stroke:#d63031,stroke-width:1px,color:#fff
    classDef db fill:#dfe6e9,stroke:#636e72,stroke-width:2px,color:#2d3436
    classDef rest fill:#fd79a8,stroke:#e84393,stroke-width:2px,color:#fff
    classDef docker fill:#f8f9fa,stroke:#2d3436,stroke-width:3px

    class Client client
    class CS_CTRL,TS_CTRL controller
    class CS_SVC,TS_SVC service
    class CS_REPO,TS_REPO repo
    class CS_DTO,TS_DTO dto
    class CS_EXC,TS_EXC exc
    class CDB,TDB db
    class TS_REST rest
    class DOCKER docker
```

---

## API Endpoints

### Card Service (`:8081`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/cards` | Create a new card |
| `GET` | `/api/cards` | List all cards |
| `GET` | `/api/cards/{id}` | Get card by ID |
| `PUT` | `/api/cards/{id}/topup` | Top up card balance |

### Transaction Service (`:8082`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/transactions` | Record a transaction (TOPUP or PAYMENT) |
| `GET` | `/api/transactions` | List all transactions |
| `GET` | `/api/transactions/{cardId}` | Get transactions by card ID |

---

## API Documentation (Swagger UI)

Both services include interactive Swagger UI for easy exploration and testing of the endpoints. 

Once the application is running via Docker Compose, you can access the Swagger documentation using the following links:

* **Card Service API Docs:** [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
* **Transaction Service API Docs:** [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

---

## Example Flows

### 1. Payment Flow

```mermaid
sequenceDiagram
    participant C as Client (Validator)
    participant TS as Transaction Service
    participant CS as Card Service
    participant TDB as Transaction DB
    participant CDB as Card DB
    
    C->>TS: POST /api/transactions (Card ID, Amount, PAYMENT)
    Note over TS: Validate request
    TS->>CS: GET /api/cards/{id} 
    CS-->>TS: Return Current Balance
    
    alt Insufficient Balance
        TS-->>C: 422 INSUFFICIENT_BALANCE
    else Sufficient Balance
        TS->>TDB: Save Transaction (PAYMENT)
        TS->>CS: PUT /api/cards/{id}/topup (Negative Amount)
        CS->>CDB: Deduct Balance
        CS-->>TS: 200 OK
        TS-->>C: 200 OK (Payment Successful)
    end
```

### 2. Top-Up Flow

```mermaid
sequenceDiagram
    participant C as Client (Mobile App)
    participant TS as Transaction Service
    participant CS as Card Service
    participant TDB as Transaction DB
    participant CDB as Card DB
    
    C->>TS: POST /api/transactions (Card ID, Amount, TOPUP)
    Note over TS: Validate request
    TS->>TDB: Save Transaction (TOPUP)
    TS->>CS: PUT /api/cards/{id}/topup (Positive Amount)
    CS->>CDB: Add Balance
    CS-->>TS: 200 OK
    TS-->>C: 200 OK (Top-up Successful)
```

---

## Tech Stack & Data Models

**Core Stack:** Java 24, Spring Boot 3.4.1, PostgreSQL 15, Docker Compose, Maven.

| Service | Entity | Key Fields |
|---|---|---|
| `card-service` | **Card** | `id`, `cardNumber` (UUID), `ownerName`, `balance`, `createdAt` |
| `transaction-service` | **Transaction** | `id`, `cardId`, `amount`, `type` (TOPUP/PAYMENT), `createdAt` |

---

## How to Run & Usage

**Prerequisites:** Docker, Java 24, Maven

```bash
# 1. Build Services
cd card-service
mvn clean package -DskipTests
cd ..

cd transaction-service
mvn clean package -DskipTests
cd ..

# 2. Start Application
docker-compose up --build

# 3. Stop & Clean DBs
docker-compose down -v
```