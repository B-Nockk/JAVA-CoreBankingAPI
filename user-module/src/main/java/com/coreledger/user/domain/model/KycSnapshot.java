// user-module/src/main/java/com/coreledger/user/domain/model/KycSnapshot.java
package com.coreledger.user.domain.model;

import java.util.List;

/**
 * Immutable value object representing a snapshot of a user's KYC profile.
 * This is a read-only projection of the aggregate state,
 * suitable for returning to the frontend or other consumers.
 *
 * Contains the userId, current tier, and the list of documents with their
 * verification status.
 */
public class KycSnapshot {

    private final UserId userId;
    private final KycTier tier;
    private final List<KycDocument> documents;

    public KycSnapshot(UserId userId, KycTier tier, List<KycDocument> documents) {
        this.userId = userId;
        this.tier = tier;
        this.documents = List.copyOf(documents); // defensive copy for immutability
    }

    /**
     * @return the unique identifier of the user
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * @return the current KYC tier derived from documents
     */
    public KycTier getTier() {
        return tier;
    }

    /**
     * @return the list of submitted documents and their verification status
     */
    public List<KycDocument> getDocuments() {
        return documents;
    }
}
