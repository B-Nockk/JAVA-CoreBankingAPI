// user-module/src/main/java/com/coreledger/user/application/port/in/ReactivateUserUseCase.java
package com.coreledger.user.application.port.in;

import com.coreledger.user.domain.model.UserId;

public interface ReactivateUserUseCase {
    void reactivateUser(UserId userId, String reason);
}
