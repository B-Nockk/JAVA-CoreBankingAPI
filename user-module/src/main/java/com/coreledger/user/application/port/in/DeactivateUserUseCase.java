// user-module/src/main/java/com/coreledger/user/application/port/in/DeactivateUserUseCase.java
package com.coreledger.user.application.port.in;

import java.util.UUID;

public interface DeactivateUserUseCase {
    void deactivateUser(UUID userId, String reason);
}
