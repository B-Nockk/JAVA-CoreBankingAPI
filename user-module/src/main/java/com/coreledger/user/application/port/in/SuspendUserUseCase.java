// user-module/src/main/java/com/coreledger/user/application/port/in/SuspendUserUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.shared.domain.UserId;

public interface SuspendUserUseCase {
    void suspendUser(UserId userId, String reason);
}
