// user-module/src/main/java/com/coreledger/user/application/port/in/FlagUserUseCase.java
package com.coreledger.user.application.port.in;

// import java.util.UUID;

import com.coreledger.user.domain.model.UserId;

public interface FlagUserUseCase {
    void flagUser(UserId userId, String reason);
}
