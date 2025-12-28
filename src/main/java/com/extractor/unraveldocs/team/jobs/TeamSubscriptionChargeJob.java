package com.extractor.unraveldocs.team.jobs;

import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.team.service.TeamBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to process subscription charges:
 * 1. Auto-charge teams when trial expires (if auto-renew enabled)
 * 2. Expire teams that have cancelled and passed subscription end date
 * 3. Charge recurring subscriptions due for billing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamSubscriptionChargeJob {

    private final TeamRepository teamRepository;
    private final TeamBillingService teamBillingService;

    /**
     * Runs daily at 2:00 AM to process subscription charges.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processSubscriptionCharges() {
        log.info("Starting team subscription charge job...");

        OffsetDateTime now = OffsetDateTime.now();
        int processedCount = 0;
        int failedCount = 0;

        // 1. Process trial expiry with auto-renew
        List<Team> teamsForAutoCharge = teamRepository.findTeamsWithExpiredTrialForAutoCharge(now);
        log.info("Found {} teams with expired trial for auto-charge", teamsForAutoCharge.size());

        for (Team team : teamsForAutoCharge) {
            try {
                boolean success = teamBillingService.chargeSubscription(team);
                if (success) {
                    team.setSubscriptionStatus(TeamSubscriptionStatus.ACTIVE);
                    team.setLastBillingDate(now);
                    team.setNextBillingDate(calculateNextBillingDate(team, now));
                    teamRepository.save(team);
                    processedCount++;
                    log.info("Successfully charged team {} for subscription", team.getTeamCode());
                } else {
                    team.setSubscriptionStatus(TeamSubscriptionStatus.PAST_DUE);
                    teamRepository.save(team);
                    failedCount++;
                    log.warn("Payment failed for team {}", team.getTeamCode());
                }
            } catch (Exception e) {
                log.error("Error processing charge for team {}: {}", team.getTeamCode(), e.getMessage(), e);
                failedCount++;
            }
        }

        // 2. Expire teams with no auto-renew
        List<Team> teamsToExpire = teamRepository.findTeamsWithExpiredTrialNoAutoRenew(now);
        log.info("Found {} teams to expire (no auto-renew)", teamsToExpire.size());

        for (Team team : teamsToExpire) {
            team.setSubscriptionStatus(TeamSubscriptionStatus.EXPIRED);
            team.setActive(false);
            teamRepository.save(team);
            log.info("Expired team {} (no auto-renew)", team.getTeamCode());
        }

        // 3. Expire cancelled teams past their subscription end date
        List<Team> cancelledTeamsToExpire = teamRepository.findCancelledTeamsReadyToExpire(now);
        log.info("Found {} cancelled teams to expire", cancelledTeamsToExpire.size());

        for (Team team : cancelledTeamsToExpire) {
            team.setSubscriptionStatus(TeamSubscriptionStatus.EXPIRED);
            team.setActive(false);
            teamRepository.save(team);
            log.info("Expired cancelled team {}", team.getTeamCode());
        }

        // 4. Process recurring billing for active subscriptions
        List<Team> teamsDueForBilling = teamRepository.findTeamsDueForBilling(now);
        log.info("Found {} teams due for recurring billing", teamsDueForBilling.size());

        for (Team team : teamsDueForBilling) {
            try {
                boolean success = teamBillingService.chargeSubscription(team);
                if (success) {
                    team.setLastBillingDate(now);
                    team.setNextBillingDate(calculateNextBillingDate(team, now));
                    teamRepository.save(team);
                    processedCount++;
                    log.info("Successfully charged recurring billing for team {}", team.getTeamCode());
                } else {
                    team.setSubscriptionStatus(TeamSubscriptionStatus.PAST_DUE);
                    teamRepository.save(team);
                    failedCount++;
                    log.warn("Recurring payment failed for team {}", team.getTeamCode());
                }
            } catch (Exception e) {
                log.error("Error processing recurring charge for team {}: {}", team.getTeamCode(), e.getMessage(), e);
                failedCount++;
            }
        }

        log.info("Completed team subscription charge job. Processed: {}, Failed: {}", processedCount, failedCount);
    }

    private OffsetDateTime calculateNextBillingDate(Team team, OffsetDateTime from) {
        return switch (team.getBillingCycle()) {
            case MONTHLY -> from.plusMonths(1);
            case YEARLY -> from.plusYears(1);
        };
    }
}
