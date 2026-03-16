// user-module/src/main/java/com/coreledger/user/application/port/out/DeleteKycPort.java
package com.coreledger.user.application.port.out;

import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycProfileId;

/**
 * Port for deleting KYC data.
 * Combined interface for both profile and document deletions
 * since they're closely related.
 */
public interface DeleteKycPort {

    /**
     * Deletes an entire KYC profile and all associated documents.
     * Use with caution - typically when a user is deleted.
     *
     * @param profileId the profile to delete
     */
    void deleteProfile(KycProfileId profileId);

    /**
     * Deletes a single KYC document.
     * Removes both metadata and binary storage.
     *
     * @param documentId the document to delete
     */
    void deleteDocument(KycDocumentId documentId);

    /**
     * Deletes multiple documents at once.
     * Useful for cleanup operations.
     *
     * @param documentIds collection of documents to delete
     */
    void deleteDocuments(Iterable<KycDocumentId> documentIds);

    /**
     * Checks if a profile has any documents.
     * Useful before deletion.
     *
     * @param profileId the profile to check
     * @return true if profile has documents
     */
    boolean hasDocuments(KycProfileId profileId);
}