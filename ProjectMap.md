# CoreLedger - Core Banking API / ProjectMap

**Version**: 0.0.1-SNAPSHOT
**Framework**: Spring Boot 3.2.3 + Java 21
**Architecture**: Hexagonal (Ports & Adapters) + Domain-Driven Design (DDD)
**Structure**: Multi-module Maven monolith with bounded contexts

---

## Project Overview

CoreLedger is a production-grade core banking REST API demonstrating enterprise patterns for financial systems. It implements accounts management, inter-account transfers, and user KYC (Know Your Customer) operations using event-driven architecture with Kafka.

**Key Philosophical Principles**:

- **Zero framework in domain**: Domain models are pure Java; Spring/JPA live in infrastructure only
- **Immutable ledger**: Financial records are append-only; no UPDATE operations on ledger rows
- **Optimistic locking**: Concurrency control via version fields, not pessimistic locks
- **Event-driven boundaries**: Modules communicate via domain events through shared kernel, never direct calls
- **Strict port contracts**: Domain ports (in/) are use-case interfaces; driven ports (out/) are repository/adapter interfaces

---

## Architecture

### Module Dependencies

```
app (Spring Boot entry point)
├── account-module
├── transfer-module
├── user-module
└── shared-kernel (pure Java — all modules depend on this)
```

### Package Structure (per bounded context)

```
com.coreledger.<module>/
├── domain/
│   ├── model/              # Aggregate roots, entities, value objects
│   ├── events/             # Domain events published by this context
│   └── exceptions/         # Domain rule violations
├── application/
│   ├── port/
│   │   ├── in/             # Driving ports — use case interfaces
│   │   └── out/            # Driven ports — repository/external service interfaces
│   ├── service/            # Application services orchestrating use cases
│   └── dto/                # Application-level DTOs (if needed)
└── infrastructure/
    ├── persistence/        # JPA entities, repositories, persistence adapters
    ├── web/                # Controllers, request/response DTOs
    └── config/             # Spring configuration, exception handlers
```

---

## Module Breakdown

### 1. **shared-kernel** — Pure Java Foundation

**Responsibility**: Cross-cutting domain primitives; framework-agnostic
**Dependencies**: Validation API only, no Spring in domain

#### What's Implemented

- **DomainEventPublisher** interface — abstraction for event publication
- **AuditMetadata** — timestamp, actor, correlation ID for audit trails
- **Domain events base classes** — foundation for event bus

**Related Files**:

- [shared-kernel/pom.xml](shared-kernel/pom.xml)
- [shared-kernel/src/main/java/com/coreledger/shared/](shared-kernel/src/main/java/com/coreledger/shared/)

#### Identified Gaps

- **NO Money/Currency value objects yet** — Critical for multi-currency banking. Should support arbitrary precision (BigDecimal), currency codes, rounding rules
- **NO structured DomainEvent hierarchy** — Events should carry version info, aggregate ID, timestamp, source context

---

### 2. **account-module** — Account Lifecycle & Ledger

**Responsibility**: Account creation, withdrawal/deposit, balance derivation, account status lifecycle
**Core Pattern**: Account balance is **derived** from append-only transaction ledger, never stored directly

#### Domain Model

**Account Aggregate Root**:

- `accountNumber`: String (business ID)
- `balance`: BigDecimal (derived from ledger)
- `status`: enum (ACTIVE, FROZEN, CLOSED)
- `version`: Integer (optimistic locking)

**Account Behaviors**:

- `deposit(amount)` — validates account active, amount > 0, adds to balance
- `withdraw(amount, minimumBalance)` — validates funds available after, prevents going below minimum
- Factory method `Account.open(accountNumber, initialDeposit)` — creates new account with version=0

**Status Transitions**:

```
ACTIVE → FROZEN (manual freeze)
ACTIVE → CLOSED (manual close)
FROZEN → ACTIVE (unfreeze)
```

