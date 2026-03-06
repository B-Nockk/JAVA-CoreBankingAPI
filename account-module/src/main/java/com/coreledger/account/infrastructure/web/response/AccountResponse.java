// account-module/src/main/java/com/coreledger/account/infrastructure/web/response/AccountResponse.java
package com.coreledger.account.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.coreledger.account.application.port.in.CreateAccountUseCase;
import com.coreledger.account.application.port.in.GetAccountUseCase;
import com.coreledger.shared.domain.Currency;

/**
 * Outbound DTO for account data returned to clients.
 *
 * Represents both the create response and the get response —
 * they share the same shape for this version.
 *
 * balance is exposed as BigDecimal (not Money) because the HTTP response
 * is plain JSON. The currency field alongside it gives the client
 * everything they need. Sending the Money value object would either
 * require custom serialisation or leak domain internals into the API contract.
 *
 * Static factory methods from() map from use case result records.
 * The controller calls these — it never builds the response manually.
 */
public record AccountResponse(
        String accountId,
        String accountNumber,
        String ownerName,
        Currency currency,
        BigDecimal balance,
        String status,
        Instant createdAt) {

    public static AccountResponse from(GetAccountUseCase.AccountResult result) {
        return new AccountResponse(
                result.accountId(),
                result.accountNumber(),
                result.ownerName(),
                result.currency(),
                result.balance().getAmount(),
                result.status(),
                result.createdAt());
    }

    public static AccountResponse from(CreateAccountUseCase.AccountCreatedResult result) {
        return new AccountResponse(
                result.accountId(),
                result.accountNumber(),
                result.ownerName(),
                result.currency(),
                BigDecimal.ZERO,
                result.status(),
                Instant.now());
    }
}