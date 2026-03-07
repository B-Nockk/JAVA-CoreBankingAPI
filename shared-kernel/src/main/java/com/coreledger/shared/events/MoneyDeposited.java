// shared-kernel/src/main/java/com/coreledger/shared/events/MoneyDeposited.java
package com.coreledger.shared.events;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;

/**
 * Raised when a deposit is successfully recorded on an account.
 *
 * Carries the amount and the resulting balance so listeners
 * can react without needing to query the account themselves.
 *
 * Potential listeners:
 * - notification-module: alert customer of credit
 * - risk-module: flag unusually large deposits
 */
public final class MoneyDeposited extends DomainEvent {

    private final String accountNumber;
    private final Money amount;
    private final Money balanceAfter;
    private final String reference;

    public MoneyDeposited(String accountId, String accountNumber,
            Money amount, Money balanceAfter, String reference) {
        super(accountId);
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reference = reference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getBalanceAfter() {
        return balanceAfter;
    }

    public String getReference() {
        return reference;
    }
}