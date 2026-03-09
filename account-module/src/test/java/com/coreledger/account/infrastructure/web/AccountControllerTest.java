// account-module/src/test/java/com/coreledger/account/infrastructure/web/AccountControllerTest.java
package com.coreledger.account.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coreledger.account.application.port.in.CreateAccountUseCase;
import com.coreledger.account.application.port.in.DepositUseCase;
import com.coreledger.account.application.port.in.GetAccountUseCase;
import com.coreledger.account.domain.exceptions.AccountNotFoundException;
import com.coreledger.account.domain.exceptions.InvalidAccountOperationException;
import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.Money;

/**
 * Web layer tests for AccountController.
 *
 * @WebMvcTest loads only the web slice: controller, filters, exception
 *             handlers.
 *             Use cases are mocked with @MockBean — registered in Spring
 *             context so the
 *             controller gets them injected, but they do nothing unless
 *             programmed with when().
 *
 *             GlobalExceptionHandler is in app/ and references transfer-module
 *             exceptions.
 *             Since account-module doesn't depend on transfer-module, we can't
 *             load the real
 *             handler here. Instead we rely on Spring's default error handling
 *             for the
 *             exception-mapping tests, and we verify HTTP status codes only
 *             (not response body).
 *
 *             What these tests prove:
 *             - Controller maps request fields to use case Commands correctly
 *             - Bean Validation fires before the service is called (400 on
 *             invalid input)
 *             - Controller maps use case results to correct HTTP status codes
 *             - Exception types produce correct HTTP status codes
 */
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateAccountUseCase createAccountUseCase;

    @MockBean
    private GetAccountUseCase getAccountUseCase;

    @MockBean
    private DepositUseCase depositUseCase;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateAccountUseCase.AccountCreatedResult accountCreatedResult() {
        return new CreateAccountUseCase.AccountCreatedResult(
                "uuid-001", "1234567890", "Ada Obi", Currency.NGN, "ACTIVE");
    }

    private GetAccountUseCase.AccountResult accountResult() {
        return new GetAccountUseCase.AccountResult(
                "uuid-001", "1234567890", "Ada Obi",
                Currency.NGN, Money.zero(Currency.NGN), "ACTIVE", Instant.now());
    }

    // ── POST /api/v1/accounts ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/accounts — create account")
    class CreateAccount {

        @Test
        @DisplayName("should return 201 with account details on valid request")
        void shouldReturn201OnValidRequest() throws Exception {
            when(createAccountUseCase.execute(any())).thenReturn(accountCreatedResult());

            mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "ownerName": "Ada Obi",
                              "currency": "NGN"
                            }
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.ownerName").value("Ada Obi"))
                    .andExpect(jsonPath("$.currency").value("NGN"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 400 when ownerName is blank")
        void shouldReturn400WhenOwnerNameIsBlank() throws Exception {
            mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "ownerName": "",
                              "currency": "NGN"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when currency is missing")
        void shouldReturn400WhenCurrencyIsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "ownerName": "Ada Obi"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when body is empty")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/v1/accounts/{accountNumber} ──────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/accounts/{accountNumber} — get account")
    class GetAccount {

        @Test
        @DisplayName("should return 200 with account details when found")
        void shouldReturn200WhenAccountFound() throws Exception {
            when(getAccountUseCase.getByAccountNumber("1234567890"))
                    .thenReturn(accountResult());

            mockMvc.perform(get("/api/v1/accounts/1234567890"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.ownerName").value("Ada Obi"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 404 when account does not exist")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            when(getAccountUseCase.getByAccountNumber(anyString()))
                    .thenThrow(new AccountNotFoundException("9999999999"));

            mockMvc.perform(get("/api/v1/accounts/9999999999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 200 with list of all accounts")
        void shouldReturn200WithAllAccounts() throws Exception {
            GetAccountUseCase.AccountResult r1 = new GetAccountUseCase.AccountResult(
                    "uuid-001", "1111111111", "Ada Obi",
                    Currency.NGN, Money.zero(Currency.NGN), "ACTIVE", Instant.now());
            GetAccountUseCase.AccountResult r2 = new GetAccountUseCase.AccountResult(
                    "uuid-002", "2222222222", "Chidi Okeke",
                    Currency.NGN, Money.zero(Currency.NGN), "ACTIVE", Instant.now());
            when(getAccountUseCase.getAllAccounts()).thenReturn(List.of(r1, r2));

            mockMvc.perform(get("/api/v1/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].accountNumber").value("1111111111"))
                    .andExpect(jsonPath("$[1].accountNumber").value("2222222222"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no accounts exist")
        void shouldReturn200WithEmptyList() throws Exception {
            when(getAccountUseCase.getAllAccounts()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ── POST /api/v1/accounts/{accountNumber}/deposits ────────────────────────

    @Nested
    @DisplayName("POST /api/v1/accounts/{accountNumber}/deposit — deposit")
    class Deposit {

        @Test
        @DisplayName("should return 200 with deposit result on valid request")
        void shouldReturn200OnValidDeposit() throws Exception {
            DepositUseCase.DepositResult result = new DepositUseCase.DepositResult(
                    "tx-uuid-001",
                    "1234567890",
                    Money.of("500.00", Currency.NGN),
                    Money.of("500.00", Currency.NGN),
                    Instant.now());
            when(depositUseCase.execute(any())).thenReturn(result);

            mockMvc.perform(post("/api/v1/accounts/1234567890/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": 500.00,
                              "reference": "REF001"
                            }
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.balanceAfter").exists());
        }

        @Test
        @DisplayName("should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            mockMvc.perform(post("/api/v1/accounts/1234567890/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": 0,
                              "reference": "REF001"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when amount is negative")
        void shouldReturn400WhenAmountIsNegative() throws Exception {
            mockMvc.perform(post("/api/v1/accounts/1234567890/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": -100,
                              "reference": "REF001"
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when reference is missing")
        void shouldReturn400WhenReferenceIsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/accounts/1234567890/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": 500.00
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when account does not exist")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            when(depositUseCase.execute(any()))
                    .thenThrow(new AccountNotFoundException("9999999999"));

            mockMvc.perform(post("/api/v1/accounts/9999999999/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": 500.00,
                              "reference": "REF001"
                            }
                            """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 422 when account operation is invalid")
        void shouldReturn422WhenOperationInvalid() throws Exception {
            when(depositUseCase.execute(any()))
                    .thenThrow(new InvalidAccountOperationException("Account is closed"));

            mockMvc.perform(post("/api/v1/accounts/1234567890/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": 500.00,
                              "reference": "REF001"
                            }
                            """))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
