package com.coreledger.authentication.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class UserAuthenticatedEvent extends DomainEvent {
    private final UserId userId;
    private final EmailAddress userEmailAddress;
    private final Instant issuedAt;
    private final String ipAddress;
    private final String userAgent;

    public UserAuthenticatedEvent(
            UserId userId,
            EmailAddress userEmailAddress,
            Instant issuedAt,
            String ipAddress,
            String userAgent

    ) {
        super(userId.toString());
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.issuedAt = issuedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;

    }

    private UserAuthenticatedEvent(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            UserId userId,
            EmailAddress userEmailAddress,
            Instant issuedAt,
            String ipAddress,
            String userAgent) {
        super(aggregateId, eventId, occurredOn);
        this.userId = userId;
        this.userEmailAddress = userEmailAddress;
        this.issuedAt = issuedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @JsonCreator
    static UserAuthenticatedEvent restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("userEmailAddress;") EmailAddress userEmailAddress,
            @JsonProperty("issuedAt;") Instant issuedAt,
            @JsonProperty("ipAddress;") String ipAddress,
            @JsonProperty("userAgent;") String userAgent

    ) {
        UserId userId = UserId.of(aggregateId);
        UserAuthenticatedEvent event = new UserAuthenticatedEvent(
                aggregateId,
                eventId,
                occurredOn,
                userId,
                userEmailAddress,
                issuedAt,
                ipAddress,
                userAgent);

        return event;
    }

    // @JsonProperty
    // public UserId getUserId() {
    // return UserId.of(getAggregateId());
    // }

}
