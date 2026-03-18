// user-module/src/main/java/com/coreledger/user/domain/events/KycDocumentVerified.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.KycDocumentId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KycDocumentVerified extends DomainEvent {
    // private final KycDocumentId documentId;
    private final UserId userId;

    public KycDocumentVerified(
            KycDocumentId documentId,
            UserId userId) {
        super(documentId.toString());
        this.userId = userId;
    }

    @JsonCreator
    static KycDocumentVerified restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("userId") UserId userId) {
        KycDocumentVerified event = new KycDocumentVerified(
                aggregateId,
                eventId,
                occurredOn,
                userId.toString()

        );
        return event;
    }

    private KycDocumentVerified(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String userId) {
        super(aggregateId, eventId, occurredOn);
        this.userId = UserId.of(userId);
    }

    @JsonProperty
    public KycDocumentId getDocumentId() {
        return KycDocumentId.of(getAggregateId());
    }

    @JsonProperty
    public UserId getUserId() {
        return userId;
    }
}
