// ─────────────────────────────────────────────────────────────────────────────
// TransferFailed.java
// shared-kernel/src/main/java/com/coreledger/shared/events/TransferFailed.java
// ─────────────────────────────────────────────────────────────────────────────
package com.coreledger.shared.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TransferFailed extends DomainEvent {

    private final String sourceAccountNumber;
    private final Money amount;
    private final String reason;

    public TransferFailed(String transferId, String sourceAccountNumber,
            Money amount, String reason) {
        super(transferId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
        this.reason = reason;
    }

    @JsonCreator
    private TransferFailed(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("sourceAccountNumber") String sourceAccountNumber,
            @JsonProperty("amount") Money amount,
            @JsonProperty("reason") String reason) {
        super(aggregateId, eventId, occurredOn);
        this.sourceAccountNumber = sourceAccountNumber;
        this.amount = amount;
        this.reason = reason;
    }

    @JsonProperty
    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    @JsonProperty
    public Money getAmount() {
        return amount;
    }

    @JsonProperty
    public String getReason() {
        return reason;
    }
}
