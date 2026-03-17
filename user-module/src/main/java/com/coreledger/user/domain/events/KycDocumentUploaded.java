// user-module/src/main/java/com/coreledger/user/domain/events/KycDocumentUploaded.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycDocumentType;
import com.coreledger.user.domain.model.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KycDocumentUploaded extends DomainEvent {

    private final UserId userId;
    private final KycDocumentType kycDocumentType;

    public KycDocumentUploaded(
            KycDocumentId documentId,
            UserId userId,
            KycDocumentType kycDocumentType) {
        super(documentId.toString());
        this.userId = userId;
        this.kycDocumentType = kycDocumentType;
    }

    @JsonCreator
    static KycDocumentUploaded restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("userId") UserId userId,
            @JsonProperty("reason") KycDocumentType kycDocumentType) {
        KycDocumentUploaded event = new KycDocumentUploaded(
                aggregateId,
                eventId,
                occurredOn,
                userId.toString(),
                kycDocumentType

        );
        return event;
    }

    private KycDocumentUploaded(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String userId,
            KycDocumentType kycDocumentType) {
        super(aggregateId, eventId, occurredOn);
        this.userId = UserId.of(userId);
        this.kycDocumentType = kycDocumentType;
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
    public KycDocumentType getKycDocumentType() {
        return kycDocumentType;
    }
}
