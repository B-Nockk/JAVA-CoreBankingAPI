// auth-module/src/main/java/com/coreledger/authentication/domain/events/MfaSucceededEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class MfaSucceededEvent extends DomainEvent {
    private final UserId userId;
    private final String challengeId;
    private final Instant succeededAt;
    private final String ipAddress;
    private final String userAgent;

    public MfaSucceededEvent(UserId userId,
            String challengeId,
            Instant succeededAt,
            String ipAddress,
            String userAgent) {
        super(userId.toString());
        this.userId = userId;
        this.challengeId = challengeId;
        this.succeededAt = succeededAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private MfaSucceededEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            String challengeId,
            Instant succeededAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userId = userId;
        this.challengeId = challengeId;
        this.succeededAt = succeededAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static MfaSucceededEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            // @JsonProperty("userId") UserId userId,
            @JsonProperty("challengeId") String challengeId,
            @JsonProperty("succeededAt") Instant succeededAt,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("userAgent") String userAgent) {
        UserId userId = UserId.of(aggregateId);
        return new MfaSucceededEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                challengeId,
                succeededAt,
                ipAddress,
                userAgent);
    }
}
