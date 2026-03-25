// auth-module/src/main/java/com/coreledger/authentication/application/ports/in/MfaLoginUseCase.java
package com.coreledger.authentication.application.ports.in;

import com.coreledger.authentication.domain.model.TokenPair;

public interface MfaLoginUseCase {
    TokenPair login(Command command);

    record Command(
            String username,
            String mfaCode,
            String clientIp,
            String userAgent) {
    }
}