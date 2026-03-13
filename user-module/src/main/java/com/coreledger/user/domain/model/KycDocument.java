// user-module/src/main/java/com/coreledger/user/domain/model/KycDocument.java
package com.coreledger.user.domain.model;

/**
 * Value object representing a single KYC document.
 * Immutable, with type and verification status.
 */
public class KycDocument {

    private final KycDocumentType type;
    private final boolean verified;

    public KycDocument(KycDocumentType type, boolean verified) {
        this.type = type;
        this.verified = verified;
    }

    public KycDocumentType getType() {
        return type;
    }

    public boolean isVerified() {
        return verified;
    }
}
