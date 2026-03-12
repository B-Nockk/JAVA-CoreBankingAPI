// shared-kernel/src/main/java/com/coreledger/shared/domain/DomainEvent.java
package com.coreledger.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in CoreLedger.
 *
 * A domain event represents something that happened in the domain —
 * past tense, immutable, factual. Examples: MoneyDeposited, TransferInitiated.
 *
 * Why events matter for our architecture:
 * - They are how modules communicate without direct dependencies.
 * The account-module publishes MoneyDeposited. The notification-module
 * listens for it. Neither knows the other exists.
 * - They are the mechanism for the audit trail. Every significant
 * state change in the domain produces an event with a timestamp and ID.
 * - They are the foundation for event sourcing if we ever go that route —
 * replaying events reconstructs any past state.
 *
 * Every event carries:
 * - eventId: unique identifier for this specific occurrence (for deduplication)
 * - occurredOn: when it happened (always UTC Instant)
 * - aggregateId: which aggregate root produced this event
 *
 * Subclasses add the specific data for that event type.
 * Subclasses must be immutable.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredOn;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.aggregateId = aggregateId;
    }

    /**
     * For testing or event replay where you need a deterministic timestamp.
     */
    protected DomainEvent(String aggregateId, Instant occurredOn) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = occurredOn;
        this.aggregateId = aggregateId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * Convenience method: returns the simple class name as the event type.
     * e.g. "MoneyDeposited", "TransferInitiated"
     * Useful for logging and event routing.
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getEventType()
                + "{eventId='" + eventId
                + "', aggregateId='" + aggregateId
                + "', occurredOn=" + occurredOn + "}";
    }
}
