// user-module/src/main/java/com/coreledger/user/domain/model/KycProfile.java
package com.coreledger.user.domain.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.coreledger.shared.domain.UserId;

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
     * Submit a new document to the profile.
     * Starts in SUBMITTED status.
     *
     * @param document
     */
    public KycDocument submitDocument(KycDocumentType type) {
        // documents should be a mutable list internally
        KycDocument document = KycDocument.submit(this.id, type);
        this.addDocument(document);
        return document;
    }

    public void addDocument(KycDocument document) {
        this.documents.add(document);
        this.tier = deriveTier(this.documents);
    }

    /**
     * Mark a document as verified by its ID.
     * Recomputes tier after verification.
     */
    public void verifyDocument(KycDocumentId documentId) {
        documents.stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .ifPresent(KycDocument::markVerified);
        this.tier = deriveTier(this.documents);
    }

    /**
     * Reject a document by its ID, with a reason.
     * Recomputes tier after rejection.
     */
    public void rejectDocument(KycDocumentId documentId, String reason) {
        documents.stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .ifPresent(d -> d.markRejected(reason));
        this.tier = deriveTier(this.documents);
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

    // ====================================================
    // Recalculate TIer
    // ====================================================

    /**
     * Forces recalculation of KYC tier based on current documents.
     * Useful when business rules change or after document deletion.
     *
     * @return the new tier after recalculation
     */
    public KycTier recalculateTier() {
        this.tier = deriveTier(this.documents);
        return this.tier;
    }

    /**
     * Removes a document from the profile and recalculates tier.
     * Used when a document is deleted.
     *
     * @param documentId the ID of the document to remove
     * @return the new tier after removal and recalculation
     * @throws IllegalArgumentException if document not found
     */
    public KycTier removeDocument(KycDocumentId documentId) {
        boolean removed = this.documents.removeIf(doc -> doc.getId().equals(documentId));

        if (!removed) {
            throw new IllegalArgumentException("Document not found in profile: " + documentId);
        }

        return recalculateTier();
    }

    /**
     * Batch removes multiple documents and recalculates tier once.
     * More efficient than removing one by one.
     *
     * @param documentIds collection of document IDs to remove
     * @return the new tier after removal and recalculation
     */
    public KycTier removeDocuments(Collection<KycDocumentId> documentIds) {
        this.documents.removeIf(doc -> documentIds.contains(doc.getId()));
        return recalculateTier();
    }

    /**
     * Checks if profile has any verified documents of a specific type.
     * Useful for validation rules.
     */
    public boolean hasVerifiedDocument(KycDocumentType type) {
        return documents.stream()
                .anyMatch(doc -> doc.getType() == type && doc.isVerified());
    }

    /**
     * Gets count of documents by status.
     * Useful for reporting and validation.
     */
    public Map<KycDocumentStatus, Long> getDocumentCountByStatus() {
        return documents.stream()
                .collect(Collectors.groupingBy(
                        KycDocument::getStatus,
                        Collectors.counting()));
    }
}