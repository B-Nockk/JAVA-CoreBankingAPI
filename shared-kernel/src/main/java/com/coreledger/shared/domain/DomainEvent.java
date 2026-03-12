// shared-kernel/src/main/java/com/coreledger/shared/domain/DomainEvent.java
package com.coreledger.shared.domain;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for all domain events.
 *
 * Jackson notes:
 * - @JsonProperty on the getters marks exactly what gets serialized.
 * - getEventType() is @JsonIgnore — it's a computed convenience method,
 * not a field. The envelope carries the type separately.
 * - Subclasses provide their own @JsonCreator constructors.
 * They call super(aggregateId, occurredOn) to restore the original
 * timestamp from the wire instead of generating a new one.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredOn;
    private final String aggregateId;

    // Called when creating a NEW event (normal domain flow)
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.aggregateId = aggregateId;
    }

    // Called when RESTORING an event from wire/storage (deserialization)
    protected DomainEvent(String aggregateId, String eventId, Instant occurredOn) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.aggregateId = aggregateId;
    }

    @JsonProperty("eventId")
    public String getEventId() {
        return eventId;
    }

    @JsonProperty("occurredOn")
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @JsonProperty("aggregateId")
    public String getAggregateId() {
        return aggregateId;
    }

    @JsonIgnore // computed — not a field, not on the wire
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