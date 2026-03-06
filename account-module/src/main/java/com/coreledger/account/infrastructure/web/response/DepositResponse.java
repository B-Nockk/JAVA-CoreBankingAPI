// account-module/src/main/java/com/coreledger/account/infrastructure/web/response/DepositResponse.java
package com.coreledger.account.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.coreledger.account.application.port.in.DepositUseCase;

/**
 * Outbound DTO for deposit operation result.
 */
public record DepositResponse(
        String transactionId,
        String accountNumber,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant timestamp) {

    public static DepositResponse from(DepositUseCase.DepositResult result) {
        return new DepositResponse(
                result.transactionId(),
                result.accountNumber(),
                result.amount().getAmount(),
                result.balanceAfter().getAmount(),
                result.timestamp());
    }
}