// auth-module/src/main/java/com/coreledger/authentication/domain/events/MfaChallengedEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class MfaChallengedEvent extends DomainEvent {
    private final UserId userId;
    private final String challengeId;
    private final Instant challengedAt;
    private final String ipAddress;
    private final String userAgent;

    public MfaChallengedEvent(UserId userId,
            String challengeId,
            Instant challengedAt,
            String ipAddress,
            String userAgent) {
        super(userId.toString());
        this.userId = userId;
        this.challengeId = challengeId;
        this.challengedAt = challengedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private MfaChallengedEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            String challengeId,
            Instant challengedAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userId = userId;
        this.challengeId = challengeId;
        this.challengedAt = challengedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static MfaChallengedEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            // @JsonProperty("userId;") UserId userId,
            @JsonProperty("challengeId;") String challengeId,
            @JsonProperty("challengedAt;") Instant challengedAt,
            @JsonProperty("ipAddress;") String ipAddress,
            @JsonProperty("userAgent;") String userAgent

    ) {
        UserId userId = UserId.of(aggregateId);
        MfaChallengedEvent event = new MfaChallengedEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                challengeId,
                challengedAt,
                ipAddress,
                userAgent);
        return event;
    }
}