#### Use Cases Implemented

| Use Case            | Port Interface                   | Status                           |
| ------------------- | -------------------------------- | -------------------------------- |
| Create account      | `CreateAccountUseCase`           | ✅ Implemented                   |
| Get account details | (Not yet exposed via controller) | ❌ Missing endpoint              |
| List all accounts   | (Not yet exposed via controller) | ❌ Missing endpoint (admin only) |
| Deposit money       | (Called internally by transfer)  | ⚠️ Partial                       |
| Withdraw money      | (Called internally by transfer)  | ⚠️ Partial                       |
| Freeze account      | Not implemented                  | ❌ Gap                           |
| Unfreeze account    | Not implemented                  | ❌ Gap                           |
| Close account       | Not implemented                  | ❌ Gap                           |

#### Persistence Layer

**JPA Entity**: `AccountJpaEntity`

- Contains `@Version` annotation for Hibernate optimistic locking
- Unique constraint on `accountNumber`
- Table: `accounts`

**Repository Adapter**: `AccountPersistenceAdapter` implements:

- `LoadAccountPort` — `loadAccount(accountNumber)` returns Optional
- `UpdateAccountStatePort` — `save(account)` applies version increment
- `CreateAccountPort` — `insert(account)` for new accounts

**Related Files**:

- Domain: [account-module/src/main/java/com/coreledger/account/domain/Account.java](account-module/src/main/java/com/coreledger/account/domain/Account.java)
- Application: [account-module/src/main/java/com/coreledger/account/application/](account-module/src/main/java/com/coreledger/account/application/)
- Persistence: [account-module/src/main/java/com/coreledger/account/infrastructure/persistence/](account-module/src/main/java/com/coreledger/account/infrastructure/persistence/)
- Web: [account-module/src/main/java/com/coreledger/account/infrastructure/web/](account-module/src/main/java/com/coreledger/account/infrastructure/web/)

#### Identified Gaps

- **NO transaction ledger table** — Balance is currently stored directly; should derive from immutable ledger rows
- **NO account status endpoints** — Freeze/unfreeze/close operations missing
- **NO balance history** — Cannot audit historical balance calculations
- **NO minimum balance enforcement** — Transfers check it, but account creation doesn't
- **NO account type/product mapping** — All accounts treated identically (checking vs savings vs investment)
- **NO overdraft policy** — Cannot configure per-account or per-account-type
- **Sparse validation on account number** — Should validate format (length, chars, IBAN?)

---

### 3. **transfer-module** — Inter-Account Movement & State Machine

**Responsibility**: Transfer orchestration, status lifecycle, account ledger appends
**Core Pattern**: Event sourcing via append-only `transfer_ledger` table; transfers never UPDATE, only INSERT new versions

#### Domain Model

**Transfer Aggregate Root**:

- `transferId`: String / UUID (business ID)
- `sourceAccountNumber`: String
- `targetAccountNumber`: String
- `amount`: BigDecimal
- `status`: enum (PENDING → COMPLETED/FAILED)
- `createdAt`: Instant
- `completedAt`: Instant (null until completion)
- `version`: int (incremented on each state transition)

**Transfer Behaviors**:

- Factory `Transfer.initiate(...)` — creates PENDING transfer with version=0
- `complete(now)` — transitions PENDING → COMPLETED, increments version
- `fail()` — transitions PENDING → FAILED, increments version
- Rehydration constructor `Transfer.rehydrate(...)` — reconstructs from ledger row

**Enforced Rules**:

```
1. Cannot transfer to same account
2. Amount must be > 0
3. Only PENDING transfers can complete or fail
4. Sender must have sufficient balance
5. Sender balance cannot go below minimum
```

#### Use Cases Implemented

