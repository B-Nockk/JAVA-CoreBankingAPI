// user-module/src/main/java/com/coreledger/user/application/port/out/LoadKycDocumentPort.java
package com.coreledger.user.application.port.out;

import java.util.List;
import java.util.Optional;

import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycProfileId;

public interface LoadKycDocumentPort {

    /**
     * Fetches the actual binary content of a document.
     * Use this for downloading/viewing the file.
     */
    Optional<KycDocumentBinary> fetchDocument(KycDocumentId documentId);

    /**
     * Loads only the document metadata (without binary content).
     * Use this for listing documents, summaries, etc.
     */
    Optional<KycDocument> loadDocument(KycDocumentId documentId);

    /**
     * Loads all document metadata for a profile.
     * Useful for showing document lists without loading binaries.
     */
    List<KycDocument> loadDocumentsForProfile(KycProfileId profileId);

    /**
     * Checks if a document exists by its ID.
     */
    boolean documentExists(KycDocumentId documentId);

    /**
     * Gets the storage path for a document.
     * Useful for generating presigned URLs or direct access paths.
     */
    Optional<String> getStoragePath(KycDocumentId documentId);
}