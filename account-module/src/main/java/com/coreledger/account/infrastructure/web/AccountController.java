// account-module/src/main/java/com/coreledger/account/infrastructure/web/AccountController.java
package com.coreledger.account.infrastructure.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coreledger.account.application.port.in.CreateAccountUseCase;
import com.coreledger.account.application.port.in.DepositUseCase;
import com.coreledger.account.application.port.in.GetAccountUseCase;
import com.coreledger.account.infrastructure.web.request.CreateAccountRequest;
import com.coreledger.account.infrastructure.web.request.DepositRequest;
import com.coreledger.account.infrastructure.web.response.AccountResponse;
import com.coreledger.account.infrastructure.web.response.DepositResponse;

import jakarta.validation.Valid;

/**
 * REST adapter — inbound web layer for the account bounded context.
 *
 * Responsibilities:
 * - Accept HTTP requests and validate input (@Valid)
 * - Map request DTOs → use case Commands
 * - Call the appropriate inbound port (never the service directly)
 * - Map use case Results → response DTOs
 * - Return appropriate HTTP status codes
 *
 * What this controller must NOT do:
 * - Contain any business logic
 * - Know about domain objects (Account, Transaction)
 * - Call repositories or persistence directly
 * - Handle transactions — that is the service's job
 *
 * The controller depends on use case interfaces, not AccountService.
 * This means in tests you can inject a mock use case without spinning
 * up the entire application context.
 *
 * "SYSTEM" as initiatedBy is a placeholder — in a real system this would
 * come from the authenticated user's JWT claims (Spring Security principal).
 * We'll replace this when security is added.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final DepositUseCase depositUseCase;

    public AccountController(
            CreateAccountUseCase createAccountUseCase,
            GetAccountUseCase getAccountUseCase,
            DepositUseCase depositUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.depositUseCase = depositUseCase;
    }

    // POST /api/v1/accounts
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        CreateAccountUseCase.Command command = new CreateAccountUseCase.Command(
                request.ownerName(),
                request.currency(),
                "SYSTEM");

        CreateAccountUseCase.AccountCreatedResult result = createAccountUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(result));
    }

    // GET /api/v1/accounts/{accountNumber}
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountNumber) {
        GetAccountUseCase.AccountResult result = getAccountUseCase.getByAccountNumber(accountNumber);
        return ResponseEntity.ok(AccountResponse.from(result));
    }

    // GET /api/v1/accounts
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = getAccountUseCase.getAllAccounts()
                .stream()
                .map(AccountResponse::from)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    // POST /api/v1/accounts/{accountNumber}/deposit
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {
        DepositUseCase.Command command = new DepositUseCase.Command(
                accountNumber,
                request.amount(),
                request.reference(),
                "SYSTEM");

        DepositUseCase.DepositResult result = depositUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(DepositResponse.from(result));
    }
}