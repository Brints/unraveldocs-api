package com.extractor.unraveldocs.team.events;

import com.extractor.unraveldocs.brokers.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.brokers.service.EmailMessageProducerService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer for team events.
 * Handles team trial expiring events and sends appropriate emails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class TeamEventListener {

    private final EmailMessageProducerService emailMessageProducerService;
    private final SanitizeLogging sanitizer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_TEAM_EVENTS, groupId = "team-events-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleTeamEvent(Object event, Acknowledgment acknowledgment) {
        try {
            // Convert to TeamTrialExpiringEvent if needed
            TeamTrialExpiringEvent trialEvent = convertToTrialEvent(event);
            if (trialEvent != null) {
                handleTrialExpiringEvent(trialEvent);
            } else {
                log.warn("Received unknown team event type: {}",
                        sanitizer.sanitizeLogging(event.getClass().getName()));
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing team event: {}", e.getMessage(), e);
            throw e; // Rethrow to trigger retry/DLQ handling
        }
    }

    private TeamTrialExpiringEvent convertToTrialEvent(Object event) {
        if (event instanceof TeamTrialExpiringEvent trialEvent) {
            return trialEvent;
        }
        if (event instanceof Map) {
            try {
                return objectMapper.convertValue(event, TeamTrialExpiringEvent.class);
            } catch (Exception e) {
                log.debug("Could not convert event to TeamTrialExpiringEvent: {}", e.getMessage());
            }
        }
        return null;
    }

    private void handleTrialExpiringEvent(TeamTrialExpiringEvent event) {
        log.info("Processing trial expiring event for team: {} ({})",
                sanitizer.sanitizeLogging(event.getTeamName()),
                sanitizer.sanitizeLogging(event.getTeamCode()));

        Map<String, Object> templateVariables = getStringObjectMap(event);

        emailMessageProducerService.queueEmail(
                event.getOwnerEmail(),
                "Your " + event.getTeamName() + " team trial ends in " + event.getDaysRemaining() + " days",
                "team-trial-expiring",
                templateVariables);

        log.info("Successfully queued trial expiry email for team: {}",
                sanitizer.sanitizeLogging(event.getTeamCode()));
    }

    private static @NonNull Map<String, Object> getStringObjectMap(TeamTrialExpiringEvent event) {
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("firstName", event.getOwnerFirstName());
        templateVariables.put("teamName", event.getTeamName());
        templateVariables.put("daysRemaining", event.getDaysRemaining());
        templateVariables.put("trialEndsAt", event.getTrialEndsAt());
        templateVariables.put("subscriptionType", event.getSubscriptionType());
        templateVariables.put("billingCycle", event.getBillingCycle());
        templateVariables.put("price", event.getPrice());
        templateVariables.put("currency", event.getCurrency());
        return templateVariables;
    }
}
