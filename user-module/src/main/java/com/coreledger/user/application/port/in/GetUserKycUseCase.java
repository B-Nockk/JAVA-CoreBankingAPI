// user-module/src/main/java/com/coreledger/user/application/port/in/GetUserKycUseCase.java
package com.coreledger.user.application.port.in;

import java.util.List;

import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.KycDocument;
import com.coreledger.user.domain.model.KycDocumentId;
import com.coreledger.user.domain.model.KycDocumentStatus;
import com.coreledger.user.domain.model.KycDocumentType;
import com.coreledger.user.domain.model.KycSnapshot;
import com.coreledger.user.domain.model.KycTier;

public interface GetUserKycUseCase {

    /**
     * Retrieves the current KYC profile snapshot for a user.
     *
     * @param userId the user's ID
     * @return the KYC snapshot
     * @throws com.coreledger.user.domain.exceptions.UserNotFoundException if user
     *                                                                     not found
     * @throws IllegalArgumentException                                    if userId
     *                                                                     is null
     */
    KycSnapshot getKycForUser(UserId userId);

    /**
     * Result DTO for KYC information (alternative to using domain snapshot
     * directly).
     * Use this if you want to decouple API from domain.
     */
    record UserKycResult(
            UserId userId,
            KycTier currentTier,
            List<KycDocumentSummary> documents,
            boolean isFullyVerified,
            int documentCount) {
        public static UserKycResult from(KycSnapshot snapshot) {
            return new UserKycResult(
                    snapshot.getUserId(),
                    snapshot.getTier(),
                    snapshot.getDocuments().stream()
                            .map(KycDocumentSummary::from)
                            .toList(),
                    snapshot.getTier() == KycTier.TIER_3 || snapshot.getTier() == KycTier.TIER_4,
                    snapshot.getDocuments().size());
        }
    }

    record KycDocumentSummary(
            KycDocumentId id,
            KycDocumentType type,
            KycDocumentStatus status,
            String rejectionReason) {
        public static KycDocumentSummary from(KycDocument doc) {
            return new KycDocumentSummary(
                    doc.getId(),
                    doc.getType(),
                    doc.getStatus(),
                    doc.getRejectionReason());
        }
    }
}