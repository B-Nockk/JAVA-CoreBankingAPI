// src/main/java/com/nairacore/corebankingapi/account/application/port/in/CreateAccountUseCase.java
package com.nairacore.corebankingapi.account.application.port.in;

import com.nairacore.corebankingapi.account.domain.Account;
import java.math.BigDecimal;

public interface CreateAccountUseCase {
    // The contract: give me an amount, I will give you a created Account
    Account createAccount(BigDecimal initialDeposit);
}