// user-module/src/main/java/com/coreledger/user/application/port/in/DeactivateUserUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.UserId;

public interface DeactivateUserUseCase {
    void deactivateUser(UserId userId, String reason);
}
