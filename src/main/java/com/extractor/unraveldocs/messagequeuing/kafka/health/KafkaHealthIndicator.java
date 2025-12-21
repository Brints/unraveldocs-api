package com.extractor.unraveldocs.messagequeuing.kafka.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Kafka cluster health checker that provides cluster status information.
 * Can be used by custom health endpoints or monitoring systems.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@ConditionalOnClass(KafkaAdmin.class)
public class KafkaHealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 10;

    /**
     * Check Kafka cluster health and return status details.
     *
     * @return Map containing health status details
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> healthDetails = new HashMap<>();

        try {
            Map<String, Object> clusterInfo = getClusterInfo();
            healthDetails.put("status", "UP");
            healthDetails.putAll(clusterInfo);
        } catch (Exception e) {
            log.error("Kafka health check failed: {}", e.getMessage());
            healthDetails.put("status", "DOWN");
            healthDetails.put("error", e.getMessage());
            healthDetails.put("errorType", e.getClass().getSimpleName());
        }

        return healthDetails;
    }

    /**
     * Check if Kafka cluster is healthy.
     *
     * @return true if the cluster is reachable and has brokers available
     */
    public boolean isHealthy() {
        try {
            Map<String, Object> clusterInfo = getClusterInfo();
            Integer brokersAvailable = (Integer) clusterInfo.get("brokersAvailable");
            return brokersAvailable != null && brokersAvailable > 0;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get detailed cluster information.
     */
    private Map<String, Object> getClusterInfo() throws Exception {
        Map<String, Object> adminConfig = new HashMap<>(kafkaAdmin.getConfigurationProperties());

        try (AdminClient adminClient = AdminClient.create(adminConfig)) {
            DescribeClusterOptions options = new DescribeClusterOptions()
                    .timeoutMs((int) TimeUnit.SECONDS.toMillis(HEALTH_CHECK_TIMEOUT_SECONDS));

            DescribeClusterResult clusterResult = adminClient.describeCluster(options);

            // Get cluster information
            String clusterId = clusterResult.clusterId().get(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Node controller = clusterResult.controller().get(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Collection<Node> nodes = clusterResult.nodes().get(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Build broker details
            String brokerInfo = nodes.stream()
                    .map(node -> String.format("%s:%d (id=%d)", node.host(), node.port(), node.id()))
                    .collect(Collectors.joining(", "));

            Map<String, Object> details = new HashMap<>();
            details.put("clusterId", clusterId);
            details.put("brokersAvailable", nodes.size());
            details.put("brokers", brokerInfo);
            details.put("controllerId", controller != null ? controller.id() : "unknown");
            details.put("controllerHost", controller != null ? controller.host() + ":" + controller.port() : "unknown");

            return details;
        }
    }
}

