// account-module/src/main/java/com/coreledger/account/application/port/in/DepositUseCase.java
package com.coreledger.account.application.port.in;

import java.time.Instant;
import java.math.BigDecimal;

import com.coreledger.shared.domain.Money;

/**
 * Inbound port — defines the contract for depositing money into an account.
 *
 * Command carries BigDecimal (not Money) because at the web boundary we
 * don't yet know the account's currency — that lives in the domain.
 * The service loads the account, resolves the currency, constructs Money,
 * then calls the domain. Currency resolution belongs in the service layer.
 */
public interface DepositUseCase {

    DepositResult execute(Command command);

    record Command(
            String accountNumber,
            BigDecimal amount,
            String reference,
            String initiatedBy) {
    }

    record DepositResult(
            String transactionId,
            String accountNumber,
            Money amount,
            Money balanceAfter,
            Instant timestamp) {
    }
}