// ─────────────────────────────────────────────────────────────────────────────
// AccountCreated.java
// account-module/src/main/java/com/coreledger/account/domain/events/AccountCreated.java
// ─────────────────────────────────────────────────────────────────────────────
package com.coreledger.account.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AccountCreated extends DomainEvent {

    private final String accountNumber;
    private final String ownerName;
    private final Currency currency;

    // Normal construction
    public AccountCreated(String accountId, String accountNumber,
            String ownerName, Currency currency) {
        super(accountId);
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
    }

    // Deserialization — restores original eventId and occurredOn from wire
    @JsonCreator
    static AccountCreated restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("accountNumber") String accountNumber,
            @JsonProperty("ownerName") String ownerName,
            @JsonProperty("currency") Currency currency) {
        return new AccountCreated(aggregateId, eventId, occurredOn, accountNumber, ownerName, currency);
    }

    // Private restore constructor
    private AccountCreated(String aggregateId, String eventId, Instant occurredOn,
            String accountNumber, String ownerName, Currency currency) {
        super(aggregateId, eventId, occurredOn);
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
    }

    @JsonProperty
    public String getAccountNumber() {
        return accountNumber;
    }

    @JsonProperty
    public String getOwnerName() {
        return ownerName;
    }

    @JsonProperty
    public Currency getCurrency() {
        return currency;
    }
}
