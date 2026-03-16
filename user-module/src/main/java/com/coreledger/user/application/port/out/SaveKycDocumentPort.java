// user-module/src/main/java/com/coreledger/user/application/port/out/SaveKycDocumentPort.java
package com.coreledger.user.application.port.out;

import java.util.Optional;

import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentBinary;
import com.coreledger.user.domain.model.KycDocumentId;

public interface SaveKycDocumentPort {

    /**
     * Saves document metadata and binary content.
     *
     * @param kycDocument the document metadata
     * @param content     the binary file content
     * @param filename    original filename
     * @return the saved document
     */
    KycDocument save(KycDocument kycDocument, byte[] content, String filename);

    /**
     * Updates only the metadata of an existing document.
     * Binary content remains unchanged.
     *
     * @param kycDocument the document with updated metadata
     * @return the updated document
     */
    KycDocument updateMetadata(KycDocument kycDocument);

    /**
     * Retrieves the binary content for a document.
     * Note: This complements LoadKycDocumentPort.fetchDocument
     *
     * @param documentId the document ID
     * @return the document binary if found
     */
    Optional<KycDocumentBinary> fetchBinary(KycDocumentId documentId);
}