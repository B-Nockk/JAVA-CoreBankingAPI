// user-module/src/main/java/com/coreledger/user/application/port/in/DeleteKycProfileUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.UserId;

public interface DeleteKycProfileUseCase {

    /**
     * Command to delete an entire KYC profile.
     */
    record DeleteProfileCommand(
            UserId userId,
            String reason) {
        public DeleteProfileCommand {
            if (userId == null)
                throw new IllegalArgumentException("userId cannot be null");
            if (reason == null || reason.isBlank())
                throw new IllegalArgumentException("reason cannot be blank for profile deletion");
        }
    }

    /**
     * Permanently deletes a user's KYC profile and all associated documents.
     * This is a destructive operation - use with caution.
     *
     * @param command the deletion command
     */
    void deleteProfile(DeleteProfileCommand command);
}