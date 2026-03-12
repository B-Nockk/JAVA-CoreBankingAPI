// app/src/main/java/com/coreledger/config/JacksonConfig.java
package com.coreledger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Single ObjectMapper bean used by:
 * - Spring MVC (HTTP request/response)
 * - Kafka producer (via KafkaProducerConfig)
 * - Kafka consumer (via KafkaConsumerConfig)
 *
 * All serialization/deserialization knowledge lives in the domain classes
 * themselves via @JsonCreator and @JsonProperty — no mix-ins needed.
 *
 * To add a new event type:
 * 1. Add @JsonCreator to its restore constructor in shared-kernel
 * 2. Add @JsonProperty to its getters
 * 3. Register the class in EventRegistry
 * Nothing changes here.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}