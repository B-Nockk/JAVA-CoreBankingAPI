// user-module/src/main/java/com/coreledger/user/application/port/in/FlagUserUseCase.java
package com.coreledger.user.application.port.in;

import java.util.UUID;

public interface FlagUserUseCase {
    void flagUser(UUID userId, String reason);
}
