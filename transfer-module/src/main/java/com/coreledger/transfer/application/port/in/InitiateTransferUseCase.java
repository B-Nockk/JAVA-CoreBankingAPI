// transfer-module/src/main/java/com/coreledger/transfer/application/port/in/InitiateTransferUseCase.java
package com.coreledger.transfer.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

import com.coreledger.shared.domain.Currency;

/**
 * Inbound port — defines the contract for initiating a transfer.
 *
 * Same pattern as account use cases:
 * - Command carries raw input (BigDecimal not Money — currency unknown at web
 * boundary)
 * - Result carries the outcome snapshot
 * - Both are nested records for clear ownership
 */
public interface InitiateTransferUseCase {

    TransferResult execute(Command command);

    record Command(
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String initiatedBy) {
    }

    record TransferResult(
            String transferId,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            Currency currency,
            String status,
            Instant createdAt) {
    }
}