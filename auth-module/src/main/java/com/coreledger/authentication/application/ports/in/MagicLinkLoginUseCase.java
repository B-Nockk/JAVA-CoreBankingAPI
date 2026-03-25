// auth-module/src/main/java/com/coreledger/authentication/application/ports/in/MagicLinkLoginUseCase.java
package com.coreledger.authentication.application.ports.in;

import com.coreledger.authentication.domain.model.TokenPair;

public interface MagicLinkLoginUseCase {
    TokenPair login(Command command);

    record Command(
            String magicLinkToken,
            String clientIp,
            String userAgent) {
    }
}