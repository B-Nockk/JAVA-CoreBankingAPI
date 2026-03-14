// user-module/src/main/java/com/coreledger/user/application/port/in/CreateUserUseCase.java
package com.coreledger.user.application.port.in;

import java.time.LocalDate;

import com.coreledger.user.domain.model.UserId;
import com.coreledger.user.domain.model.UserRole;

public interface CreateUserUseCase {

    UserCreatedResult execute(Command command);

    record Command(
            String firstName,
            String lastName,
            String address,
            String phone,
            String email,
            LocalDate dateOfBirth,
            UserRole userRole) {
    }

    record UserCreatedResult(
            UserId id,
            String firstName,
            String lastName,
            String address,
            String phone,
            String email,
            LocalDate dateOfBirth,
            UserRole userRole) {
    }
}