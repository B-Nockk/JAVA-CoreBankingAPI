// user-module/src/main/java/com/coreledger/user/application/port/out/LoadUserKycPort.java
package com.coreledger.user.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.KycProfile;
import com.coreledger.user.domain.model.KycProfileId;

public interface LoadUserKycPort {
    Optional<KycProfile> loadKycProfile(UserId userId);

    Optional<KycProfile> loadKycProfile(KycProfileId profileId);

    boolean hasKycProfile(UserId userId);

    Page<KycProfile> findAll(Pageable pageable);
}
