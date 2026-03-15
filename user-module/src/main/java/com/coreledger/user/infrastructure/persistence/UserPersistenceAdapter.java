// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/UserPersistenceAdapter.java
package com.coreledger.user.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.user.application.port.out.LoadUserPort;
import com.coreledger.user.domain.model.User;
import com.coreledger.user.domain.model.UserId;

@Component
public class UserPersistenceAdapter implements LoadUserPort {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<User> findById(UserId id) {
        UUID uuid = Objects.requireNonNull(id.getValue(), "UserId cannot be null");
        return userJpaRepository.findById(uuid).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(EmailAddress emailAddress) {
        return userJpaRepository.findByEmail(emailAddress.value()).map(this::toDomain);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable cannot be null");
        return userJpaRepository.findAll(pageable).map(this::toDomain);
    }

    private User toDomain(UserJpaEntity entity) {
        return User.reconstituteUser(
                UserId.of(entity.getId()),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getAddress(),
                entity.getUserStatus(),
                entity.getPhone(),
                EmailAddress.of(entity.getEmail()),
                entity.getDateOfBirth(),
                entity.getUserRole());
    }
}
