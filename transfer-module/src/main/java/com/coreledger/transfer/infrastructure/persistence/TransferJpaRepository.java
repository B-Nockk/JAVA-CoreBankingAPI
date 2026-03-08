// transfer-module/src/main/java/com/coreledger/transfer/infrastructure/persistence/TransferJpaRepository.java
package com.coreledger.transfer.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coreledger.transfer.domain.model.TransferStatus;

/**
 * Spring Data JPA repository for TransferJpaEntity.
 *
 * No JOIN FETCH needed here — transfers have no child collections.
 * Simple findById from JpaRepository is sufficient.
 */
public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    List<TransferJpaEntity> findBySourceAccountNumber(String accountNumber);

    List<TransferJpaEntity> findByStatus(TransferStatus status);
}