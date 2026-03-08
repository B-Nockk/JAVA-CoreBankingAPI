// app/src/main/java/com/coreledger/app/adapter/AccountVerificationAdapter.java
package com.coreledger.app.adapter;

import org.springframework.stereotype.Component;

import com.coreledger.account.application.port.in.GetAccountUseCase;
import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.transfer.application.port.out.AccountVerificationPort;
import com.coreledger.transfer.domain.exceptions.InvalidTransferException;

/**
 * Cross-module adapter — implements AccountVerificationPort (defined by
 * transfer-module)
 * by delegating to GetAccountUseCase (defined by account-module).
 *
 * This class is the ONLY place in the codebase where transfer-module and
 * account-module are deliberately coupled. It lives in app/ — the wiring layer
 * —
 * precisely because app/ is allowed to know about all modules.
 *
 * Microservice extraction path:
 * When transfer-module becomes its own service, delete this class and replace
 * it
 * with an HTTP client implementation of AccountVerificationPort that calls
 * the account service over the network. Transfer-module itself doesn't change.
 *
 * Why InvalidTransferException and not AccountNotFoundException?
 * AccountNotFoundException is account-module's domain exception —
 * transfer-module
 * has no dependency on account-module and cannot use it. This adapter
 * translates
 * the account-module exception into the transfer-module's own exception
 * vocabulary.
 * This is the anti-corruption layer in action.
 */
@Component
public class AccountVerificationAdapter implements AccountVerificationPort {

    private final GetAccountUseCase getAccountUseCase;

    public AccountVerificationAdapter(GetAccountUseCase getAccountUseCase) {
        this.getAccountUseCase = getAccountUseCase;
    }

    @Override
    public AccountView findActiveAccount(String accountNumber) {
        try {
            GetAccountUseCase.AccountResult result = getAccountUseCase.getByAccountNumber(accountNumber);

            if (!"ACTIVE".equals(result.status())) {
                throw new InvalidTransferException(
                        "Account " + accountNumber + " is not active: " + result.status());
            }

            return new AccountView(
                    result.accountNumber(),
                    result.currency(),
                    true);

        } catch (AccountNotFoundException e) {
            // Translate account-module exception → transfer-module exception
            throw new InvalidTransferException(
                    "Account not found: " + accountNumber);
        }
    }
}