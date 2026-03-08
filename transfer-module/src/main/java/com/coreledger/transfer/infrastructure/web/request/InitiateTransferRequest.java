// transfer-module/src/main/java/com/coreledger/transfer/infrastructure/web/request/InitiateTransferRequest.java
package com.coreledger.transfer.infrastructure.web.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound DTO for POST /api/v1/transfers
 *
 * Currency is intentionally absent — resolved from the source account.
 * Same reasoning as DepositRequest: fewer inputs means fewer mismatch bugs.
 */
public record InitiateTransferRequest(

        @NotBlank(message = "Source account number is required") String sourceAccountNumber,

        @NotBlank(message = "Destination account number is required") String destinationAccountNumber,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount) {
}