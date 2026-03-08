// transfer-module/src/main/java/com/coreledger/transfer/infrastructure/persistence/TransferPersistenceAdapter.java
package com.coreledger.transfer.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.coreledger.shared.domain.AuditMetadata;
import com.coreledger.shared.domain.Money;
import com.coreledger.transfer.application.port.out.LoadTransferPort;
import com.coreledger.transfer.application.port.out.SaveTransferPort;
import com.coreledger.transfer.domain.model.Transfer;
import com.coreledger.transfer.domain.model.TransferId;

/**
 * Persistence adapter for the transfer bounded context.
 * Implements LoadTransferPort and SaveTransferPort.
 *
 * Transfer mapping is simpler than account mapping — no child collections,
 * no JOIN FETCH, straightforward field-to-field translation.
 *
 * Note on save() for status updates:
 * JPA's save() on an existing entity issues an UPDATE. Because only
 * status and failure_reason are marked updatable=true in the entity,
 * Hibernate will only update those columns — the financial facts
 * (amount, accounts) are physically protected at the DB column level.
 */
@Component
public class TransferPersistenceAdapter implements LoadTransferPort, SaveTransferPort {

    private final TransferJpaRepository transferJpaRepository;

    public TransferPersistenceAdapter(TransferJpaRepository transferJpaRepository) {
        this.transferJpaRepository = transferJpaRepository;
    }

    // -------------------------------------------------------------------------
    // LoadTransferPort
    // -------------------------------------------------------------------------

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return transferJpaRepository.findById(id.getValue())
                .map(this::toDomain);
    }

    // -------------------------------------------------------------------------
    // SaveTransferPort
    // -------------------------------------------------------------------------

    @Override
    public Transfer save(Transfer transfer) {
        TransferJpaEntity entity = toJpaEntity(transfer);
        TransferJpaEntity saved = transferJpaRepository.save(entity);
        return toDomain(saved);
    }

    // -------------------------------------------------------------------------
    // Mapping: JPA entity → domain
    // -------------------------------------------------------------------------

    private Transfer toDomain(TransferJpaEntity entity) {
        return Transfer.reconstitute(
                TransferId.of(entity.getId()),
                entity.getSourceAccountNumber(),
                entity.getDestinationAccountNumber(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                entity.getStatus(),
                entity.getFailureReason(),
                AuditMetadata.of(entity.getCreatedAt(), entity.getCreatedBy()));
    }

    // -------------------------------------------------------------------------
    // Mapping: domain → JPA entity
    // -------------------------------------------------------------------------

    private TransferJpaEntity toJpaEntity(Transfer transfer) {
        TransferJpaEntity entity = new TransferJpaEntity();
        entity.setId(transfer.getId().getValue());
        entity.setSourceAccountNumber(transfer.getSourceAccountNumber());
        entity.setDestinationAccountNumber(transfer.getDestinationAccountNumber());
        entity.setAmount(transfer.getAmount().getAmount());
        entity.setCurrency(transfer.getAmount().getCurrency());
        entity.setStatus(transfer.getStatus());
        entity.setFailureReason(transfer.getFailureReason());
        entity.setCreatedAt(transfer.getAudit().getCreatedAt());
        entity.setCreatedBy(transfer.getAudit().getCreatedBy());
        return entity;
    }
}