| Use Case                       | Port Interface            | Status          |
| ------------------------------ | ------------------------- | --------------- |
| Initiate transfer (internal)   | `InitiateTransferUseCase` | ✅ Implemented  |
| Get transfer status            | `GetTransferUseCase`      | ❌ Missing impl |
| List transfers by account      | Not scoped                | ❌ Missing      |
| Cancel transfer (PENDING only) | Not scoped                | ❌ Gap          |
| Reverse transfer (COMPLETED)   | Not scoped                | ❌ Gap          |
| Schedule transfer (future)     | Not scoped                | ❌ Gap          |

#### Persistence Layer

**JPA Entity**: `TransferJpaEntity`

- Append-only pattern: `@UniqueConstraint(columnNames = {"transferId", "version"})` prevents duplicate versions
- No UPDATE operations — each state change is a new INSERT
- Table: `transfer_ledger`
- Query: `findTopByTransferIdOrderByVersionDesc()` gets latest version

**Repository Adapter**: `TransferPersistenceAdapter`

- Implements `TransferRepositoryPort`
- Uses `@Transactional(propagation = Propagation.REQUIRES_NEW)` to isolate ledger appends from parent transaction
- Converts domain `Transfer` to `TransferJpaEntity`

**Flow in InitiateTransferUseCase**:

1. Create PENDING transfer, append to ledger
2. Load both accounts from persistence
3. Execute domain logic: sender.withdraw(), recipient.deposit()
4. Save both accounts (via optimistic locking)
5. Transition transfer → COMPLETED, append to ledger
6. On exception: Transition → FAILED, append to ledger (catches race conditions)

**Related Files**:

- Domain: [transfer-module/src/main/java/com/coreledger/transfer/domain/Transfer.java](transfer-module/src/main/java/com/coreledger/transfer/domain/Transfer.java)
- Application: [transfer-module/src/main/java/com/coreledger/transfer/application/TransferService.java](transfer-module/src/main/java/com/coreledger/transfer/application/TransferService.java)
- Persistence: [transfer-module/src/main/java/com/coreledger/transfer/infrastructure/persistence/](transfer-module/src/main/java/com/coreledger/transfer/infrastructure/persistence/)
- Web: [transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/](transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/)

#### Identified Gaps

- **NO inter-bank transfers** — Only internal (same bank) transfers supported
- **NO idempotency keys** — Duplicate requests could create duplicate transfers; missing tracking of idempotent request IDs
- **NO transfer compensation/reversal** — Once COMPLETED, cannot reverse; no refund mechanism
- **NO transfer fee calculation** — Fixed amounts only, no percentage-based or tiered fees
- **NO scheduled transfers** — All transfers execute immediately
- **NO batch transfers** — Cannot send one-to-many or many-to-many
- **NO transfer limits per account** — Missing daily/monthly transfer caps
- **NO circuit breaker for account service** — If account loads fail, transfer fails hard; should have fallback
- **NO Dead Letter Queue (DLQ) setup** — Kafka failures not captured for retry/investigation
- **NO transaction audit trail** — Cannot trace who initiated transfer (missing user context)
- **Missing source/dest account validation** — No check if accounts exist before appending PENDING

---

### 4. **user-module** — User Management & KYC

**Responsibility**: User lifecycle, Know Your Customer (KYC) verification, document management, tier-based access control

#### Domain Model

**User Aggregate Root**:

- `userId`: UserId (value object / wrapper)
- `email`: String
- Status: (Created, Active, Suspended, Closed — not fully mapped to domain)

**KycProfile Aggregate Root** (separate aggregate):

- `id`: UUID
- `userId`: UserId (reference to User)
- `tier`: enum (TIER_1 → TIER_3 → TIER_4)
- `documents`: List<KycDocument>
- `verifiedAt`: Instant (null until all TIER_1 docs verified)

**KycDocument Entity**:

- `id`: UUID
- `profileId`: UUID
- `type`: enum (PASSPORT, NATIONAL_ID, UTILITY_BILL, BANK_STATEMENT, etc.)
- `status`: enum (PENDING, VERIFIED, REJECTED)
- `rejectionReason`: String (nullable)
- `uploadedAt`: Instant
- `verifiedAt`: Instant (nullable)

