// app/src/main/java/com/coreledger/config/Jacksonconfig.java
package com.coreledger.config;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.coreledger.account.domain.events.AccountCreated;
import com.coreledger.shared.domain.Currency;
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.domain.Money;
import com.coreledger.shared.events.MoneyDeposited;
import com.coreledger.shared.events.MoneyWithdrawn;
import com.coreledger.shared.events.TransferCompleted;
import com.coreledger.shared.events.TransferFailed;
import com.coreledger.shared.events.TransferInitiated;
import com.coreledger.shared.events.TransferReversed;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson configuration using mix-ins.
 *
 * Mix-ins let us tell Jackson how to construct our domain classes
 * WITHOUT adding Jackson annotations to the domain classes themselves.
 * The domain stays framework-free; the annotation knowledge lives here
 * in app/ where infrastructure concerns belong.
 *
 * How mix-ins work:
 * - Define an abstract class or interface with @JsonCreator on a constructor
 * - Register it: mapper.addMixIn(DomainClass.class, MixInClass.class)
 * - Jackson uses the mix-in's @JsonCreator when deserialising DomainClass
 */
@Configuration
public class JacksonConfig {

    // ── Mix-ins ───────────────────────────────────────────────────────────────
    // Inside JacksonConfig, replace MoneyMixIn with this:

    static class MoneyDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Money> {
        @Override
        public Money deserialize(com.fasterxml.jackson.core.JsonParser p,
                com.fasterxml.jackson.databind.DeserializationContext ctxt)
                throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            Currency currency = Currency.valueOf(node.get("currency").asText());
            return Money.of(amount, currency);
        }
    }

    abstract static class MoneyMixIn {
        @JsonIgnore
        abstract boolean isZero();

        @JsonIgnore
        abstract boolean isNegative();

        @JsonIgnore
        abstract boolean isPositive();
    }

    abstract static class DomainEventMixIn {
        // DomainEvent has no default constructor and generates eventId/occurredOn
        // internally. For deserialisation we use the two-arg constructor that
        // accepts an explicit occurredOn so the original timestamp is preserved.
        @JsonCreator
        DomainEventMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("occurredOn") Instant occurredOn) {
        }

        @JsonIgnore // ← add this
        abstract String getEventType();
    }

    abstract static class TransferInitiatedMixIn {
        @JsonCreator
        TransferInitiatedMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("sourceAccountNumber") String sourceAccountNumber,
                @JsonProperty("destinationAccountNumber") String destinationAccountNumber,
                @JsonProperty("amount") Money amount) {
        }
    }

    abstract static class MoneyDepositedMixIn {
        @JsonCreator
        MoneyDepositedMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("accountNumber") String accountNumber,
                @JsonProperty("amount") Money amount,
                @JsonProperty("balanceAfter") Money balanceAfter,
                @JsonProperty("reference") String reference) {
        }
    }

    abstract static class MoneyWithdrawnMixIn {
        @JsonCreator
        MoneyWithdrawnMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("accountNumber") String accountNumber,
                @JsonProperty("amount") Money amount,
                @JsonProperty("balanceAfter") Money balanceAfter,
                @JsonProperty("reference") String reference) {
        }
    }

    abstract static class TransferCompletedMixIn {
        @JsonCreator
        TransferCompletedMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("sourceAccountNumber") String sourceAccountNumber,
                @JsonProperty("destinationAccountNumber") String destinationAccountNumber,
                @JsonProperty("amount") Money amount) {
        }
    }

    abstract static class TransferFailedMixIn {
        @JsonCreator
        TransferFailedMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("sourceAccountNumber") String sourceAccountNumber,
                @JsonProperty("amount") Money amount,
                @JsonProperty("reason") String reason) {
        }
    }

    abstract static class TransferReversedMixIn {
        @JsonCreator
        TransferReversedMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("sourceAccountNumber") String sourceAccountNumber,
                @JsonProperty("amount") Money amount) {
        }
    }

    abstract static class AccountCreatedMixIn {
        @JsonCreator
        AccountCreatedMixIn(
                @JsonProperty("aggregateId") String aggregateId,
                @JsonProperty("accountNumber") String accountNumber,
                @JsonProperty("ownerName") String ownerName,
                @JsonProperty("currency") Currency currency) {
        }
    }

    // ── ObjectMapper bean ─────────────────────────────────────────────────────

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Handle Instant serialisation
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register mix-ins
        // mapper.addMixIn(Money.class, MoneyMixIn.class);
        mapper.addMixIn(Money.class, MoneyMixIn.class); // suppresses isZero/isNegative/isPositive on serialize

        com.fasterxml.jackson.databind.module.SimpleModule moneyModule = new com.fasterxml.jackson.databind.module.SimpleModule();
        moneyModule.addDeserializer(Money.class, new MoneyDeserializer());
        mapper.registerModule(moneyModule);
        mapper.addMixIn(DomainEvent.class, DomainEventMixIn.class);
        mapper.addMixIn(TransferInitiated.class, TransferInitiatedMixIn.class);
        mapper.addMixIn(MoneyDeposited.class, MoneyDepositedMixIn.class);
        mapper.addMixIn(MoneyWithdrawn.class, MoneyWithdrawnMixIn.class);
        mapper.addMixIn(TransferCompleted.class, TransferCompletedMixIn.class);
        mapper.addMixIn(TransferFailed.class, TransferFailedMixIn.class);
        mapper.addMixIn(TransferReversed.class, TransferReversedMixIn.class);
        mapper.addMixIn(AccountCreated.class, AccountCreatedMixIn.class);

        return mapper;
    }
}