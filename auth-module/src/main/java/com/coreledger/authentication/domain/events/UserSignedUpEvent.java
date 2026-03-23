// auth-module/src/main/java/com/coreledger/authentication/domain/events/UserSignedUpEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class UserSignedUpEvent extends DomainEvent {
    private final UserId userId;
    private final EmailAddress userEmailAddress;
    private final Instant signedUpAt;
    private final String ipAddress;
    private final String userAgent;

    public UserSignedUpEvent(UserId userId,
            EmailAddress userEmailAddress,
            Instant signedUpAt,
            String ipAddress,
            String userAgent) {
        super(userId.toString());
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.signedUpAt = signedUpAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private UserSignedUpEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            EmailAddress userEmailAddress,
            Instant signedUpAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.signedUpAt = signedUpAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static UserSignedUpEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            // @JsonProperty("userId") UserId userId,
            @JsonProperty("userEmailAddress") EmailAddress userEmailAddress,
            @JsonProperty("signedUpAt") Instant signedUpAt,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("userAgent") String userAgent) {
        UserId userId = UserId.of(aggregateId);
        return new UserSignedUpEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                userEmailAddress,
                signedUpAt,
                ipAddress,
                userAgent);
    }
}