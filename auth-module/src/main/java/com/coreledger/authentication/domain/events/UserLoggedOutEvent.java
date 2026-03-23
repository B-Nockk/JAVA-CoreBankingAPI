// auth-module/src/main/java/com/coreledger/authentication/domain/events/UserLoggedOutEvent.java
package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class UserLoggedOutEvent extends DomainEvent {
    private final UserId userId;
    private final EmailAddress userEmailAddress;
    private final Instant loggedOutAt;
    private final String ipAddress;
    private final String userAgent;

    public UserLoggedOutEvent(
            UserId userId,
            EmailAddress userEmailAddress,
            Instant loggedOutAt,
            String ipAddress,
            String userAgent) {
        super(userId.toString());
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.loggedOutAt = loggedOutAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    private UserLoggedOutEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            EmailAddress userEmailAddress,
            Instant loggedOutAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.loggedOutAt = loggedOutAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static UserLoggedOutEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            // @JsonProperty("userId") UserId userId,
            @JsonProperty("userEmailAddress") EmailAddress userEmailAddress,
            @JsonProperty("loggedOutAt") Instant loggedOutAt,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("userAgent") String userAgent) {
        UserId userId = UserId.of(aggregateId);
        return new UserLoggedOutEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                userEmailAddress,
                loggedOutAt,
                ipAddress,
                userAgent);
    }
}
