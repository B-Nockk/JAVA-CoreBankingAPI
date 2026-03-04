// src/main/java/com/nairacore/corebankingapi/account/application/AccountService.java
package com.nairacore.corebankingapi.account.application;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nairacore.corebankingapi.account.application.port.in.CreateAccountUseCase;
import com.nairacore.corebankingapi.account.application.port.out.AccountPersistencePort;
import com.nairacore.corebankingapi.account.domain.Account;

@Service // Tells Spring to manage this orchestration class
public class AccountService implements CreateAccountUseCase {

    private final AccountPersistencePort persistencePort;

    // We inject the Out-Port (Interface) here, NOT the specific Database Adapter!
    public AccountService(AccountPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public Account createAccount(BigDecimal initialDeposit) {
        // 1. Generate a dummy 10-digit account number for now
        String accountNumber = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 10);

        // 2. Create the pure Domain object
        Account newAccount = new Account(accountNumber, initialDeposit);

        // 3. Save it via the Out-Port (which triggers the JPA Adapter we built earlier)
        persistencePort.save(newAccount);

        return newAccount;
    }
}