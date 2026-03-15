// user-module/src/main/java/com/coreledger/user/domain/model/KycDocument.java
package com.coreledger.user.domain.model;

import java.util.Objects;

/**
 * Entity representing a single KYC document.
 * Has an ID, type, and lifecycle status.
 */
public class KycDocument {

    private final KycDocumentId id;
    private final KycProfileId profileId;
    private final KycDocumentType type;
    private KycDocumentStatus status;
    private String rejectionReason;

    public KycDocument(KycDocumentId id, KycProfileId profileId, KycDocumentType type) {
        this.id = Objects.requireNonNull(id);
        this.profileId = Objects.requireNonNull(profileId);
        this.type = Objects.requireNonNull(type);
        this.status = KycDocumentStatus.SUBMITTED;
    }

    public static KycDocument submit(KycProfileId profileId, KycDocumentType type) {
        return new KycDocument(KycDocumentId.generate(), profileId, type);
    }

    public void markVerified() {
        this.status = KycDocumentStatus.VERIFIED;
        this.rejectionReason = null;
    }

    public void markRejected(String reason) {
        this.status = KycDocumentStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public KycDocumentId getId() {
        return id;
    }

    public KycProfileId getProfileId() {
        return profileId;
    }

    public KycDocumentType getType() {
        return type;
    }

    public KycDocumentStatus getStatus() {
        return status;
    }

    public boolean isVerified() {
        return status == KycDocumentStatus.VERIFIED;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
