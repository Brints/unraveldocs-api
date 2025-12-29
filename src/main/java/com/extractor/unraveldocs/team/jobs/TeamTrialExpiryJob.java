package com.extractor.unraveldocs.team.jobs;

import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.brokers.rabbitmq.events.BaseEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventMetadata;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventPublisherService;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.events.TeamTrialExpiringEvent;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.team.service.TeamSubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled job to send trial expiry warning emails 3 days before trial ends.
 * Runs daily at 9:00 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamTrialExpiryJob {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EventPublisherService eventPublisherService;
    private final TeamSubscriptionPlanService planService;
    private final SanitizeLogging sanitizer;

    private static final int DAYS_BEFORE_EXPIRY = 3;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    /**
     * Runs daily at 9:00 AM to check for teams with trials expiring in 3 days.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendTrialExpiryReminders() {
        log.info("Starting team trial expiry reminder job...");

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime reminderDate = now.plusDays(DAYS_BEFORE_EXPIRY);

        List<Team> teamsNeedingReminder = teamRepository.findTeamsNeedingTrialReminder(now, reminderDate);

        log.info("Found {} teams needing trial expiry reminder", sanitizer.sanitizeLoggingInteger(teamsNeedingReminder.size()));

        for (Team team : teamsNeedingReminder) {
            try {
                sendTrialExpiringEvent(team);
                teamRepository.markTrialReminderSent(team.getId());
                log.info("Sent trial expiry reminder for team: {} ({})", sanitizer.sanitizeLogging(team.getName()), sanitizer.sanitizeLogging(team.getTeamCode()));
            } catch (Exception e) {
                log.error("Failed to send trial expiry reminder for team: {} - {}",
                        sanitizer.sanitizeLogging(team.getTeamCode()), e.getMessage(), e);
            }
        }

        log.info("Completed team trial expiry reminder job. Processed {} teams.", sanitizer.sanitizeLoggingInteger(teamsNeedingReminder.size()));
    }

    private void sendTrialExpiringEvent(Team team) {
        // Find the team owner
        TeamMember owner = teamMemberRepository.findFirstByTeamIdAndRole(team.getId(), MemberRole.OWNER)
                .orElseThrow(() -> new IllegalStateException("Team has no owner: " + team.getTeamCode()));

        int daysRemaining = team.getDaysUntilTrialExpiry();

        // Get price from database-driven plan
        String price = getPlanPrice(team);
        String subscriptionTypeName = getPlanDisplayName(team);

        TeamTrialExpiringEvent payload = TeamTrialExpiringEvent.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .teamCode(team.getTeamCode())
                .ownerEmail(owner.getUser().getEmail())
                .ownerFirstName(owner.getUser().getFirstName())
                .subscriptionType(subscriptionTypeName)
                .billingCycle(team.getBillingCycle().getDisplayName())
                .price(price)
                .currency(team.getCurrency())
                .trialEndsAt(team.getTrialEndsAt().format(DATE_FORMATTER))
                .daysRemaining(daysRemaining)
                .build();

        EventMetadata metadata = EventMetadata.builder()
                .eventType(EventTypes.TEAM_TRIAL_EXPIRING)
                .eventSource("TeamTrialExpiryJob")
                .eventTimestamp(System.currentTimeMillis())
                .correlationId(UUID.randomUUID().toString())
                .build();

        BaseEvent<TeamTrialExpiringEvent> event = new BaseEvent<>(metadata, payload);

        eventPublisherService.publishEvent(
                RabbitMQQueueConfig.TEAM_EVENTS_EXCHANGE,
                RabbitMQQueueConfig.TEAM_TRIAL_EXPIRING_ROUTING_KEY,
                event);
    }

    private String getPlanPrice(Team team) {
        if (team.getPlan() != null) {
            return team.getPlan().getPrice(team.getBillingCycle()).toString();
        }
        // Fallback: lookup from database
        return planService.getPrice(team.getSubscriptionType().name(), team.getBillingCycle()).toString();
    }

    private String getPlanDisplayName(Team team) {
        if (team.getPlan() != null) {
            return team.getPlan().getDisplayName();
        }
        // Fallback: format enum name nicely
        return team.getSubscriptionType().name().replace("_", " ");
    }
}
