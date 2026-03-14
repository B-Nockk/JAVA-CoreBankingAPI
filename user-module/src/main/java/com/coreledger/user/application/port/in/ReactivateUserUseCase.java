// user-module/src/main/java/com/coreledger/user/application/port/in/ReactivateUserUseCase.java
package com.coreledger.user.application.port.in;

import java.util.UUID;

public interface ReactivateUserUseCase {
    void reactivateUser(UUID userId, String reason);
}
