// ─────────────────────────────────────────────────────────────────────────────
// TransferReversed.java
// shared-kernel/src/main/java/com/coreledger/shared/events/TransferReversed.java
// ─────────────────────────────────────────────────────────────────────────────
package com.coreledger.shared.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TransferReversed extends DomainEvent {

    private final String sourceAccountNumber;
    private final Money amount;

    public TransferReversed(String transferId, String sourceAccountNumber, Money amount) {
        super(transferId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
    }

    @JsonCreator
    private TransferReversed(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("sourceAccountNumber") String sourceAccountNumber,
            @JsonProperty("amount") Money amount) {
        super(aggregateId, eventId, occurredOn);
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
    }

    @JsonProperty
    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    @JsonProperty
    public Money getAmount() {
        return amount;
    }
}