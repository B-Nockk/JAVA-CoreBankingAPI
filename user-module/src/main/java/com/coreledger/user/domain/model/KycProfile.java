// user-module/src/main/java/com/coreledger/user/domain/model/KycProfile.java
package com.coreledger.user.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a user's KYC profile.
 * Owns the documents submitted and verification status.
 * Responsible for deriving the user's KYC tier based on rules.
 */
public class KycProfile {

    private final KycProfileId id;
    private final UserId userId;
    private final List<KycDocument> documents;
    private KycTier tier;

    /**
     * Constructor is private to enforce controlled creation.
     * Use factory methods to create or reconstitute.
     */
    private KycProfile(KycProfileId id, UserId userId, List<KycDocument> documents) {
        this.id = Objects.requireNonNull(id, "KycProfileId is required");
        this.userId = Objects.requireNonNull(userId, "UserId is required");
        this.documents = Objects.requireNonNull(documents, "Documents list is required");
        this.tier = deriveTier(documents);
    }

    /**
     * Factory method to create a new KYC profile.
     */
    public static KycProfile create(UserId userId, List<KycDocument> documents) {
        return new KycProfile(KycProfileId.generate(), userId, documents);
    }

    public static KycProfile reconstitute(KycProfileId id, UserId userId, List<KycDocument> documents, KycTier tier) {
        KycProfile profile = new KycProfile(id, userId, documents);
        profile.tier = tier; // restore persisted tier
        return profile;
    }

    // ====================================================
    // Domain behaviors
    // ====================================================

    /**
     * Derives the KYC tier based on submitted documents.
     * This is where business rules are applied.
     */
    private KycTier deriveTier(List<KycDocument> documents) {
        // Example rules (simplified):
        // - If only email/phone → Tier 1
        // - If ID card verified → Tier 2
        // - If ID + address proof + biometrics → Tier 3
        // - If corporate docs → Tier 4
        // In real life, this would be more complex.
        boolean hasId = documents.stream().anyMatch(d -> d.getType() == KycDocumentType.ID_CARD && d.isVerified());
        boolean hasAddress = documents.stream()
                .anyMatch(d -> d.getType() == KycDocumentType.ADDRESS_PROOF && d.isVerified());
        boolean hasBiometrics = documents.stream()
                .anyMatch(d -> d.getType() == KycDocumentType.BIOMETRICS && d.isVerified());
        boolean hasCorporate = documents.stream()
                .anyMatch(d -> d.getType() == KycDocumentType.CORPORATE_DOC && d.isVerified());

        if (hasCorporate)
            return KycTier.TIER_4;
        if (hasId && hasAddress && hasBiometrics)
            return KycTier.TIER_3;
        if (hasId)
            return KycTier.TIER_2;
        return KycTier.TIER_1;
    }

    /**
     * Recomputes kyc tier when document is submitted
     *
     * @param document
     */
    public void submitDocument(KycDocument document) {
        // documents should be a mutable list internally
        this.documents.add(document);
        this.tier = deriveTier(this.documents); // recompute
    }

    /**
     * Creates a snapshot of the current KYC profile state.
     * This is the recommended way to expose data externally
     * without leaking domain internals.
     */
    public KycSnapshot snapshot() {
        return new KycSnapshot(userId, tier, documents);
    }

    // ====================================================
    // Assessors
    // ====================================================

    /**
     * Returns the current KYC tier.
     */
    public KycTier getTier() {
        return tier;
    }

    /**
     * Returns the documents associated with this profile.
     */
    public List<KycDocument> getDocuments() {
        return documents;
    }

    public UserId getUserId() {
        return userId;
    }

    public KycProfileId getId() {
        return id;
    }

}
