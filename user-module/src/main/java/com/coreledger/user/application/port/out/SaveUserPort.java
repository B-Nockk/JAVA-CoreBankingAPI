// user-module/src/main/java/com/coreledger/user/application/port/out/SaveUserPort.java

package com.coreledger.user.application.port.out;

import com.coreledger.user.domain.model.User;

public interface SaveUserPort {

    User save(User user);
}