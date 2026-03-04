// src/main/java/com/nairacore/corebankingapi/transfer/application/port/in/InitiateTransferUseCase.java
package com.nairacore.corebankingapi.transfer.application.port.in;

import java.math.BigDecimal;

import com.nairacore.corebankingapi.transfer.domain.Transfer;

public interface InitiateTransferUseCase {
    Transfer initiateTransfer(
            BigDecimal amount,
            String recipientAccountNumber,
            String senderAccountNumber);
}