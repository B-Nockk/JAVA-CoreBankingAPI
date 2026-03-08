// transfer-module/src/main/java/com/coreledger/transfer/infrastructure/persistence/TransferJpaEntity.java
package com.coreledger.transfer.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.coreledger.shared.domain.Currency;
import com.coreledger.transfer.domain.model.TransferStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for the transfers table.
 *
 * Unlike the transaction ledger (append-only), transfer records ARE updated
 * as the transfer moves through its lifecycle. This is correct because:
 * - A transfer is an orchestration record, not a financial ledger entry
 * - The financial truth lives in the account transaction ledger
 * - The transfer record tracks the saga state — what happened to the
 * orchestration
 *
 * Only status and failure_reason are updatable — the financial facts
 * (amount, source, destination) are immutable once initiated.
 */
@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfers_source_account", columnList = "source_account_number"),
        @Index(name = "idx_transfers_destination_account", columnList = "destination_account_number"),
        @Index(name = "idx_transfers_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class TransferJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "source_account_number", updatable = false, nullable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", updatable = false, nullable = false, length = 10)
    private String destinationAccountNumber;

    @Column(name = "amount", updatable = false, nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", updatable = false, nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    // Nullable — only populated on FAILED or REVERSED
    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false, nullable = false)
    private String createdBy;
}