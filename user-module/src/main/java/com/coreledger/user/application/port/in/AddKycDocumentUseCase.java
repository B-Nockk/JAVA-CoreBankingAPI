// user-module/src/main/java/com/coreledger/user/application/port/in/AddKycDocumentUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycDocumentType;
import com.coreledger.user.domain.model.KycTier;
import com.coreledger.user.domain.model.UserId;

public interface AddKycDocumentUseCase {

    /**
     * Command to add a new KYC document for a user.
     */
    record Command(
            UserId userId,
            KycDocumentType documentType,
            String filename,
            byte[] fileContent,
            String contentType) {
        public Command {
            if (userId == null)
                throw new IllegalArgumentException("userId cannot be null");
            if (documentType == null)
                throw new IllegalArgumentException("documentType cannot be null");
            if (filename == null || filename.isBlank())
                throw new IllegalArgumentException("filename cannot be blank");
            if (fileContent == null || fileContent.length == 0)
                throw new IllegalArgumentException("fileContent cannot be empty");
            if (contentType == null || contentType.isBlank())
                throw new IllegalArgumentException("contentType cannot be blank");
        }
    }

    /**
     * Result of adding a document.
     */
    record AddDocumentResult(
            KycDocumentId documentId,
            String storagePath,
            KycTier newTier) {
    }

    /**
     * Adds a new KYC document to a user's profile.
     * The document is stored and linked to the user's KYC profile.
     *
     * @param command the document addition command
     * @return result containing document ID and new KYC tier
     * @throws IllegalStateException if user has no KYC profile
     */
    AddDocumentResult addDocument(Command command);
}