// account-module/src/main/java/com/coreledger/account/application/port/in/DepositUseCase.java
package com.coreledger.account.application.port.in;

import java.time.Instant;

import com.coreledger.shared.domain.Money;

/**
 * Inbound port — defines the contract for depositing money into an account.
 */
public interface DepositUseCase {

    DepositResult execute(Command command);

    record Command(
            String accountNumber,
            Money amount,
            String reference, // external deposit reference (e.g. bank transfer ref)
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