// user-module/src/main/java/com/coreledger/user/domain/events/UserCreated.java
package com.coreledger.user.domain.events;

import java.time.Instant;

import com.coreledger.shared.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UserCreated extends DomainEvent {
    private final String firstName;
    private final String lastName;
    private final String address;

    public UserCreated(
            String userId,
            String firstName,
            String lastName,
            String address) {
        super(userId);
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    @JsonCreator
    static UserCreated restore(
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("address") String address

    ) {
        return new UserCreated(
                aggregateId,
                eventId,
                occurredOn,
                firstName,
                lastName,
                address);
    }

    private UserCreated(
            String aggregateId,
            String eventId,
            Instant occurredOn,
            String firstName,
            String lastName,
            String address) {
        super(aggregateId, eventId, occurredOn);
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    @JsonProperty
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty
    public String getLastName() {
        return lastName;
    }

    @JsonProperty
    public String getAddressString() {
        return address;
    }
}
