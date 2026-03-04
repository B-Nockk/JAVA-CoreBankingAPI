// src/main/java/com/nairacore/corebankingapi/account/application/port/out/LoadAccountPort.java
package com.nairacore.corebankingapi.account.application.port.out;

import java.util.Optional;

import com.nairacore.corebankingapi.account.domain.Account;

public interface LoadAccountPort {
    Optional<Account> loadAccount(String accountNumber);
}