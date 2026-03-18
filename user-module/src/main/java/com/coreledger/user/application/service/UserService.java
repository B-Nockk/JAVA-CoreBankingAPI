// user-module/src/main/java/com/coreledger/user/application/service/UserService.java
package com.coreledger.user.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coreledger.shared.DomainEventPublisher;
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.shared.domain.UserId;
import com.coreledger.user.application.port.in.CreateUserUseCase;
import com.coreledger.user.application.port.in.DeactivateUserUseCase;
import com.coreledger.user.application.port.in.FlagUserUseCase;
import com.coreledger.user.application.port.in.GetUserUseCase;
import com.coreledger.user.application.port.in.ReactivateUserUseCase;
import com.coreledger.user.application.port.in.SuspendUserUseCase;
import com.coreledger.user.application.port.out.LoadUserPort;
import com.coreledger.user.application.port.out.SaveUserPort;
import com.coreledger.user.domain.exceptions.UserNotFoundException;
import com.coreledger.user.domain.model.User;

/**
 * Application service for user management within the user bounded context
 *
 * Responsibilities:
 * - Implement User management inbound ports (use cases):
 * - Orchestrate the domain: load -> user -> save -> publish event
 * - Owns user management boundaries
 * - Maps domain objects to result records for callers
 *
 * The pattern here is always the same four steps:
 * 1. Load the aggregate via outbound port
 * 2. Call the behavior on the aggregate (domain does the work)
 * 3. Persist via the outbound port
 * 4. Publish domain event *
 */
@Service
public class UserService
        implements CreateUserUseCase, GetUserUseCase, FlagUserUseCase, SuspendUserUseCase, DeactivateUserUseCase,
        ReactivateUserUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id");

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final DomainEventPublisher eventPublisher;

    public UserService(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            DomainEventPublisher eventPublisher) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    // CreateUserUseCase
    // ============================================================

    @Override
    public UserCreatedResult execute(CreateUserUseCase.Command command) {
        User user = User.create(
                command.firstName(),
                command.lastName(),
                command.address(),
                command.phone(),
                EmailAddress.of(command.email()),
                command.dateOfBirth(),
                command.userRole());

        User saved = saveUserPort.save(user);
        // TODO: eventPublisher.publish(new UserCreatedEvent(saved));

        return new UserCreatedResult(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getAddress(),
                saved.getPhone(),
                saved.getEmail().toString(),
                saved.getDateOfBirth(),
                saved.getUserRole());
    }

    // ============================================================
    // GetUserUseCase
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetails> getUserById(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        return loadUserPort.findById(userId)
                .map(UserDetails::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetails> getUserByEmail(EmailAddress email) {
        if (email == null) {
            throw new IllegalArgumentException("email cannot be null");
        }
        return loadUserPort.findByEmail(email)
                .map(UserDetails::from);
    }

    // ============================================================
    // GetUserUseCase - Pagination (int, int)
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public UserPage getAllUsers(int page, int size) {
        // Guard clauses for input validation
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be >= 0, but was: " + page);
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Page size must be between 1 and %d, but was: %d", MAX_PAGE_SIZE, size));
        }

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        return getAllUsers(pageable);
    }

    // ============================================================
    // GetUserUseCase - Pagination (Pageable)
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public UserPage getAllUsers(Pageable pageable) {
        // Guard clauses for input validation
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }

        // Validate and sanitize the pageable
        Pageable validatedPageable = validateAndSanitizePageable(pageable);
        Page<User> userPage = loadUserPort.findAll(validatedPageable);

        // Convert domain Users to UserDetails
        Page<UserDetails> userDetailsPage = userPage.map(UserDetails::from);

        return UserPage.from(userDetailsPage);
    }

    /**
     * Validates and sanitizes the Pageable object.
     * Ensures page size doesn't exceed maximum and applies default sort if none
     * provided.
     */
    private Pageable validateAndSanitizePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException(
                    "Page number must be >= 0, but was: " + pageable.getPageNumber());
        }

        int pageSize = pageable.getPageSize();
        if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Page size must be between 1 and %d, but was: %d",
                            MAX_PAGE_SIZE, pageSize));
        }

        // If no sort is specified, apply default sort
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    DEFAULT_SORT);
        }

        return pageable;
    }

    // ============================================================
    // FlagUserUseCase
    // ============================================================

    @Override
    @Transactional
    public void flagUser(UserId userId, String reason) {
        validateUserIdAndReason(userId, reason);

        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        var events = user.flag(reason);
        saveUserPort.save(user);

        // Step 4: Publish domain events
        publishUserEvents(events); // Even cleaner!
    }

    // ============================================================
    // SuspendUserUseCase
    // ============================================================

    @Override
    @Transactional
    public void suspendUser(UserId userId, String reason) {
        validateUserIdAndReason(userId, reason);

        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        var events = user.suspend(reason);
        saveUserPort.save(user);
        publishUserEvents(events);
    }

    // ============================================================
    // DeactivateUserUseCase
    // ============================================================

    @Override
    @Transactional
    public void deactivateUser(UserId userId, String reason) {
        validateUserIdAndReason(userId, reason);

        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        var events = user.deactivate(reason);
        saveUserPort.save(user);
        publishUserEvents(events);
    }

    // ============================================================
    // ReactivateUserUseCase
    // ============================================================

    @Override
    @Transactional
    public void reactivateUser(UserId userId, String reason) {
        validateUserIdAndReason(userId, reason);

        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        var events = user.reactivate(reason);
        saveUserPort.save(user);
        publishUserEvents(events);
    }

    /**
     * Shared guard clause for validating userId and reason.
     * Keeps event methods DRY while preserving existing semantics.
     */
    private void validateUserIdAndReason(UserId userId, String reason) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason cannot be null or blank");
        }
    }

    private void publishUserEvents(List<DomainEvent> events) {
        eventPublisher.publishAll(events, eventPublisher::publishUserEvent);
        // events.forEach(eventPublisher::publishUserEvent);
    }
}