// user-module/src/main/java/com/coreledger/user/domain/events/KycProfileDeleted.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycProfileId;
import com.coreledger.user.domain.model.KycTier;
import com.coreledger.user.domain.model.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KycProfileDeleted extends DomainEvent {

    private final UserId userId;
    private final KycTier kycTier;
    private final String reason;

    public KycProfileDeleted(
            KycProfileId id,
            UserId userId,
            KycTier kycTier,
            String reason) {
        super(id.toString());
        this.userId = userId;
        this.kycTier = kycTier;
        this.reason = reason;
    }

    @JsonCreator
    static KycProfileDeleted restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("userId") UserId userId,
            @JsonProperty("kycTier") KycTier kycTier,
            @JsonProperty("reason") String reason) {
        KycProfileDeleted event = new KycProfileDeleted(
                aggregateId,
                eventId,
                occurredOn,
                userId.toString(),
                kycTier,
                reason

        );
        return event;
    }

    private KycProfileDeleted(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String userId,
            KycTier kycTier,
            String reason) {
        super(aggregateId, eventId, occurredOn);
        this.userId = UserId.of(userId);
        this.kycTier = kycTier;
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
    public KycTier getKycTier() {
        return kycTier;
    }

    @JsonProperty
    public String getReason() {
        return reason;
    }
}
