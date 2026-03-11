// app/src/main/java/com/coreledger/config/EventPublisher.java
package com.coreledger.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.coreledger.shared.DomainEventPublisher;
import com.coreledger.shared.domain.DomainEvent;

/**
 * Wraps KafkaTemplate to provide a clean publishing API for domain events.
 *
 * Why this wrapper exists instead of injecting KafkaTemplate directly:
 * - Services don't need to know topic names — that's infrastructure concern
 * - Single place to add error handling, logging, and future outbox logic
 * - Easy to swap implementation (e.g. outbox pattern) without touching services
 *
 * Key is the aggregateId — Kafka uses it for partition routing.
 * All events for the same aggregate go to the same partition,
 * guaranteeing ordering per aggregate.
 */
@Component
public class EventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.account-events}")
    private String accountEventsTopic;

    @Value("${kafka.topics.transfer-events}")
    private String transferEventsTopic;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish an account-domain event to the account events topic.
     * Use for: AccountCreated, MoneyDeposited, MoneyWithdrawn, TransferReversed
     */
    public void publishAccountEvent(DomainEvent event) {
        publish(accountEventsTopic, event);
    }

    /**
     * Publish a transfer-domain event to the transfer events topic.
     * Use for: TransferInitiated, TransferCompleted, TransferFailed
     */
    public void publishTransferEvent(DomainEvent event) {
        publish(transferEventsTopic, event);
    }

    private void publish(String topic, DomainEvent event) {
        String key = event.getAggregateId(); // partition key — same aggregate = same partition
        log.debug("Publishing {} to topic {} with key {}", event.getClass().getSimpleName(), topic, key);

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to topic {}: {}",
                                event.getClass().getSimpleName(), topic, ex.getMessage());
                    } else {
                        log.debug("Published {} to {}@{}",
                                event.getClass().getSimpleName(),
                                topic,
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
