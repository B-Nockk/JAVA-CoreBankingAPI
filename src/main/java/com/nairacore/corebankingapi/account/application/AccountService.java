// src/main/java/com/nairacore/corebankingapi/account/application/AccountService.java
package com.nairacore.corebankingapi.account.application;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.nairacore.corebankingapi.account.application.port.in.CreateAccountUseCase;
import com.nairacore.corebankingapi.account.application.port.out.LoadAccountPort;
import com.nairacore.corebankingapi.account.application.port.out.UpdateAccountStatePort;
import com.nairacore.corebankingapi.account.domain.Account;
import com.nairacore.corebankingapi.common.config.BankingProperties;

@Service // Tells Spring to manage this orchestration class
public class AccountService implements CreateAccountUseCase {

    private final LoadAccountPort loadAccountPort;
    private final UpdateAccountStatePort updateAccountStatePort;
    private final BankingProperties bankingProperties;

    // Constructor Injection
    public AccountService(
            LoadAccountPort loadAccountPort,
            UpdateAccountStatePort updateAccountStatePort,
            BankingProperties bankingProperties) {
        this.loadAccountPort = loadAccountPort;
        this.updateAccountStatePort = updateAccountStatePort;
        this.bankingProperties = bankingProperties;
    }

    @Override
    public Account createAccount(BigDecimal initialDeposit) {
        // 1. Generate a dummy 10-digit account number for now
        String accountNumber = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 10);

        // 2. Create the pure Domain object
        Account newAccount = new Account(accountNumber, initialDeposit);

        // 3. Save it via the Out-Port (which triggers the JPA Adapter we built earlier)
        updateAccountStatePort.save(newAccount);

        return newAccount;
    }

    public void processWithdrawal(String accountNumber, BigDecimal amount) {
        // 1. Load via the read-only port
        Account account = loadAccountPort.loadAccount(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // 2. Use strongly-typed config for business rules
        account.withdraw(amount, bankingProperties.minimumBalance());

        // 3. Save via the write-only port
        updateAccountStatePort.save(account);
    }
}