**KYC Tier Requirements**:

- **TIER_1**: Basic (can create account, small limits)
- **TIER_2**: Enhanced (identity document verified)
- **TIER_3**: Full (identity + proof of address verified)
- **TIER_4**: Premium (identity + address + wealth verification)

#### Use Cases Implemented

| Use Case                         | Port Interface             | Status                           |
| -------------------------------- | -------------------------- | -------------------------------- |
| Get KYC status for user          | `GetUserKycUseCase`        | ✅ Implemented                   |
| Upload KYC document              | `AddKycDocumentUseCase`    | ✅ Implemented                   |
| Verify document                  | `VerifyKycDocumentUseCase` | ✅ Implemented (publishes event) |
| Reject document                  | `RejectKycDocumentUseCase` | ✅ Implemented (publishes event) |
| Load document (metadata)         | `LoadKycDocumentUseCase`   | ✅ Implemented                   |
| Load document (binary)           | `LoadKycDocumentUseCase`   | ✅ Implemented                   |
| Update KYC tier (admin override) | `UpdateUserKycUseCase`     | ⚠️ Partial (no audit)            |
| Recalculate tier                 | `UpdateUserKycUseCase`     | ⚠️ Partial (incomplete logic)    |
| Delete document                  | `DeleteKycDocumentUseCase` | ✅ Implemented                   |
| Delete profile                   | `DeleteKycProfileUseCase`  | ✅ Implemented                   |

#### Persistence Layer

**JPA Entities**:

- `UserJpaEntity` — User aggregate
- `KycProfileJpaEntity` — KYC profile aggregate
- `KycDocumentJpaEntity` — Document metadata

**Repositories**:

- `UserJpaRepository`
- `KycProfileJpaRepository`
- `KycDocumentJpaRepository`

**Document Storage**:

- Abstract port: `DocumentStorageService`
- Binary documents stored separately (file system, S3, or blob storage)
- URL mapping: `/api/kyc/documents/{documentId}/content`

**Related Files**:

- Domain: [user-module/src/main/java/com/coreledger/user/domain/](user-module/src/main/java/com/coreledger/user/domain/)
- Application: [user-module/src/main/java/com/coreledger/user/application/service/UserKycService.java](user-module/src/main/java/com/coreledger/user/application/service/UserKycService.java)
- Persistence: [user-module/src/main/java/com/coreledger/user/infrastructure/persistence/](user-module/src/main/java/com/coreledger/user/infrastructure/persistence/)
- Web: [user-module/src/main/java/com/coreledger/user/infrastructure/web/](user-module/src/main/java/com/coreledger/user/infrastructure/web/)
- Document upload/storage [user-module/src/main/java/com/coreledger/user/infrastructure/storage/](user-module/src/main/java/com/coreledger/user/infrastructure/storage/)

#### Identified Gaps

- **NO user authentication framework** — No username/password, JWT tokens, or OAuth2 integration
- **NO liveness checks (re-verification)** — KYC verified once, never re-checked (e.g., annual verification)
- **NO sanctions/blacklist checking** — Missing OFAC/AML compliance checks
- **NO KYC document versioning** — Cannot track history of document uploads per type
- **NO expiration dates on verified documents** — Verified docs don't expire; passports/licenses do
- **NO tier downgrade rules** — Cannot auto-downgrade if documents expire
- **KycDocumentUploaded event NOT published** — Code is commented out; missing event for audit trail
- **NO audit trail for admin overrides** — Tier updates via UpdateUserKycUseCase bypass rules, no reason/audit recorded
- **Tier recalculation incomplete** — `recalculateTier()` has no-op logic, doesn't check document types
- **NO document removal from profile** — When doc is deleted, profile tier not recalculated
- **NO automated document classification** — All docs uploaded by user; no AI/ML classification or validation
- **NO bulk document upload** — Single document at a time only
- **NO parallel verification workflows** — Documents processed sequentially, no batch approval
- **NO integration with external KYC providers** — Manual verification only, no automated/third-party checkers

