// app/src/main/java/com/coreledger/config/KafkaTopicConfig.java
package com.coreledger.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares Kafka topics as Spring beans.
 * Spring Kafka's KafkaAdmin picks these up on startup and creates
 * the topics if they don't already exist on the broker.
 *
 * partitions(1) — single partition for now, strict ordering guaranteed.
 * Scale to multiple partitions when we have multiple
 * app instances and can tolerate per-partition ordering.
 * replicas(1) — single broker in dev. Prod would use 3 for fault tolerance.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.account-events}")
    private String accountEventsTopic;

    @Value("${kafka.topics.transfer-events}")
    private String transferEventsTopic;

    @Bean
    public NewTopic accountEventsTopic() {
        return TopicBuilder.name(accountEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transferEventsTopic() {
        return TopicBuilder.name(transferEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
