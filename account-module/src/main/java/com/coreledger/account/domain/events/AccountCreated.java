// account-module/src/main/java/com/coreledger/account/domain/events/AccountCreated.java
package com.coreledger.account.domain.events;

import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.DomainEvent;

/**
 * Raised when a new account is successfully opened.
 *
 * Carries only the data that downstream listeners need to react —
 * not the entire Account object. Events are self-contained facts.
 *
 * Potential listeners (in future modules):
 * - notification-module: send welcome message to account holder
 * - audit-module: log account opening for compliance
 */
public final class AccountCreated extends DomainEvent {

    private final String accountNumber;
    private final String ownerName;
    private final Currency currency;

    public AccountCreated(String accountId, String accountNumber,
            String ownerName, Currency currency) {
        super(accountId);
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Currency getCurrency() {
        return currency;
    }
}