---

## API Endpoints

### Accounts

```
POST   /api/v1/accounts                    Create account
       Body: { "initialDeposit": 1000.00 }
       Response: { "accountNumber": "abc123", "balance": 1000.00, "status": "ACTIVE" }

GET    /api/v1/accounts/{accountNumber}    Get account details (⚠️ NOT EXPOSED — exists in service layer)
       Response: { "accountNumber", "balance", "status", "version" }

GET    /api/v1/accounts                    List all accounts (⚠️ NOT EXPOSED — admin only)
```

### Transfers

```
POST   /api/v1/transfers                   Initiate transfer
       Body: { "senderAccountNumber": "acc1", "recipientAccountNumber": "acc2", "amount": 500.00 }
       Response: { "transferId": "uuid", "status": "COMPLETED", "amount": 500.00, "createdAt": "..." }

GET    /api/v1/transfers/{transferId}      Get transfer status (⚠️ NOT EXPOSED — exists in service layer)
       Response: { "transferId", "status", "amount", "sourceAccount", "targetAccount", ... }
```

### Users & KYC

```
GET    /api/v1/users/{userId}/kyc          Get KYC status
       Response: { "userId", "tier": "TIER_1", "documents": [...], "verifiedAt": null }

POST   /api/v1/users/{userId}/kyc/documents      Upload KYC document
       Body: multipart/form-data { file, documentType: "PASSPORT" }
       Response: { "documentId": "uuid", "status": "PENDING", "tier": "TIER_1" }

POST   /api/v1/users/{userId}/kyc/documents/{docId}/verify      Verify document (admin)
       Response: { "documentId", "status": "VERIFIED", "tier": "TIER_2" }

POST   /api/v1/users/{userId}/kyc/documents/{docId}/reject       Reject document (admin)
       Body: { "reason": "Low quality" }
       Response: { "documentId", "status": "REJECTED", "tier": "TIER_1" }

GET    /api/v1/users/{userId}/kyc/documents/{docId}             Get document metadata
       Response: { "documentId", "type", "status", "uploadedAt", "verifiedAt" }

GET    /api/v1/users/{userId}/kyc/documents/{docId}/content     Download document binary
```

---

## Configuration & Infrastructure

### Application Configuration

**File**: [app/src/main/resources/application.yml](app/src/main/resources/application.yml)

**Profile-specific overrides**:

- `dev` profile: Real Postgres (matches prod behavior) + detailed logging
- `test` profile: H2 in-memory database
- `prod` profile: Full Postgres, minimal logging, strict error handling

**Key Settings**:

- `spring.jpa.open-in-view: false` — Prevents lazy-loading in web layer
- `server.error.include-stacktrace: never` — Never expose internals to client
- `app.banking.minimum-balance: 1000.00` — Enforced on all accounts

### Database Design

**Tables**:

1. **accounts**

   - PK: `id` (Long)
   - UK: `account_number` (String)
   - Fields: `balance`, `status`, `version` (optimistic lock)

2. **transfer_ledger** (append-only)

   - PK: `id` (Long)
   - UK: `(transfer_id, version)` — prevents duplicate state entries
   - Fields: `transfer_id`, `source_account`, `target_account`, `amount`, `status`, `created_at`, `completed_at`, `version`

3. **users**

   - PK: `id` (Long)
   - UK: `email` (String)

4. **kyc_profiles**

   - PK: `id` (UUID)
   - FK: `user_id`
   - Fields: `tier`, `verified_at`

5. **kyc_documents**
   - PK: `id` (UUID)
   - FK: `profile_id`
   - Fields: `type`, `status`, `uploaded_at`, `verified_at`, `rejection_reason`, `storage_path`

