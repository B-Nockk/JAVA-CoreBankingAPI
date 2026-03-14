// user-module/src/main/java/com/coreledger/user/infrastructure/persistence/KycDocumentJpaRepository.java
package com.coreledger.user.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KycDocumentJpaRepository extends JpaRepository<KycDocumentJpaEntity, UUID> {

    @Query("SELECT d FROM KycDocumentJpaEntity d WHERE d.kycProfileId = :kycProfileId")
    List<KycDocumentJpaEntity> findByKycProfileId(@Param("kycProfileId") UUID kycProfileId);

    // *NOTE:: Use default provided by JpaRepository
    // @Query("SELECT d FROM KycDocumentJpaEntity d WHERE d.id = :id")
    // Optional<KycDocumentJpaEntity> findById(@Param("id") UUID id);
}
