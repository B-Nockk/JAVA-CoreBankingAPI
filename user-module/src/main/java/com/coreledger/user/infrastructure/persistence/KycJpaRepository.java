// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/KycJpaRepository.java
package com.coreledger.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KycJpaRepository extends JpaRepository<KycJpaEntity, UUID> {

    @Query("SELECT k FROM KycJpaEntity k WHERE k.userId = :userId")
    Optional<KycJpaEntity> findByUserId(@Param("userId") UUID userId);

    // *NOTE:: Use default provided by JpaRepository
    // @Query("SELECT k FROM KycJpaEntity k WHERE k.id = :id")
    // Optional<KycJpaEntity> findById(@Param("id") UUID id);
}