### Kafka Topics

**Configured Topics**:

- `coreledger.account.events` — Account domain events
- `coreledger.transfer.events` — Transfer domain events
- `coreledger.user.events` — User/KYC domain events

**Event Publishing**:

- `EventPublisher` bean in [app/src/main/java/com/coreledger/config/EventPublisher.java](app/src/main/java/com/coreledger/config/EventPublisher.java)
- Consumer config in [KafkaConsumerConfig](app/src/main/java/com/coreledger/config/KafkaConsumerConfig.java)

**Related Files**:

- [app/src/main/java/com/coreledger/config/KafkaTopicConfig.java](app/src/main/java/com/coreledger/config/KafkaTopicConfig.java)
- [app/src/main/java/com/coreledger/config/KafkaProducerConfig.java](app/src/main/java/com/coreledger/config/KafkaProducerConfig.java)
- [app/src/main/java/com/coreledger/config/KafkaConsumerConfig.java](app/src/main/java/com/coreledger/config/KafkaConsumerConfig.java)
- [app/src/main/java/com/coreledger/config/EventRegistry.java](app/src/main/java/com/coreledger/config/EventRegistry.java)

**Identified Gaps**:

- **NO Dead Letter Queue (DLQ)** — Failed messages not captured for retry/investigation
- **NO Consumer acknowledgment strategy** — Unclear if offset commits are automatic or manual
- **NO event versioning** — Events don't carry schema version; breaking changes not handled
- **NO saga/choreography orchestration** — No multi-step workflow management (e.g., transfer completion → settlement)
- **NO consumer lag monitoring** — No metrics/alerts for processing delays

### Database Migrations

**Tool**: Flyway (version 10.15.0)
**Location**: [app/src/main/resources/db/migration/](app/src/main/resources/db/migration/)

**Naming Convention**: `V{version}__{description}.sql`

**Related Files**:

- [app/pom.xml](app/pom.xml) — Flyway dependencies

**Identified Gaps**:

- **NO migration for transfer_ledger** — Manual table creation required
- **NO migration for kyc tables** — Manual table creation required
- **NO baseline/existing schema** — Fresh migrations from scratch, no historical data handling

### Logging & Monitoring

**Logback Configuration**: Default Spring Boot profile-based setup
**Levels**:

- `com.coreledger`: DEBUG
- `org.springframework.web`: INFO
- `org.hibernate.SQL`: WARN

**Identified Gaps**:

- **NO structured logging (JSON)** — Logs are plain text; difficult to parse/index in ELK
- **NO correlation IDs** — Cannot trace requests across services
- **NO distributed tracing (OpenTelemetry)** — No trace/span propagation for Kafka boundaries
- **NO metrics (Micrometer)** — No counters for transfers, account creates, KYC events
- **NO health checks** — No `/actuator/health` endpoint for Kubernetes readiness/liveness
- **NO custom error codes** — Error responses don't include unique error codes for client routing

---

## Event System

### Published Domain Events

**Account Events**:

- (No events published yet — all state changes are stored in accounts table)

**Transfer Events**:

- (No events published yet — all state changes are stored in transfer_ledger table)

**User/KYC Events**:

- `KycDocumentVerified(documentId, userId)` — Published when doc marked verified
- `KycDocumentRejected(documentId, userId, reason)` — Published when doc rejected
- `KycTierUpdated(profileId, userId, newTier)` — Published when tier changes
- `KycProfileDeleted(profileId, userId, deletedTier, reason)` — Published on profile deletion
- `KycDocumentUploaded(...)` — ❌ **NOT PUBLISHED** (code commented out — GAP)

**Event Publishing Pattern**:

```java
eventPublisher.publish(event, eventPublisher::publishUserEvent);
```

**Related Files**:

