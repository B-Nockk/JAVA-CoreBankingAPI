// user-module/src/main/java/com/coreledger/user/application/port/in/UpdaterUserKycUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.KycTier;

public interface UpdateUserKycUseCase {

    /**
     * Command to manually update KYC tier.
     * This should be used sparingly, typically for manual overrides.
     */
    record UpdateKycCommand(
            UserId userId,
            KycTier newTier,
            String reason) {
        public UpdateKycCommand {
            if (userId == null)
                throw new IllegalArgumentException("userId cannot be null");
            if (newTier == null)
                throw new IllegalArgumentException("newTier cannot be null");
            if (reason == null || reason.isBlank())
                throw new IllegalArgumentException("reason cannot be blank");
        }
    }

    /**
     * Manually updates a user's KYC tier.
     * This should be restricted to admin users and properly audited.
     *
     * @param command the update command
     * @return the new KYC tier
     */
    KycTier updateKycTier(UpdateKycCommand command);

    /**
     * Forces a recalculation of KYC tier based on current documents.
     * Useful if business rules change.
     *
     * @param userId the user's ID
     * @return the recalculated tier
     */
    KycTier recalculateTier(UserId userId);
}