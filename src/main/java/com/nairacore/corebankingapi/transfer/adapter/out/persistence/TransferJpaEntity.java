// src/main/java/com/nairacore/corebankingapi/transfer/adapter/out/persistence/TransferJpaEntity.java
package com.nairacore.corebankingapi.transfer.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transfer_ledger", uniqueConstraints = {
        // Prevents two threads from saving the same version of a transfer
        @UniqueConstraint(columnNames = { "transferId", "version" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Surrogate Primary Key

    @Column(nullable = false)
    private String transferId; // The Business ID

    @Column(nullable = false)
    private String sourceAccountNumber;

    @Column(nullable = false)
    private String targetAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    @Column(nullable = false)
    private int version; // For Optimistic Locking / Append ordering
}