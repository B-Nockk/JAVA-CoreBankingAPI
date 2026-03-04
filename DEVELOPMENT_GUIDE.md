# DEVELOPMENT GUIDE — JAVA-CoreBankingAPI (Release 1)

## Project Structure (follow exactly)

```tree
src/main/java/com/nairacore/corebankingapi/
├── CoreBankingApiApplication.java          # Spring Boot entry point
├── common/                                 # cross-cutting concerns
│   ├── config/
│   ├── exception/
│   └── logging/
├── account/                                # Bounded Context 1 – Account Management
│   ├── domain/                             # Pure business domain (entities, value objects, rules)
│   ├── application/                        # Use cases / orchestration layer
│   │   └── port/                           # Input & output ports (interfaces)
│   │       ├── in/                         # Driving / input ports (use-case interfaces)
│   │       └── out/                        # Driven / output ports (repository ports, etc.)
│   ├── adapter/                            # Technology-specific adapters
│   │   ├── in/
│   │   │   └── web/                        # REST controllers (inbound)
│   │   └── out/
│   │       └── persistence/                # JPA / DB adapters (outbound)
│   └── dto/                                # API request/response shapes
└── transfer/                               # Bounded Context 2 – Transfers & Payments
    ├── domain/
    ├── application/
    │   └── port/
    │       ├── in/
    │       └── out/
    ├── adapter/
    │   ├── in/
    │   │   └── web/
    │   └── out/
    │       └── persistence/
    └── dto/
    # ... future: payment/, notification/, risk/, etc.
```

## User Flows

1. Create account → get account number + balance
2. View account details
3. Transfer money between accounts (internal only)

## Full Endpoint List (v1)

- POST /api/v1/accounts → create account
- GET /api/v1/accounts/{accountNumber} → get one account
- GET /api/v1/accounts → list all (admin)
- POST /api/v1/transfers → make internal transfer

## Implementation Order (your preferred flow + testing)

For every feature (Account → Transfer):

1. Config & application.yml
2. Logging setup
3. Entry point (already generated)
4. Model (entity + DTOs)
5. Repository (interface)
6. Service (interface + impl)
7. Controller (handler)
8. Exception handling
9. Tests (unit + integration)
