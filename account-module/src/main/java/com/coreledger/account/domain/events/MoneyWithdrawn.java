// account-module/src/main/java/com/coreledger/account/domain/events/MoneyWithdrawn.java
package com.coreledger.account.domain.events;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;

/**
 * Raised when a withdrawal is successfully recorded on an account.
 *
 * Potential listeners:
 * - notification-module: alert customer of debit
 * - risk-module: flag unusual withdrawal patterns
 */
public final class MoneyWithdrawn extends DomainEvent {

    private final String accountNumber;
    private final Money amount;
    private final Money balanceAfter;
    private final String reference;

    public MoneyWithdrawn(String accountId, String accountNumber,
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