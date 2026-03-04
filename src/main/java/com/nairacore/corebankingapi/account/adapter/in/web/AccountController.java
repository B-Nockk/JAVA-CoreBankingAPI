// src/main/java/com/nairacore/corebankingapi/account/adapter/in/web/AccountController.java
package com.nairacore.corebankingapi.account.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nairacore.corebankingapi.account.application.port.in.CreateAccountUseCase;
import com.nairacore.corebankingapi.account.domain.Account;
import com.nairacore.corebankingapi.account.dto.CreateAccountRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;

    // Inject the In-Port interface
    public AccountController(CreateAccountUseCase createAccountUseCase) {
        this.createAccountUseCase = createAccountUseCase;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        // The @Valid annotation automatically checks the @PositiveOrZero rule in the
        // DTO
        Account account = createAccountUseCase.createAccount(request.initialDeposit());

        // Return 201 Created status along with the account data
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }
}