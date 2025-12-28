package com.extractor.unraveldocs.team.events;

import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.messaging.dto.EmailRequestDto;
import com.extractor.unraveldocs.messaging.service.EmailRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to team events from RabbitMQ and sends appropriate emails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class TeamEventListener {

    private final EmailRequestPublisher emailRequestPublisher;

    @RabbitListener(queues = RabbitMQQueueConfig.TEAM_EVENTS_QUEUE)
    public void handleTeamEvent(Object event) {
        if (event instanceof TeamTrialExpiringEvent trialEvent) {
            handleTrialExpiringEvent(trialEvent);
        } else {
            log.warn("Received unknown team event type: {}", event.getClass().getName());
        }
    }

    private void handleTrialExpiringEvent(TeamTrialExpiringEvent event) {
        log.info("Processing trial expiring event for team: {} ({})", event.getTeamName(), event.getTeamCode());

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("firstName", event.getOwnerFirstName());
        templateVariables.put("teamName", event.getTeamName());
        templateVariables.put("daysRemaining", event.getDaysRemaining());
        templateVariables.put("trialEndsAt", event.getTrialEndsAt());
        templateVariables.put("subscriptionType", event.getSubscriptionType());
        templateVariables.put("billingCycle", event.getBillingCycle());
        templateVariables.put("price", event.getPrice());
        templateVariables.put("currency", event.getCurrency());

        EmailRequestDto emailRequest = EmailRequestDto.builder()
                .to(event.getOwnerEmail())
                .subject("Your " + event.getTeamName() + " team trial ends in " + event.getDaysRemaining() + " days")
                .template("team-trial-expiring")
                .templateVariables(templateVariables)
                .build();

        emailRequestPublisher.publishEmailRequest(emailRequest);

        log.info("Successfully queued trial expiry email for team: {}", event.getTeamCode());
    }
}
