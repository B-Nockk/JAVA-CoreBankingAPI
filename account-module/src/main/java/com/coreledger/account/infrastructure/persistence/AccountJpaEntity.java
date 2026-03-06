// account-module/src/main/java/com/coreledger/account/infrastructure/persistence/AccountJpaEntity.java
package com.coreledger.account.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.coreledger.account.domain.model.AccountStatus;
import com.coreledger.shared.domain.Currency;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for the accounts table.
 *
 * This is NOT the domain model. It is a persistence mapping class whose
 * only job is to represent how account data is stored in the database.
 *
 * Why keep these separate?
 * - The domain Account has behaviour, invariants, and no JPA annotations.
 * Mixing JPA into the domain would force the domain to care about
 * column names, fetch strategies, and DB constraints — infrastructure concerns.
 * - JPA requires a no-args constructor and mutable fields (setters).
 * Our domain Account is intentionally not mutable in that way.
 * - When the DB schema changes, only this class changes — not the domain.
 *
 * balance is NOT stored here — it is derived from the transaction ledger.
 * This enforces the append-only design at the schema level.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class AccountJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_number", unique = true, nullable = false, length = 10)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    /**
     * CascadeType.ALL — persisting/deleting an account cascades to its
     * transactions.
     * orphanRemoval = true — a transaction removed from the list is deleted from
     * DB.
     * (In practice we never remove transactions, but this is correct JPA hygiene.)
     *
     * FetchType.LAZY — transactions are not loaded unless explicitly accessed.
     * For balance derivation we need them, so the persistence adapter will
     * ensure they are loaded when required.
     */
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<TransactionJpaEntity> transactions = new ArrayList<>();
}