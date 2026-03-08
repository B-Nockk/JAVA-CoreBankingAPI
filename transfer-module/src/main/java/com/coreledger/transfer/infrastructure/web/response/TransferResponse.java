// transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/response/TransferResponse.java
package com.coreledger.transfer.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.coreledger.shared.domain.Currency;
import com.coreledger.transfer.application.port.in.GetTransferUseCase;
import com.coreledger.transfer.application.port.in.InitiateTransferUseCase;

/**
 * Outbound DTO for transfer data returned to clients.
 *
 * failureReason is included but will be null for non-failed transfers.
 * API clients should treat null failureReason as "no failure."
 */
public record TransferResponse(
        String transferId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        Currency currency,
        String status,
        String failureReason,
        Instant createdAt) {

    public static TransferResponse from(InitiateTransferUseCase.TransferResult result) {
        return new TransferResponse(
                result.transferId(),
                result.sourceAccountNumber(),
                result.destinationAccountNumber(),
                result.amount(),
                result.currency(),
                result.status(),
                null,
                result.createdAt());
    }

    public static TransferResponse from(GetTransferUseCase.TransferResult result) {
        return new TransferResponse(
                result.transferId(),
                result.sourceAccountNumber(),
                result.destinationAccountNumber(),
                result.amount(),
                result.currency(),
                result.status(),
                result.failureReason(),
                result.createdAt());
    }
}