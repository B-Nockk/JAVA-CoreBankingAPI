// src/main/java/com/nairacore/corebankingapi/account/dto/CreateAccountRequest.java
package com.nairacore.corebankingapi.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;

// A Record automatically generates constructors, getters, equals, and hashcode
public record CreateAccountRequest(
        @PositiveOrZero(message = "Initial deposit cannot be negative") BigDecimal initialDeposit) {
}