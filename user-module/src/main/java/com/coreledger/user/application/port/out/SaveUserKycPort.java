// user-module/src/main/java/com/coreledger/user/application/port/out/SaveUserKycPort.java
package com.coreledger.user.application.port.out;

import com.coreledger.user.domain.model.KycProfile;

public interface SaveUserKycPort {

    /**
     * Saves a new or updates an existing KYC profile.
     */
    KycProfile save(KycProfile profile);

    /**
     * Updates only specific fields of a profile.
     * Useful for partial updates.
     */
    KycProfile update(KycProfile profile);
}