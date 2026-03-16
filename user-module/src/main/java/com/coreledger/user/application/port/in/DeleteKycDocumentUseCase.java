// user-module/src/main/java/com/coreledger/user/application/port/in/DeleteKycDocumentUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.UserId;

public interface DeleteKycDocumentUseCase {

    /**
     * Command to delete a specific KYC document.
     */
    record DeleteDocumentCommand(
            UserId userId,
            KycDocumentId documentId,
            String reason) {
        public DeleteDocumentCommand {
            if (userId == null)
                throw new IllegalArgumentException("userId cannot be null");
            if (documentId == null)
                throw new IllegalArgumentException("documentId cannot be null");
            // reason is optional, can be null for audit trail
        }
    }

    /**
     * Deletes a KYC document.
     * This should be restricted and properly audited.
     *
     * @param command the deletion command
     */
    void deleteDocument(DeleteDocumentCommand command);
}