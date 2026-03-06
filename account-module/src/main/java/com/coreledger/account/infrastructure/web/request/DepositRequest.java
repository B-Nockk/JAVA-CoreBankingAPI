// account-module/src/main/java/com/coreledger/account/infrastructure/web/request/DepositRequest.java
package com.coreledger.account.infrastructure.web.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound DTO for POST /api/v1/accounts/{accountNumber}/deposit
 *
 * amount arrives as BigDecimal from JSON — Jackson deserialises
 * numeric values to BigDecimal when the field type is BigDecimal.
 * Never accept amount as double or float in a financial API.
 *
 * currency is intentionally NOT in this request — the account already
 * knows its currency. Accepting currency here would open a vector for
 * currency mismatch bugs. The service derives currency from the account.
 */
public record DepositRequest(

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,

        @NotBlank(message = "Reference is required") String reference) {
}