// account-module/src/main/java/com/coreledger/account/infrastructure/web/request/CreateAccountRequest.java
package com.coreledger.account.infrastructure.web.request;

import com.coreledger.shared.domain.Currency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound DTO for POST /api/v1/accounts
 *
 * This is the shape of the JSON body the client sends.
 * It exists only in the web layer — it never enters the domain.
 * The controller maps this to a use case Command before calling the service.
 *
 * Java record: immutable by default, compact, no boilerplate.
 * Bean Validation annotations here catch bad input at the HTTP boundary
 * before any domain logic runs — fail fast, fail cheap.
 */
public record CreateAccountRequest(

        @NotBlank(message = "Owner name is required") String ownerName,

        @NotNull(message = "Currency is required") Currency currency) {
}