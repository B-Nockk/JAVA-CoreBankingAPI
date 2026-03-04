package com.nairacore.corebankingapi.transfer.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Transfer {

    private final String transferId;
    private final String sourceAccountNumber;
    private final String targetAccountNumber;
    private final BigDecimal amount;
    private final TransferStatus status;
    private final Instant createdAt;
    private final Instant completedAt;
    private final int version;

    // === Factory method for NEW transfers ===
    public static Transfer initiate(
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            Instant now) {

        Objects.requireNonNull(sourceAccountNumber);
        Objects.requireNonNull(targetAccountNumber);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(now);

        if (sourceAccountNumber.equals(targetAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        return new Transfer(
                UUID.randomUUID().toString(),
                sourceAccountNumber,
                targetAccountNumber,
                amount,
                TransferStatus.PENDING,
                now,
                null,
                0);
    }

    // === Rehydration constructor (for persistence layer only) ===
    public static Transfer rehydrate(
            String transferId,
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            TransferStatus status,
            Instant createdAt,
            Instant completedAt,
            int version) {
        return new Transfer(
                transferId,
                sourceAccountNumber,
                targetAccountNumber,
                amount,
                status,
                createdAt,
                completedAt,
                version);
    }

    private Transfer(
            String transferId,
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            TransferStatus status,
            Instant createdAt,
            Instant completedAt,
            int version) {
        this.transferId = transferId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.version = version;
    }

    // === State Transitions ===

    public Transfer complete(Instant now) {

        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Only pending transfers can be completed");
        }

        return new Transfer(
                this.transferId,
                this.sourceAccountNumber,
                this.targetAccountNumber,
                this.amount,
                TransferStatus.COMPLETED,
                this.createdAt,
                now,
                this.version + 1);
    }

    public Transfer fail() {

        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Only pending transfers can be failed");
        }

        return new Transfer(
                this.transferId,
                this.sourceAccountNumber,
                this.targetAccountNumber,
                this.amount,
                TransferStatus.FAILED,
                this.createdAt,
                null,
                this.version + 1);
    }

    // === Getters ===

    public String getTransferId() {
        return transferId;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public enum TransferStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}