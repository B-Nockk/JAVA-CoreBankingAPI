// user-module/src/main/java/com/coreledger/user/application/port/in/SuspendUserUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.UserId;

public interface SuspendUserUseCase {
    void suspendUser(UserId userId, String reason);
}