- Event classes: [user-module/src/main/java/com/coreledger/user/domain/events/](user-module/src/main/java/com/coreledger/user/domain/events/)
- Publisher: [app/src/main/java/com/coreledger/config/EventPublisher.java](app/src/main/java/com/coreledger/config/EventPublisher.java)
- Event Registry: [app/src/main/java/com/coreledger/config/EventRegistry.java](app/src/main/java/com/coreledger/config/EventRegistry.java)

**Identified Gaps**:

- **Account state changes not published** — No events when account created/updated/frozen/closed
- **Transfer state changes not published** — No events when transfer initiated/completed/failed
- **NO event replay/replay pattern** — Cannot rebuild aggregate state from event log
- **NO event sourcing storage** — Events are transient (Kafka only), not stored in DB for audit
- **NO outbox pattern** — No guaranteed event delivery; transactional boundary between entity save and event pub

---

## Testing

### Test Structure

**Unit Tests**:

- Domain logic tests (no Spring) — validates business rules
- Service tests (mocked ports) — validates orchestration

**Integration Tests**:

- Persistence adapter tests — validates JPA entity mapping and repository queries
- E2E tests (full Spring context) — validates flow end-to-end

**Test Tools**:

- JUnit 5
- Mockito (mocking)
- Spring Boot Test (test context)
- Spring Test (MockMvc for web layer)

**Example Test Files**:

- [account-module/src/test/java/...](account-module/src/test/java/com/coreledger/account/)
- [transfer-module/src/test/java/...](transfer-module/src/test/java/com/coreledger/transfer/)
- [user-module/src/test/java/...](user-module/src/test/java/com/coreledger/user/)

**Related Files**:

- [pom.xml](pom.xml) — Test dependencies (JUnit 5, Mockito, Spring Boot Test)

**Identified Gaps**:

- **NO contract tests** — No Pact tests for module boundaries
- **NO mutation testing** — Cannot verify test quality/coverage
- **NO load/stress tests** — No testing for concurrent transfer scenarios
- **NO property-based tests** — No QuickCheck-style generators for edge cases
- **NO test fixtures/builders** — Tests create entities inline, no reusable builders
- **Sparse test coverage on domain models** — Account/Transfer domain rules undertested compared to services

---

## Critical Gaps & TODOs

### High Priority — Domain/Business Logic

| Gap                                              | Impact                                        | Estimated Work |
| ------------------------------------------------ | --------------------------------------------- | -------------- |
| Append-only transaction ledger for accounts      | Cannot audit balance calculation history      | 2-3 days       |
| Optimistic locking validation errors not handled | Race conditions silent/unhandled              | 1 day          |
| Transfer reversal/refund mechanism               | Cannot undo transfers; critical for disputes  | 2-3 days       |
| Idempotency keys for transfers                   | Duplicate requests create duplicate transfers | 1 day          |
| Account status lifecycle (freeze/unfreeze/close) | Missing 30% of account operations             | 1 day          |
| KYC tier recalculation logic                     | Incomplete; cannot auto-downgrade on expiry   | 1 day          |
| KYC document expiration/re-verification          | Non-compliant with regulatory refresh cycles  | 1 day          |
| AML/Sanctions checking on user creation          | Missing compliance control                    | 2-3 days       |
| User authentication framework                    | Cannot control who does what                  | 2-3 days       |
| Transfer limits (daily/monthly caps)             | No protection against fraud/exposure          | 1 day          |

### Medium Priority — Infrastructure/Operations

