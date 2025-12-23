package com.extractor.unraveldocs.brokers.kafka.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Custom Actuator endpoint for Kafka health information.
 * Accessible via /actuator/kafka
 */
@Component
@RequiredArgsConstructor
@Endpoint(id = "kafka")
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class KafkaHealthEndpoint {

    private final KafkaHealthIndicator kafkaHealthIndicator;

    /**
     * Get Kafka cluster health information.
     *
     * @return Map containing Kafka cluster health details
     */
    @ReadOperation
    public Map<String, Object> kafkaHealth() {
        return kafkaHealthIndicator.checkHealth();
    }
}

