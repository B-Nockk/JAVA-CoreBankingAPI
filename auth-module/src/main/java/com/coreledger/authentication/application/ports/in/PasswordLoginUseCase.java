// auth-module/src/main/java/com/coreledger/authentication/application/ports/in/PasswordLoginUseCase.java
package com.coreledger.authentication.application.ports.in;

import com.coreledger.authentication.domain.model.TokenPair;
import com.coreledger.shared.domain.EmailAddress;

public interface PasswordLoginUseCase {
    TokenPair login(Command command);

    record Command(
            EmailAddress userEmailAddress,
            String password,
            String clientIp,
            String userAgent) {
    }
}
