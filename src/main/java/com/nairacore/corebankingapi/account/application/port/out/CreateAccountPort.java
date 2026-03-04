// src/main/java/com/nairacore/corebankingapi/account/application/port/out/CreateAccountPort.java
package com.nairacore.corebankingapi.account.application.port.out;

import com.nairacore.corebankingapi.account.domain.Account;

public interface CreateAccountPort {
    void insert(Account account);
}