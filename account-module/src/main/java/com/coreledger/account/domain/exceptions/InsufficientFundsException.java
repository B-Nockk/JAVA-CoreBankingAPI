// account-module/src/main/java/com/coreledger/account/domain/exceptions/InsufficientFundsException.java
package com.coreledger.account.domain.exceptions;

import com.coreledger.shared.domain.Money;

/**
 * Thrown when a debit operation is attempted but the account balance
 * is insufficient to cover the requested amount.
 *
 * Carries both the available balance and the requested amount so that
 * the caller (or the global exception handler) can produce a meaningful
 * error response without needing to re-query anything.
 *
 * Note: we deliberately do NOT expose the balance in the HTTP response
 * (see GlobalExceptionHandler) — revealing exact balances in error messages
 * is a security concern. The fields here are for internal logging only.
 */
public class InsufficientFundsException extends RuntimeException {

    private final Money availableBalance;
    private final Money requestedAmount;

    public InsufficientFundsException(Money availableBalance, Money requestedAmount) {
        super(String.format("Insufficient funds: available %s, requested %s",
                availableBalance, requestedAmount));
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public Money getAvailableBalance() {
        return availableBalance;
    }

    public Money getRequestedAmount() {
        return requestedAmount;
    }
}