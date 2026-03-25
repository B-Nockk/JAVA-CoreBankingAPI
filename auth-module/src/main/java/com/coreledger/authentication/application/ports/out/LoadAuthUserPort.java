// auth-module/src/main/java/com/coreledger/authentication/application/ports/out/LoadAuthUserPort.java
package com.coreledger.authentication.application.ports.out;

import java.util.Optional;

import com.coreledger.authentication.domain.model.AuthUser;
import com.coreledger.shared.domain.EmailAddress;

public interface LoadAuthUserPort {
    Optional<AuthUser> loadByEmail(EmailAddress emailAddress);
}