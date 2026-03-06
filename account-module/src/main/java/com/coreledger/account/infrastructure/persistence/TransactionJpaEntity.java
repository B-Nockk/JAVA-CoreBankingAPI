// account-module/src/main/java/com/coreledger/account/infrastructure/persistence/TransactionJpaEntity.java
package com.coreledger.account.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.coreledger.account.domain.model.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for the transactions table.
 *
 * This is the append-only ledger. Rows are only ever INSERTed — never
 * UPDATEd or DELETEd. The schema enforces this via no update columns
 * (updatable = false on all business fields).
 *
 * amount and balance_after are stored as BigDecimal → DECIMAL(19,4) in the DB.
 * - DECIMAL(19,4): up to 15 digits before decimal, 4 after.
 * - We use scale 2 in the domain (Money) but store at scale 4 in the DB
 * to accommodate future currencies with higher precision (e.g. crypto).
 * - currency is stored per-transaction (denormalised from the account)
 * so that each row is a self-contained, auditable record.
 *
 * The account relationship is ManyToOne — many transactions belong to one
 * account.
 * We hold a reference to AccountJpaEntity here (not just the UUID) so JPA
 * can manage the foreign key correctly.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_account_id", columnList = "account_id"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class TransactionJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private AccountJpaEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "reference", nullable = false, updatable = false)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;
}