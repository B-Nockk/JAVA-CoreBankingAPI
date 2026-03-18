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

```txt
app (Spring Boot entry point)
├── account-module
├── transfer-module
├── user-module
└── shared-kernel (pure Java — all modules depend on this)
```

### Package Structure (per bounded context)

```txt
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

- `id`: AccountId (UUID, internal identifier)
- `accountNumber`: String (human-facing business ID)
- `ownerName`: String
- `currency`: Currency (e.g., NGN, USD)
- `status`: enum (ACTIVE, FROZEN, CLOSED)
- `transactions`: List<Transaction> (append-only ledger)
- `audit`: AuditMetadata (createdAt, createdBy)

**Account Behaviors**:

- `open(accountNumber, ownerName, currency, openedBy)` — factory method, creates new account with empty transactions
- `deposit(amount, reference, initiatedBy)` — creates DEPOSIT transaction, updates balance
- `withdraw(amount, reference, initiatedBy)` — creates WITHDRAWAL transaction, validates sufficient funds
- `creditTransfer(amount, transferId, initiatedBy)` — creates TRANSFER_IN transaction
- `debitTransfer(amount, transferId, initiatedBy)` — creates TRANSFER_OUT transaction

**Derived Balance**:

- Computed from the last transaction's `balanceAfter` field
- If no transactions exist, balance is zero
- Never stored directly; always derived on read

**Status Transitions**:

```txt
ACTIVE → FROZEN (manual freeze — credits still allowed)
ACTIVE → CLOSED (manual close — no credits or debits)
FROZEN → ACTIVE (unfreeze)
```

#### Append-Only Transaction Ledger (✅ Fully Implemented)

**Transaction Entity** (immutable domain object):

- `transactionId`: String (UUID)
- `accountId`: AccountId
- `type`: enum (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)
- `amount`: Money (currency-aware)
- `balanceAfter`: Money (snapshot of account balance after this transaction)
- `reference`: String (transfer ID, deposit ref, etc. — links transactions across aggregates)
- `audit`: AuditMetadata (createdAt, createdBy)

**Structural Immutability**:

- All fields `final`, set only at construction
- No setters anywhere
- Only package-private factory methods within Account (e.g., `Transaction.deposit(...)`, `Transaction.transferIn(...)`)
- External code **cannot create, modify, or delete transactions**

**Database Schema** (enforces immutability):

```sql
CREATE TABLE transactions (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL (UPDATABLE=FALSE),
  type VARCHAR(20) NOT NULL (UPDATABLE=FALSE),
  amount DECIMAL(19,4) NOT NULL (UPDATABLE=FALSE),
  balance_after DECIMAL(19,4) NOT NULL (UPDATABLE=FALSE),
  currency VARCHAR(3) NOT NULL (UPDATABLE=FALSE),
  reference VARCHAR NOT NULL (UPDATABLE=FALSE),
  created_at TIMESTAMP NOT NULL (UPDATABLE=FALSE),
  created_by VARCHAR NOT NULL (UPDATABLE=FALSE),

  FOREIGN KEY (account_id) REFERENCES accounts(id),
  INDEX idx_transactions_account_id (account_id),
  INDEX idx_transactions_created_at (created_at)
);
-- ⚠️ NO UPDATE or DELETE permissions on this table
-- ✅ Only INSERT allowed (append-only ledger)
```

**Benefits of This Pattern**:

1. **Auditability**: Every balance change is permanently recorded with timestamp + actor
2. **Point-in-time calculation**: Can determine balance at any historical moment by filtering transactions
3. **No race conditions**: Adding transactions is atomic; no UPDATE contention
4. **Compliance-ready**: Immutable ledger satisfies regulatory requirements (financial record retention)
5. **Integrity checks**: If `balanceAfter(tx[n])` ≠ sum of all prior transactions, system is corrupted (data integrity issue)

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

#### How Transaction Ledger Persists

**Persistence Adapter** ([AccountPersistenceAdapter.java](account-module/src/main/java/com/coreledger/account/infrastructure/persistence/AccountPersistenceAdapter.java)):

- Loads Account + all related transactions in one query (`findByAccountNumberWithTransactions`)
- Maps `TransactionJpaEntity` rows to domain `Transaction` objects
- When saving Account, cascades all new transactions to JPA, which INSERTs them
- Existing transactions are never UPDATEd (JPA respects `updatable=false`)

**Related Files**:

- Domain: [account-module/src/main/java/com/coreledger/account/domain/model/Transaction.java](account-module/src/main/java/com/coreledger/account/domain/model/Transaction.java)
- Domain: [account-module/src/main/java/com/coreledger/account/domain/model/Account.java](account-module/src/main/java/com/coreledger/account/domain/model/Account.java)
- JPA: [account-module/src/main/java/com/coreledger/account/infrastructure/persistence/TransactionJpaEntity.java](account-module/src/main/java/com/coreledger/account/infrastructure/persistence/TransactionJpaEntity.java)
- Adapter: [account-module/src/main/java/com/coreledger/account/infrastructure/persistence/AccountPersistenceAdapter.java](account-module/src/main/java/com/coreledger/account/infrastructure/persistence/AccountPersistenceAdapter.java)

#### Identified Gaps (Account)

- **NO account status endpoints** — Freeze/unfreeze/close operations exist in domain but not exposed via REST
- **NO transaction query/export API** — Cannot retrieve transaction history via endpoint
- **NO minimum balance enforcement at creation** — Only enforced during transfers
- **NO account type/product mapping** — All accounts treated identically (checking vs savings vs investment)
- **NO overdraft policy** — Cannot configure per-account or per-account-type limits
- **Sparse validation on account number** — Should validate format (length, chars, IBAN?)
- **NO transaction reconciliation** — Cannot detect ledger corruption (balanceAfter mismatch)

---

### 3. **transfer-module** — Inter-Account Movement & State Machine

**Responsibility**: Transfer orchestration, status lifecycle, inter-module event choreography
**Core Pattern**: Event-driven choreography between transfer and account modules; transfers and accounts publish events; each module reacts

#### Domain Model

**Transfer Aggregate Root**:

- `id`: TransferId (UUID)
- `sourceAccountNumber`: String
- `destinationAccountNumber`: String
- `amount`: Money (currency-aware)
- `status`: enum (INITIATED → DEBITED → COMPLETED/FAILED → REVERSED)
- `createdAt`: Instant
- `audit`: AuditMetadata
- `failureReason`: String (nullable)

**Transfer State Machine**:

```
Initiated
    ↓
  (publish TransferInitiated)
    ↓
  [Account module deducts from source]
    ↓
