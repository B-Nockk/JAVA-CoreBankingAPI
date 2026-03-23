// auth-module/src/main/java/com/coreledger/authentication/domain/events/PasswordChangedEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class PasswordChangedEvent extends DomainEvent {
    private final UserId userId;
    private final EmailAddress userEmailAddress;
    private final Instant changedAt;
    private final String ipAddress;
    private final String userAgent;

    public PasswordChangedEvent(UserId userId,
            EmailAddress userEmailAddress,
            Instant changedAt,
            String ipAddress,
            String userAgent) {
        super(userId.toString());
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.changedAt = changedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private PasswordChangedEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            EmailAddress userEmailAddress,
            Instant changedAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.changedAt = changedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static PasswordChangedEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("userEmailAddress") EmailAddress userEmailAddress,
            @JsonProperty("changedAt") Instant changedAt,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("userAgent") String userAgent) {

        UserId userId = UserId.of(aggregateId);
        return new PasswordChangedEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                userEmailAddress,
                changedAt,
                ipAddress,
                userAgent);
    }
}
