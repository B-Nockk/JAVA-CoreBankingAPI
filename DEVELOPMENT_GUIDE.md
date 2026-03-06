# DEVELOPMENT GUIDE — CoreLedger (Release 1)

## Architecture

Hexagonal (Ports & Adapters) + DDD, structured as a multi-module Maven monolith.
Modules are bounded contexts — each can be extracted into a microservice without domain changes.
Rule: domain has zero dependency on Spring, JPA, or any framework. Infrastructure depends on domain, never the reverse.

## Module Layout

```tree
coreledger/
├── pom.xml                  # Parent pom — declares all modules, manages versions
├── shared-kernel/           # Pure Java: Money, Currency, AuditMetadata, DomainEvent
├── account-module/          # Bounded context: account lifecycle + transaction ledger
├── transfer-module/         # Bounded context: transfer orchestration + status lifecycle
└── app/                     # Spring Boot entry point only — wires modules, holds config
```

## Package Structure (per module)

```tree
com.coreledger.<module>/
├── domain/
│   ├── model/               # Aggregate roots, entities, value objects
│   ├── events/              # Domain events (e.g. MoneyDeposited, TransferInitiated)
│   └── exceptions/          # Domain rule violations
├── application/
│   ├── port/
│   │   ├── in/              # Use case interfaces (driving ports)
│   │   └── out/             # Repository/external interfaces (driven ports)
│   └── service/             # Implements use cases, orchestrates domain
└── infrastructure/
    ├── persistence/         # JPA entities, Spring repositories, persistence adapters
    └── web/
        ├── request/         # Inbound DTOs
        └── response/        # Outbound DTOs
```

## Core Design Rules

- **Balance is never stored — it is derived** from the append-only transaction ledger
- **No UPDATE on financial records** — only INSERTs. Every state change is a new row
- **DTOs never enter the domain** — controllers map requests → domain calls, domain → response
- **Modules never call each other directly** — communication is via domain events through shared kernel
- **JPA entities are not domain models** — maintain separate classes for each
- **`AccountJpaRepository`** (Spring interface) vs **`LoadAccountPort`** (domain port) — never conflate these

## Implementation Order (per bounded context)

```tree
1. shared-kernel values first     # Money, Currency — everything depends on these
2. domain/model/                  # Aggregate root + child entities + value objects
3. domain/events/                 # What happened (MoneyDeposited, etc.)
4. domain/exceptions/             # Business rule violations
5. application/port/in/           # Use case interfaces
6. application/port/out/          # Repository port interfaces
7. application/service/           # Orchestration — implements use cases
8. infrastructure/persistence/    # JPA entities + Spring repo + persistence adapter
9. infrastructure/web/            # Controller + request/response DTOs
10. Tests                         # Unit (domain/service) → integration (persistence) → e2e (web)
```

## Endpoints (v1)

- `POST   /api/v1/accounts` — create account
- `GET    /api/v1/accounts/{accountNumber}` — get account details + derived balance
- `GET    /api/v1/accounts` — list all accounts (admin)
- `POST   /api/v1/transfers` — initiate internal transfer
- `GET    /api/v1/transfers/{transferId}` — get transfer status

## User Flows

1. Create account → returns account number and opening balance (zero)
2. View account → returns details and current balance derived from transaction ledger
3. Transfer → debit source, credit destination, record immutable transfer record

## Naming Conventions

| Thing              | Convention           | Example                  |
| ------------------ | -------------------- | ------------------------ |
| Package            | lowercase            | `com.coreledger.account` |
| Module/folder      | lowercase-hyphenated | `account-module`         |
| Class              | PascalCase           | `AccountService`         |
| Method / variable  | camelCase            | `getBalance()`           |
| Constant           | SCREAMING_SNAKE_CASE | `MAX_TRANSFER_LIMIT`     |
| JPA repo interface | `*JpaRepository`     | `AccountJpaRepository`   |
| Domain port        | `*Port`              | `LoadAccountPort`        |
| Use case interface | `*UseCase`           | `CreateAccountUseCase`   |

## Tech Stack

- Java 21, Spring Boot 4.0.3
- Spring Data JPA, PostgreSQL (prod), H2 (dev/test)
- Lombok, Bean Validation, SpringDoc OpenAPI
- Maven multi-module

## What Comes After Release 1

- `notification-module` — event-driven alerts on transfer completion
- `fx-module` — multi-currency exchange rate handling
- Kafka adapter — replace in-process event bus with real message broker
- Security — Spring Security + JWT
