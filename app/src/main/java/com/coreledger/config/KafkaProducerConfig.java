package com.coreledger.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration.
 * Separated from EventPublisher (@Component) because @Bean methods
 * are only processed by Spring when declared inside @Configuration classes.
 */
@Configuration
public class KafkaProducerConfig {

    static final String TYPE_MAPPINGS = "AccountCreated:com.coreledger.account.domain.events.AccountCreated," +
            "MoneyDeposited:com.coreledger.shared.events.MoneyDeposited," +
            "MoneyWithdrawn:com.coreledger.shared.events.MoneyWithdrawn," +
            "TransferInitiated:com.coreledger.shared.events.TransferInitiated," +
            "TransferCompleted:com.coreledger.shared.events.TransferCompleted," +
            "TransferFailed:com.coreledger.shared.events.TransferFailed," +
            "TransferReversed:com.coreledger.shared.events.TransferReversed";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final ObjectMapper objectMapper;

    public KafkaProducerConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper; // inject your configured one
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.TYPE_MAPPINGS, TYPE_MAPPINGS);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);
        factory.setValueSerializer(new JsonSerializer<>(objectMapper));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}