// shared-kernel/src/main/java/com/coreledger/shared/kafka/EventEnvelope.java
package com.coreledger.shared.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Wraps every domain event published to Kafka.
 *
 * Wire format:
 * {
 * "eventType": "TransferInitiated",
 * "payload": { ...event fields... }
 * }
 *
 * Why an envelope:
 * - Consumer can read eventType first, then deserialize payload into the
 * correct class — no __TypeId__ headers, no type mapper, no surprises.
 * - Adding new event types never requires consumer config changes.
 * - Easy to add version, traceId, source fields here later.
 */
public record EventEnvelope(
        String eventType,
        JsonNode payload) {

    @JsonCreator
    public EventEnvelope(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("payload") JsonNode payload) {
        this.eventType = eventType;
        this.payload = payload;
    }
}