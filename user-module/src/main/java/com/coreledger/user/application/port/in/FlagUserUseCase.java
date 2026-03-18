// user-module/src/main/java/com/coreledger/user/application/port/in/FlagUserUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.shared.domain.UserId;

public interface FlagUserUseCase {
    void flagUser(UserId userId, String reason);
}
