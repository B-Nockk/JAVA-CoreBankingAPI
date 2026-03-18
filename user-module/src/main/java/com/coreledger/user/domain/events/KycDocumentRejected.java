// user-module/src/main/java/com/coreledger/user/domain/events/KycDocumentRejected.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.KycDocumentId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KycDocumentRejected extends DomainEvent {

    private final UserId userId;
    private final String reason;

    public KycDocumentRejected(
            KycDocumentId documentId,
            UserId userId,
            String reason) {
        super(documentId.toString());
        this.userId = userId;
        this.reason = reason;
    }

    @JsonCreator
    static KycDocumentRejected restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("userId") UserId userId,
            @JsonProperty("reason") String reason) {
        KycDocumentRejected event = new KycDocumentRejected(
                aggregateId,
                eventId,
                occurredOn,
                userId.toString(),
                reason

        );
        return event;
    }

    private KycDocumentRejected(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String userId,
            String reason) {
        super(aggregateId, eventId, occurredOn);
        this.userId = UserId.of(userId);
        this.reason = reason;
    }

    @JsonProperty
    public KycDocumentId getDocumentId() {
        return KycDocumentId.of(getAggregateId());
    }

    @JsonProperty
    public UserId getUserId() {
        return userId;
    }

    @JsonProperty
    public String getReason() {
        return reason;
    }
}
