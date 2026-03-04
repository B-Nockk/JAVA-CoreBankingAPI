// src/main/java/com/nairacore/corebankingapi/account/application/port/out/UpdateAccountStatePort.java
package com.nairacore.corebankingapi.account.application.port.out;

import com.nairacore.corebankingapi.account.domain.Account;

public interface UpdateAccountStatePort {
    void save(Account account);
}