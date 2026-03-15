// user-module/src/main/java/com/coreledger/user/application/port/out/SaveUserKycPort.java
package com.coreledger.user.application.port.out;

import com.coreledger.user.domain.model.KycProfile;

public interface SaveUserKycPort {
    KycProfile save(KycProfile profile);
}
