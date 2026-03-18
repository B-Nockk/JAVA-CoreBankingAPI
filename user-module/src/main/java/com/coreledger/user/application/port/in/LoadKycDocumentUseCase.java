// user-module/src/main/java/com/coreledger/user/application/port/in/LoadKycDocumentUseCase.java
package com.coreledger.user.application.port.in;

import java.util.Optional;

import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;

public interface LoadKycDocumentUseCase {

    /**
     * Command to load a document's binary content.
     */
    record LoadDocumentCommand(
            UserId userId,
            KycDocumentId documentId) {
        public LoadDocumentCommand {
            if (userId == null)
                throw new IllegalArgumentException("userId cannot be null");
            if (documentId == null)
                throw new IllegalArgumentException("documentId cannot be null");
        }
    }

    /**
     * Loads a KYC document's binary content.
     * This is typically used for downloading/viewing documents.
     *
     * @param command the load command
     * @return Optional containing the document binary if found
     */
    Optional<KycDocumentBinary> loadDocument(LoadDocumentCommand command);

    /**
     * Gets the download URL/path for a document.
     * Useful for generating presigned URLs etc.
     *
     * @param command the load command
     * @return Optional containing the access URL if available
     */
    Optional<String> getDocumentUrl(LoadDocumentCommand command);
}