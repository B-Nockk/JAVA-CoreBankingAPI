// ─────────────────────────────────────────────────────────────────────────────
// MoneyWithdrawn.java
// shared-kernel/src/main/java/com/coreledger/shared/events/MoneyWithdrawn.java
// ─────────────────────────────────────────────────────────────────────────────
package com.coreledger.shared.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonCreator
    private MoneyWithdrawn(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("accountNumber") String accountNumber,
            @JsonProperty("amount") Money amount,
            @JsonProperty("balanceAfter") Money balanceAfter,
            @JsonProperty("reference") String reference) {
        super(aggregateId, eventId, occurredOn);
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reference = reference;
    }

    @JsonProperty
    public String getAccountNumber() {
        return accountNumber;
    }

    @JsonProperty
    public Money getAmount() {
        return amount;
    }

    @JsonProperty
    public Money getBalanceAfter() {
        return balanceAfter;
    }

    @JsonProperty
    public String getReference() {
        return reference;
    }
}
