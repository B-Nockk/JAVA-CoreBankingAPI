// user-module/src/main/java/com/coreledger/user/application/port/in/rj.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycTier;
import com.coreledger.user.domain.model.UserId;

public interface RejectKycDocumentUseCase {

    /**
     * Command to reject a KYC document.
     */
    record RejectCommand(
            UserId userId,
            KycDocumentId documentId,
            String reason) {
        public RejectCommand {
            if (userId == null)
                throw new IllegalArgumentException("userId cannot be null");
            if (documentId == null)
                throw new IllegalArgumentException("documentId cannot be null");
            if (reason == null || reason.isBlank())
                throw new IllegalArgumentException("reason cannot be blank");
        }
    }

    /**
     * Rejects a submitted KYC document with a reason.
     * This should typically be called by compliance/admin users.
     *
     * @param command the rejection command
     * @return the new KYC tier after rejection
     */
    KycTier rejectDocument(RejectCommand command);
}