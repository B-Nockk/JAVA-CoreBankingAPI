// user-module/src/main/java/com/coreledger/user/application/port/out/LoadUserPort.java
package com.coreledger.user.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.coreledger.shared.domain.EmailAddress;
import com.coreledger.shared.domain.UserId;
import com.coreledger.user.domain.model.User;

public interface LoadUserPort {
    Optional<User> findById(UserId id);

    Optional<User> findByEmail(EmailAddress emailAddress);

    Page<User> findAll(Pageable pageable);
}
