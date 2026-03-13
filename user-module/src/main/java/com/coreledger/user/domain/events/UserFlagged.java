// user-module/src/main/java/com/coreledger/user/domain/events/UserFlagged.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.user.domain.model.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UserFlagged extends DomainEvent {
    private final String reason;

    public UserFlagged(
            UserId userId,
            String reason) {
        super(userId.toString());
        this.reason = reason;
    }

    private UserFlagged(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String reason) {
        super(aggregateId, eventId, occurredOn);
        this.reason = reason;
    }

    @JsonCreator
    static UserFlagged restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("reason") String reason) {
        return new UserFlagged(aggregateId, eventId, occurredOn, reason);
    }

    @JsonProperty
    public String getReason() {
        return reason;
    }

    public UserId getUserId() {
        return UserId.of(getAggregateId());
    }
}