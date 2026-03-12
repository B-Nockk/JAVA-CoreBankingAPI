// app/src/main/java/com/coreledger/config/EventPublisher.java
package com.coreledger.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.coreledger.shared.DomainEventPublisher;
import com.coreledger.shared.domain.DomainEvent;
import com.coreledger.shared.kafka.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes domain events to Kafka wrapped in an EventEnvelope.
 *
 * Wire format for every message:
 * {
 * "eventType": "TransferInitiated",
 * "payload": { ...all event fields... }
 * }
 *
 * Key = aggregateId → Kafka partition routing.
 * All events for the same aggregate go to the same partition,
 * guaranteeing ordering per aggregate.
 */
@Component
public class EventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    @Value("${kafka.topics.account-events}")
    private String accountEventsTopic;

    @Value("${kafka.topics.transfer-events}")
    private String transferEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishAccountEvent(DomainEvent event) {
        publish(accountEventsTopic, event);
    }

    @Override
    public void publishTransferEvent(DomainEvent event) {
        publish(transferEventsTopic, event);
    }

    private void publish(String topic, DomainEvent event) {
        String eventType = event.getClass().getSimpleName();
        String key = event.getAggregateId();

        try {
            // Serialize event to JsonNode, then wrap in envelope
            JsonNode payload = objectMapper.valueToTree(event);
            EventEnvelope envelope = new EventEnvelope(eventType, payload);

            log.debug("Publishing {} to {} key={}", eventType, topic, key);

            kafkaTemplate.send(topic, key, envelope)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} to {} key={}: {}",
                                    eventType, topic, key, ex.getMessage(), ex);
                        } else {
                            log.debug("Published {} to {}@{} key={}",
                                    eventType, topic,
                                    result.getRecordMetadata().offset(), key);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize {} for publishing: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Event serialization failed for " + eventType, e);
        }
    }
}
