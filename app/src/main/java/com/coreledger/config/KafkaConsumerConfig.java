// app/src/main/java/com/coreledger/config/KafkaConsumerConfig.java
package com.coreledger.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.kafka.EventDeserializer;
import com.coreledger.shared.kafka.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kafka consumer configuration.
 *
 * Pipeline:
 * 1. Raw bytes → EventEnvelope (StringDeserializer wraps JsonDeserializer)
 * 2. Listener receives EventEnvelope
 * 3. EventEnvelope.eventType looked up in EventRegistry
 * 4. EventEnvelope.payload deserialized into the correct DomainEvent subclass
 * 5. Handler dispatches on instanceof
 *
 * Error handling:
 * - ErrorHandlingDeserializer wraps envelope deserialization.
 * If the raw bytes can't be read as EventEnvelope, the error is logged
 * and the message is skipped (offset committed). This prevents poison pills
 * from blocking the consumer forever.
 * - CommonLoggingErrorHandler logs any exception thrown by listener methods.
 * - Both log at ERROR level with full stack trace — nothing swallowed silently.
 *
 * To add a new event type: add it to EventRegistry. Nothing changes here.
 */
@Configuration
public class KafkaConsumerConfig implements EventDeserializer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final ObjectMapper objectMapper;

    public KafkaConsumerConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public ConsumerFactory<String, EventEnvelope> consumerFactory() {
        // Deserialize the envelope itself — concrete type is always EventEnvelope
        JsonDeserializer<EventEnvelope> jsonDeserializer = new JsonDeserializer<>(EventEnvelope.class, objectMapper);
        jsonDeserializer.setUseTypeHeaders(false); // we don't use __TypeId__ headers

        ErrorHandlingDeserializer<EventEnvelope> valueDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
        ErrorHandlingDeserializer<String> keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "coreledger");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props, keyDeserializer, valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> kafkaListenerContainerFactory(
            ConsumerFactory<String, EventEnvelope> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        // Logs full stack trace on any listener exception — no silent swallowing
        factory.setCommonErrorHandler(new CommonLoggingErrorHandler());
        return factory;
    }

    /**
     * Deserializes the envelope payload into the correct DomainEvent subclass.
     * Call this from every @KafkaListener method.
     *
     * Returns null if eventType is unknown — listener should log and skip.
     */
    public DomainEvent deserialize(EventEnvelope envelope) {
        if (envelope == null) {
            log.error("Received null envelope from Kafka — skipping");
            return null;
        }

        String eventType = envelope.eventType();
        // log.debug("Resolving eventType={} to {}", eventType, clazz.getName());

        return EventRegistry.resolve(eventType)
                .map(clazz -> {
                    try {
                        Object payload = envelope.payload();
                        if (payload instanceof JsonNode node) {
                            return (DomainEvent) objectMapper.treeToValue(node, clazz);
                        } else {
                            return (DomainEvent) objectMapper.convertValue(payload, clazz);
                        }
                    } catch (Exception e) {
                        log.error("Failed to deserialize payload for eventType={} payload={}", eventType,
                                envelope.payload(), e);
                        return null;
                    }
                })
                .orElseGet(() -> {
                    log.warn("Unknown eventType='{}' — not in EventRegistry, skipping", eventType);
                    return null;
                });

    }
}