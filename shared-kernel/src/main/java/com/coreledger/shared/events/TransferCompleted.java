// ─────────────────────────────────────────────────────────────────────────────
// TransferCompleted.java
// shared-kernel/src/main/java/com/coreledger/shared/events/TransferCompleted.java
// ─────────────────────────────────────────────────────────────────────────────
package com.coreledger.shared.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TransferCompleted extends DomainEvent {

    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final Money amount;

    public TransferCompleted(String transferId, String sourceAccountNumber,
            String destinationAccountNumber, Money amount) {
        super(transferId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
    }

    @JsonCreator
    private TransferCompleted(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("sourceAccountNumber") String sourceAccountNumber,
            @JsonProperty("destinationAccountNumber") String destinationAccountNumber,
            @JsonProperty("amount") Money amount) {
        super(aggregateId, eventId, occurredOn);
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
    }

    @JsonProperty
    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    @JsonProperty
    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    @JsonProperty
    public Money getAmount() {
        return amount;
    }
}
