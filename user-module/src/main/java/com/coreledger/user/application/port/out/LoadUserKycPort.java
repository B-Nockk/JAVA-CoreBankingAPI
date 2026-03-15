// user-module/src/main/java/com/coreledger/user/application/port/out/LoadUserKycPort.java
package com.coreledger.user.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.coreledger.user.domain.model.KycProfile;
import com.coreledger.user.domain.model.UserId;

public interface LoadUserKycPort {
    Optional<KycProfile> findByUserId(UserId userId);

    Page<KycProfile> findAll(Pageable pageable);
}
