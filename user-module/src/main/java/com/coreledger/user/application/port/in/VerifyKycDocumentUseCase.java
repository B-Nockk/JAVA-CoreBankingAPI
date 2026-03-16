// user-module/src/main/java/com/coreledger/user/application/port/in/VerifyKycDocumentUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycTier;
import com.coreledger.user.domain.model.UserId;

public interface VerifyKycDocumentUseCase {

    /**
     * Command to verify a KYC document.
     */
    record VerifyCommand(
            UserId userId,
            KycDocumentId documentId) {
        public VerifyCommand {
            if (userId == null)
                throw new IllegalArgumentException("userId cannot be null");
            if (documentId == null)
                throw new IllegalArgumentException("documentId cannot be null");
        }
    }

    /**
     * Verifies a submitted KYC document.
     * This should typically be called by compliance/admin users.
     *
     * @param command the verification command
     * @return the new KYC tier after verification
     */
    KycTier verifyDocument(VerifyCommand command);
}