| Gap                                             | Impact                                           | Estimated Work |
| ----------------------------------------------- | ------------------------------------------------ | -------------- |
| Structured logging (JSON) + correlation IDs     | Cannot trace requests in production              | 1 day          |
| Distributed tracing (OpenTelemetry)             | Blind when debugging cross-service flows         | 1-2 days       |
| Metrics (Micrometer) — counters, timers, gauges | No visibility into system behavior               | 1-2 days       |
| Health checks (/actuator/health)                | Cannot detect service degradation                | 0.5 day        |
| Dead Letter Queue for Kafka                     | Failed messages disappear                        | 1 day          |
| Event sourcing database (change data capture)   | Cannot replay events; audit trail incomplete     | 2-3 days       |
| Outbox pattern for transactional event publish  | Race condition between entity save and event pub | 1-2 days       |
| Rate limiting                                   | No protection against abuse/DoS                  | 1 day          |
| Request validation middleware                   | Missing centralized validation guard             | 1 day          |

### Medium Priority — API Completeness

| Gap                                     | Impact                             | Estimated Work |
| --------------------------------------- | ---------------------------------- | -------------- |
| GET /api/v1/accounts/{accountNumber}    | Cannot retrieve account state      | 0.5 day        |
| GET /api/v1/accounts (list, admin)      | Cannot audit all accounts          | 0.5 day        |
| GET /api/v1/transfers/{transferId}      | Cannot check transfer status       | 0.5 day        |
| GET /api/v1/transfers (list by account) | Cannot show user their transfers   | 0.5 day        |
| Account freeze/unfreeze endpoints       | Cannot block accounts              | 0.5 day        |
| Account closure endpoint                | Cannot deactivate accounts         | 0.5 day        |
| Transfer cancellation (PENDING only)    | Cannot abort in-flight transfers   | 0.5 day        |
| Batch transfer endpoint                 | Cannot send to multiple recipients | 1 day          |
| Scheduled transfer endpoint             | Cannot defer transfers             | 1 day          |

### Low Priority — Polish/Future

| Gap                                           | Impact                            | Estimated Work |
| --------------------------------------------- | --------------------------------- | -------------- |
| Money/Currency value objects in shared-kernel | Type safety for amounts           | 1 day          |
| Structured domain event hierarchy             | Better event versioning/routing   | 1 day          |
| Custom error codes (not HTTP status only)     | Better client-side error handling | 0.5 day        |
| Circuit breaker for cross-module calls        | Resilience when upstream slow     | 1 day          |
| Contract tests between modules                | Confidence in module boundaries   | 1-2 days       |
| Mutation testing                              | Test quality assurance            | 1 day          |
| Load/stress testing                           | Know capacity limits              | 1-2 days       |
| - Notification-module (planned)               | Alerts on transfer completion     | 2-3 days       |

---

## Getting Help

### How to Use This Map with an AI Assistant

When asking an AI to implement a feature or fix a gap:

1. **Identify the gap** from the sections above
2. **Provide the AI this file** so it understands:
   - The architecture and design patterns
   - Which files to read first
   - Which related features already exist
   - What dependencies might be affected
3. **Ask the AI to**:
   - Read the related files listed
   - Check for existing patterns (e.g., "See how TransferService orchestrates ports")
   - Implement consistently (e.g., follow the Hexagonal pattern)
   - Add tests (e.g., "Follow the test structure in transfer-module/src/test")
   - Document assumptions

### Example Conversation

```txt
You: "I need to add a GET /api/v1/accounts/{accountNumber} endpoint to view account details."

AI will reference this map to:
1. Find the existing AccountController in account-module
2. See that AccountService already has the logic (via LoadAccountPort)
3. Check if a GetAccountUseCase port exists (it doesn't — gap to fill)
4. Follow the pattern used in transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/GetTransferController.java
5. Add the port interface, implement in service, expose via controller
6. Match test coverage from AccountControllerTest
```

---

## Quick Navigation

**Startup**: [README.md](README.md)
**Development**: [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
**Docker Setup**: [docker-compose.yml](docker-compose.yml)
**Make Commands**: [Makefile](Makefile)
**Tech Details**: [Kafka patterns](Kafka.sample.md), [Event patterns](events%20patterns.sample.md)

---

**Last Updated**: March 17, 2026
**This document should be refreshed whenever new modules, features, or endpoints are added.**
