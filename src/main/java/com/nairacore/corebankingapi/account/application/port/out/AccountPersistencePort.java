// src/main/java/com/nairacore/corebankingapi/account/application/port/out/AccountPersistencePort.java
package com.nairacore.corebankingapi.account.application.port.out;

import com.nairacore.corebankingapi.account.domain.Account;
import java.util.Optional;

public interface AccountPersistencePort {
    void save(Account account);

    Optional<Account> loadAccount(String accountNumber);
}