Debited
    ↓
  (MoneyWithdrawn event received)
    ↓
  [Account module adds to destination]
    ↓
Completed
    ↓
  (publish TransferCompleted)
    ↓
(later, if reversal requested)
    ↓
Reversed
```

**Transfer Behaviors** (domain model):

- Factory `Transfer.initiate(source, dest, amount, initiatedBy)` — creates INITIATED transfer
- `markDebited()` — transitions INITIATED → DEBITED
- `markCompleted()` — transitions DEBITED → COMPLETED (calls `isDebited()` check)
- `markFailed(reason)` — transitions any state → FAILED
- `markReversed()` — transitions any state → REVERSED

**Enforced Rules**:

```
1. Cannot transfer to same account
2. Amount must be > 0
3. Source and destination must use same currency
4. Only INITIATED transfers can be debited
5. Only DEBITED/INITIATED transfers can be completed
6. Sender must have sufficient balance
7. Sender balance cannot go below minimum
```

#### Event Choreography Flow (✅ Fully Implemented)

**Step-by-Step Execution**:

```
┌─────────────────────── Transfer Module ───────────────────────┐
│                                                                │
│  TransferService.execute(command)                             │
│  ├─ Verify both accounts exist & active                       │
│  ├─ Verify same currency                                      │
│  ├─ Create Transfer(INITIATED)                                │
│  ├─ Save transfer to DB                                       │
│  └─ publishTransferEvent(TransferInitiated)  ──────┐          │
│                                                     │          │
└─────────────────────────────────────────────────────────────────┘
                                                      │
                ┌─────────────────────────────────────┘
                │
