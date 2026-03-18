// user-module/src/main/java/com/coreledger/user/domain/events/KycTierUpdated.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycProfileId;
import com.coreledger.user.domain.model.KycTier;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KycTierUpdated extends DomainEvent {

    private final UserId userId;
    private final KycTier kycTier;

    public KycTierUpdated(
            KycProfileId id,
            UserId userId,
            KycTier kycTier) {
        super(id.toString());
        this.userId = userId;
        this.kycTier = kycTier;
    }

    @JsonCreator
    static KycTierUpdated restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("userId") UserId userId,
            @JsonProperty("kycTier") KycTier kycTier) {
        KycTierUpdated event = new KycTierUpdated(
                aggregateId,
                eventId,
                occurredOn,
                userId.toString(),
                kycTier

        );
        return event;
    }

    private KycTierUpdated(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String userId,
            KycTier kycTier) {
        super(aggregateId, eventId, occurredOn);
        this.userId = UserId.of(userId);
        this.kycTier = kycTier;
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
}
