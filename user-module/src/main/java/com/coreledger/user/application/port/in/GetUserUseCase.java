// user-module/src/main/java/com/coreledger/user/application/port/in/GetUserUseCase.java
package com.coreledger.user.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.coreledger.user.domain.model.UserId;
import com.coreledger.user.domain.model.UserRole;
import com.coreledger.user.domain.model.UserStatus;
import com.coreledger.shared.domain.EmailAddress;

public interface GetUserUseCase {

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param userId the user's ID (never null)
     * @return Optional containing the user if found, empty otherwise
     * @throws IllegalArgumentException if userId is null
     */
    Optional<UserDetails> getUserById(UserId userId);

    /**
     * Retrieves a user by their email address.
     * Email matching is case-insensitive.
     *
     * @param email the user's email address (never null)
     * @return Optional containing the user if found, empty otherwise
     * @throws IllegalArgumentException if email is null or invalid
     */
    Optional<UserDetails> getUserByEmail(EmailAddress email);

    /**
     * Retrieves all users in the system with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return paginated list of users (never null)
     */
    UserPage getAllUsers(int page, int size);

    /**
     * User data returned by the use case.
     * This is a read-only view of user information.
     */
    record UserDetails(
            UserId id,
            String firstName,
            String lastName,
            String address,
            String phone,
            EmailAddress email,
            LocalDate dateOfBirth,
            UserRole userRole,
            UserStatus status) {

        public UserDetails {
            // Only validate fields that AREN'T value objects
            if (id == null)
                throw new IllegalArgumentException("id cannot be null");
            if (firstName == null || firstName.isBlank())
                throw new IllegalArgumentException("firstName cannot be blank");
            if (lastName == null || lastName.isBlank())
                throw new IllegalArgumentException("lastName cannot be blank");
            // email validation is handled by EmailAddress class itself!
        }

        public String getFullName() {
            return firstName + " " + lastName;
        }

        // Helper methods using EmailAddress
        public String getEmailDomain() {
            return email.getDomain();
        }

        public String getDisplayName() {
            return email.getLocalPart();
        }
    }

    /**
     * Paginated result for user listings.
     */
    record UserPage(
            List<UserDetails> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {

        public static UserPage empty(int page, int size) {
            return new UserPage(List.of(), page, size, 0, 0, true, true);
        }
    }
}