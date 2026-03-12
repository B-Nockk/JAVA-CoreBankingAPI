// app/src/main/java/com/coreledger/config/KafkaConsumerConfig.java
package com.coreledger.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final ObjectMapper objectMapper;

    public KafkaConsumerConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        // Pass our ObjectMapper (with mix-ins) to JsonDeserializer
        // Use Object.class — actual type resolved from __TypeId__ header
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, objectMapper);
        jsonDeserializer.addTrustedPackages("com.coreledger.*");
        jsonDeserializer.setUseTypeHeaders(true); // ← use __TypeId__ header to determine type
        jsonDeserializer.setTypeMapper(typeMapper()); // ← resolve alias → class

        ErrorHandlingDeserializer<Object> valueDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
        ErrorHandlingDeserializer<String> keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "coreledger"); // ← add this
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put("spring.deserializer.value.delegate.class", JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props, keyDeserializer, valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        // log deserialization errors instead of silently skipping
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.CommonLoggingErrorHandler());
        return factory;
    }

    /**
     * Maps the short alias in __TypeId__ header → concrete class.
     * Must mirror KafkaProducerConfig.TYPE_MAPPINGS exactly.
     */
    private org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper typeMapper() {
        org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper mapper = new org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper();
        mapper.setTypePrecedence(
                org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);

        Map<String, Class<?>> mappings = new HashMap<>();
        mappings.put("AccountCreated", com.coreledger.account.domain.events.AccountCreated.class);
        mappings.put("MoneyDeposited", com.coreledger.shared.events.MoneyDeposited.class);
        mappings.put("MoneyWithdrawn", com.coreledger.shared.events.MoneyWithdrawn.class);
        mappings.put("TransferInitiated", com.coreledger.shared.events.TransferInitiated.class);
        mappings.put("TransferCompleted", com.coreledger.shared.events.TransferCompleted.class);
        mappings.put("TransferFailed", com.coreledger.shared.events.TransferFailed.class);
        mappings.put("TransferReversed", com.coreledger.shared.events.TransferReversed.class);
        mapper.setIdClassMapping(mappings);

        return mapper;
    }
}