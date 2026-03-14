// user-module/src/main/java/com/coreledger/user/domain/model/User.java
package com.coreledger.user.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.user.domain.events.UserDeactivated;
import com.coreledger.user.domain.events.UserFlagged;
import com.coreledger.user.domain.events.UserReactivated;
import com.coreledger.user.domain.events.UserSuspended;
import com.coreledger.user.domain.exceptions.InvalidUserOperationException;

public class User {
    private final UserId id;
    private final String firstName;
    private final String lastName;
    private final String address;
    private final String phone;
    private final String email;
    private final LocalDate dateOfBirth;
    private final UserRole userRole;
    private UserStatus status;

    private User(
            UserId id,
            String firstName,
            String lastName,
            String address,
            String phone,
            String email,
            LocalDate dateOfBirth,
            UserRole userRole) {
        this.id = Objects.requireNonNull(id);
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        this.address = Objects.requireNonNull(address);
        this.phone = Objects.requireNonNull(phone);
        this.email = Objects.requireNonNull(email);
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth);
        this.userRole = Objects.requireNonNull(userRole);
        this.status = UserStatus.ACTIVE;
    }

    public static User create(
            String firstName,
            String lastName,
            String address,
            String phone,
            String email,
            LocalDate dateOfBirth,
            UserRole userRole) {
        return new User(
                UserId.generate(),
                firstName,
                lastName,
                address,
                phone,
                email,
                dateOfBirth,
                userRole);
    }

    public static User reconstituteUser(
            UserId id,
            String firstName,
            String lastName,
            String address,
            UserStatus status,
            String phone,
            String email,
            LocalDate dateOfBirth,
            UserRole userRole) {
        User user = new User(id, firstName, lastName, address, phone, email, dateOfBirth, userRole);
        user.status = status;
        return user;
    }

    //
    // Domain Behavior
    //

    public List<DomainEvent> deactivate(String reason) {
        if (this.status == UserStatus.IN_ACTIVE) {
            throw new InvalidUserOperationException("User is already inactive");
        }
        this.status = UserStatus.IN_ACTIVE;
        return List.of(new UserDeactivated(this.id, reason));
    }

    public List<DomainEvent> suspend(String reason) {
        if (this.status == UserStatus.SUSPEND) {
            throw new InvalidUserOperationException("Only active users can be suspended");
        }
        this.status = UserStatus.SUSPEND;
        return List.of(new UserSuspended(this.id, reason));
    }

    public List<DomainEvent> flag(String reason) {
        if (this.status == UserStatus.FLAGGED) {
            throw new InvalidUserOperationException("Only active users can be suspended");
        }
        this.status = UserStatus.FLAGGED;
        return List.of(new UserFlagged(this.id, reason));
    }

    public List<DomainEvent> reactivate(String reason) {
        if (this.status == UserStatus.ACTIVE) {
            throw new InvalidUserOperationException("User is already active");
        }
        this.status = UserStatus.ACTIVE;
        return List.of(new UserReactivated(this.id, reason));
    }

    // Guard used by other operations
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    // =========================================================
    // Accessors
    // =========================================================
    public UserId getId() {
        return id;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getUserName() {
        return firstName + " " + lastName;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}