┌───────── Account Module (Kafka Listener) ──────────┐
│                │                                    │
│                ↓                                    │
│ TransferEventHandler.onTransferEvent()             │
│                                                    │
│ handleTransferInitiated(TransferInitiated e):      │
│   ├─ Load source account                          │
│   ├─ source.debitTransfer(amount, transferId)     │
│   │   └─ Creates TRANSFER_OUT transaction         │
│   │   └─ Updates balance (immutable ledger)       │
│   ├─ Save source account (cascades transaction)  │
│   ├─ publishAccountEvent(MoneyWithdrawn)  ──┐    │
│   │                                          │    │
│   ├─ Load destination account                │    │
│   ├─ dest.creditTransfer(amount, transferId) │    │
│   │   └─ Creates TRANSFER_IN transaction     │    │
│   │   └─ Updates balance (immutable ledger)  │    │
│   ├─ Save destination account               │    │
│   └─ publishAccountEvent(MoneyDeposited)────┼─┐  │
│                                              │ │  │
└──────────────────────────────────────────────────┼──┘
                                               │  │
           ┌───────────────────────────────────┘  │
           │                                      │
┌─────── Transfer Module (Kafka Listener) ───────┐│
│          │                                     ││
│          ↓                                     ││
│ TransferService.onAccountEvent()              ││
│                                               ││
│ handleMoneyWithdrawn(MoneyWithdrawn e):       ││
│   ├─ Find transfer by reference               ││
│   ├─ transfer.markDebited()                   ││
│   └─ Save transfer                            ││
│                                               ││
│ handleMoneyDeposited(MoneyDeposited e):    ←──┘│
│   ├─ Find transfer by reference               │
│   ├─ transfer.markCompleted()                 │
│   ├─ Save transfer                            │
│   └─ publishTransferEvent(TransferCompleted)  │
│                                               │
└───────────────────────────────────────────────┘
```

**Key Design Points**:

1. **Decoupled modules**: Transfer doesn't know about Account internals; Account doesn't call Transfer directly
2. **Event-driven**: Changes published as events; listeners react asynchronously
3. **Idempotency**: Multiple deliveries of same event are safe (transfer status checked before state change)
4. **Failures visible**: If account debit fails, transfer remains INITIATED; admin can investigate
5. **Audit trail**: Every state change is recorded; events logged with timestamps

#### Use Cases Implemented

| Use Case                     | Port Interface            | Status                                     |
| ---------------------------- | ------------------------- | ------------------------------------------ |
| Initiate transfer (internal) | `InitiateTransferUseCase` | ✅ Implemented (event-driven choreography) |
| Get transfer status          | `GetTransferUseCase`      | ✅ Implemented                             |
| List transfers by account    | Not scoped                | ❌ Missing endpoint                        |
| Cancel transfer (INITIATED)  | Not scoped                | ❌ Gap                                     |
| Reverse transfer (COMPLETED) | Not scoped                | ✅ Partially (via TransferReversed event)  |
| Schedule transfer (future)   | Not scoped                | ❌ Gap                                     |
| Batch transfer (one-to-many) | Not scoped                | ❌ Gap                                     |

#### Event Choreography Implementation

**Components** ([TransferService.java](transfer-module/src/main/java/com/coreledger/transfer/application/service/TransferService.java)):

| Role                 | Class                                 | Method                                            | Kafka Topic                                                      | Status |
| -------------------- | ------------------------------------- | ------------------------------------------------- | ---------------------------------------------------------------- | ------ |
| **Initiator**        | TransferService                       | `execute(command)`                                | Publishes `TransferInitiated` to `transfer-events`               | ✅     |
| **Listener**         | TransferEventHandler (account-module) | `onTransferEvent()` → `handleTransferInitiated()` | Consumes from `transfer-events`                                  | ✅     |
| **Account Updates**  | Account                               | `debitTransfer()`, `creditTransfer()`             | Publishes `MoneyWithdrawn`, `MoneyDeposited` to `account-events` | ✅     |
| **Status Updater**   | TransferService                       | `onAccountEvent()` → `handleMoneyWithdrawn()`     | Consumes from `account-events`                                   | ✅     |
| **State Transition** | TransferService                       | `onAccountEvent()` → `handleMoneyDeposited()`     | Consumes from `account-events`, publishes `TransferCompleted`    | ✅     |
| **Reversal Handler** | TransferService                       | `handleTransferReversed()`                        | Consumes from `account-events`                                   | ✅     |

**Orchestration Flow Summary**:

1. **Client initiates** via `TransferService.execute()`
2. **Transfer created** with INITIATED status
3. **Account listeners react** via Kafka → debit/credit accounts → transactions recorded
4. **Transfer status updated** back to DEBITED/COMPLETED via incoming account events
5. **TransferCompleted event** published for any subscribers

#### Persistence Layer

**JPA Entity**: `TransferJpaEntity`

- Status field: Simple UPDATE (not append-only like Account transactions)
- One row per transfer (not versioned rows)
- Table: `transfer`
- Query: Simple ID lookup returns current Transfer state

**Transfer Domain Model**:

- Immutable by design (all fields final)
- State transitions create new Transfer instances: `transfer.markCompleted()` returns new Transfer
- Domain enforces: transfer "completed from INITIATED or DEBITED" state

**Differences from Account Ledger**:

- **Account**: Every transaction is recorded as a ledger row (append-only, immutable schema)
- **Transfer**: Transfer state is overwritten (UPDATE permitted); only one row per transfer
- **Reason**: Transfer is a short-lived aggregate (INITIATED → COMPLETED in seconds); Account is long-lived (month/year of history)

**Related Files**:

- Domain: [transfer-module/src/main/java/com/coreledger/transfer/domain/model/Transfer.java](transfer-module/src/main/java/com/coreledger/transfer/domain/model/Transfer.java)
- Application: [transfer-module/src/main/java/com/coreledger/transfer/application/service/TransferService.java](transfer-module/src/main/java/com/coreledger/transfer/application/service/TransferService.java)
- Event Handler: [account-module/src/main/java/com/coreledger/account/application/service/TransferEventHandler.java](account-module/src/main/java/com/coreledger/account/application/service/TransferEventHandler.java)
- Persistence: [transfer-module/src/main/java/com/coreledger/transfer/infrastructure/persistence/](transfer-module/src/main/java/com/coreledger/transfer/infrastructure/persistence/)
- Web: [transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/](transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/)
- Kafka Config: [app/src/main/java/com/coreledger/config/KafkaConsumerConfig.java](app/src/main/java/com/coreledger/config/KafkaConsumerConfig.java)

#### Identified Gaps

- **NO endpoint to list transfers by account** — `GetTransferUseCase` implemented but not exposed via REST
- **NO inter-bank transfers** — Only internal (same bank) transfers supported
- **NO idempotency keys** — Duplicate requests create duplicate transfers; missing request deduplication
- **NO transfer fee calculation** — Fixed amounts only, no percentage-based or tiered fees
- **NO scheduled transfers** — All transfers execute immediately
- **NO batch transfers** — Cannot send one-to-many or many-to-many in single request
- **NO transfer limits per account** — Missing daily/monthly transfer caps or velocity checks
- **NO circuit breaker for account service** — If account loads fail, transfer fails hard; should have fallback
- **NO Dead Letter Queue (DLQ) setup** — Kafka failures don't get captured for retry/investigation
- **NO explicit user context in transfer** — Cannot track which real person initiated transfer (only system user ID)
- **NO transfer reconciliation** — Cannot detect stuck/orphaned transfers (INITIATED forever)
- **Transfer state not truly append-only** — Unlike Account transactions, Transfer rows are UPDATEd (acceptable for short-lived state, but loses full audit trail)

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
