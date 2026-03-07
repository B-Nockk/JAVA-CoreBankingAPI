// transfer-module/src/main/java/com/coreledger/transfer/application/port/in/GetTransferUseCase.java
package com.coreledger.transfer.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

import com.coreledger.shared.domain.Currency;

/**
 * Inbound port — defines the contract for querying transfer state.
 */
public interface GetTransferUseCase {

    TransferResult getById(String transferId);

    record TransferResult(
            String transferId,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            Currency currency,
            String status,
            String failureReason,
            Instant createdAt) {
    }
}