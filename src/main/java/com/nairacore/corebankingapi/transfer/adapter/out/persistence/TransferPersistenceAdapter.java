// src/main/java/com/nairacore/corebankingapi/transfer/adapter/out/persistence/TransferPersistenceAdapter.java
package com.nairacore.corebankingapi.transfer.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.nairacore.corebankingapi.transfer.application.port.out.SaveTransferPort;
import com.nairacore.corebankingapi.transfer.domain.Transfer;

@Repository
public class TransferPersistenceAdapter implements SaveTransferPort {

    private final TransferRepository repository;

    public TransferPersistenceAdapter(TransferRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Transfer transfer) {
        TransferJpaEntity entity = new TransferJpaEntity(
                transfer.getTransferId(),
                transfer.getSourceAccountNumber(),
                transfer.getTargetAccountNumber(),
                transfer.getAmount(),
                transfer.getStatus().name(),
                transfer.getCreatedAt());
        repository.save(entity);
    }
}