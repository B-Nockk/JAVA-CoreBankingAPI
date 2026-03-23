// auth-module/src/main/java/com/coreledger/authentication/domain/events/MfaFailedEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class MfaFailedEvent extends DomainEvent {
    private final UserId userId;
    private final String challengeId;
    private final Instant failedAt;
    private final String ipAddress;
    private final String userAgent;

    public MfaFailedEvent(UserId userId,
            String challengeId,
            Instant failedAt,
            String ipAddress,
            String userAgent) {
        super(userId.toString());
        this.userId = userId;
        this.challengeId = challengeId;
        this.failedAt = failedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private MfaFailedEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            String challengeId,
            Instant failedAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userId = userId;
        this.challengeId = challengeId;
        this.failedAt = failedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static MfaFailedEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            // @JsonProperty("userId") UserId userId,
            @JsonProperty("challengeId") String challengeId,
            @JsonProperty("failedAt") Instant failedAt,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("userAgent") String userAgent) {
        UserId userId = UserId.of(aggregateId);
        return new MfaFailedEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                challengeId,
                failedAt,
                ipAddress,
                userAgent);
    }
}
