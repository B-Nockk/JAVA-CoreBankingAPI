# DEVELOPMENT GUIDE — JAVA-CoreBankingAPI (Release 1)

## Project Structure (follow exactly)

```mermaid

src/main/java/com/nairacore/corebankingapi/
├── CoreBankingApiApplication.java # entry point
├── common/ # shared across all features
│ ├── config/
│ ├── exception/
│ └── logging/
├── account/ # Bounded Context 1 (easy to extract later)
│ ├── controller/
│ ├── service/
│ ├── repository/
│ ├── model/
│ └── dto/
├── transfer/ # Bounded Context 2 (will add next)
│ ├── controller/
│ ├── service/
│ ├── repository/
│ ├── model/
│ └── dto/
└── ... (future: payment, notification, etc.)
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
