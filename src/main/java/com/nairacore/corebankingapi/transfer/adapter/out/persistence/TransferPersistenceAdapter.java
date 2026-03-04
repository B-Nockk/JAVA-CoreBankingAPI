// src/main/java/com/nairacore/corebankingapi/transfer/adapter/out/persistence/TransferPersistenceAdapter.java
package com.nairacore.corebankingapi.transfer.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nairacore.corebankingapi.transfer.domain.Transfer;

@Repository
public class TransferPersistenceAdapter implements TransferRepositoryPort {

    private final TransferRepository repository;

    public TransferPersistenceAdapter(TransferRepository repository) {
        this.repository = repository;
    }

    @Override
    // REQUIRES_NEW suspends the main transaction, opens a new one, commits, and
    // resumes the main one!
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(Transfer transfer) {

        TransferJpaEntity entity = new TransferJpaEntity(
                null, // 1. DB auto-generates the Surrogate Primary Key (Long id)
                transfer.getTransferId(), // 2. The Business ID remains the same across state changes
                transfer.getSourceAccountNumber(),
                transfer.getTargetAccountNumber(),
                transfer.getAmount(),
                transfer.getStatus().name(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt(), // 3. Added the new fields
                transfer.getVersion() // 4. Added the version
        );

        repository.save(entity);
    }
}