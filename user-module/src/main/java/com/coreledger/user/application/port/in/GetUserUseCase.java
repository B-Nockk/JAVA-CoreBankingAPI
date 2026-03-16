// user-module/src/main/java/com/coreledger/user/application/port/in/GetUserUseCase.java
package com.coreledger.user.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.coreledger.user.domain.model.User;
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
     * Retrieves all users with simple pagination (page number and size only).
     * Results are sorted by ID ascending by default.
     *
     * @param page page number (0-based, must be >= 0)
     * @param size page size (must be between 1 and 100)
     * @return paginated list of users
     * @throws IllegalArgumentException if page is negative or size is invalid
     */
    UserPage getAllUsers(int page, int size);

    /**
     * Retrieves all users with full pagination and sorting capabilities.
     * This method provides more flexibility for sorting and complex queries.
     *
     * @param pageable pagination information (page, size, sort)
     * @return paginated list of users
     * @throws IllegalArgumentException if pageable contains invalid values
     */
    UserPage getAllUsers(Pageable pageable);

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

        // NEW: Static factory method to create from domain User
        public static UserDetails from(User user) {
            return new UserDetails(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getAddress(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getDateOfBirth(),
                    user.getUserRole(),
                    user.getStatus());
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

        public static UserPage from(org.springframework.data.domain.Page<UserDetails> page) {
            return new UserPage(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isFirst(),
                    page.isLast());
        }
    }
}
