// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/UserJpaRepository.java
package com.coreledger.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    @Query("SELECT u FROM UserJpaEntity u WHERE u.email = :email")
    Optional<UserJpaEntity> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.phone = :phone")
    Optional<UserJpaEntity> findByPhone(@Param("phone") String phone);

    // *NOTE:: Use default provided by JpaRepository
    // @Query("SELECT u FROM UserJpaEntity u WHERE u.id = :id")
    // Optional<UserJpaEntity> findById(@Param("id") UUID id);
}
