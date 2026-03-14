// user-module/src/main/java/com/coreledger/user/application/port/in/SuspendUserUseCase.java
package com.coreledger.user.application.port.in;

import java.util.UUID;

public interface SuspendUserUseCase {
    void suspendUser(UUID userId, String reason);
}
