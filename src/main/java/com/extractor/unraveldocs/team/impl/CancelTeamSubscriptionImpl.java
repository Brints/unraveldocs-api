package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.dto.response.TeamResponse;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.team.service.TeamBillingService;
import com.extractor.unraveldocs.team.service.TeamSubscriptionPlanService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Implementation for cancelling team subscriptions.
 * The user can cancel before subscription expires but can still use services
 * until expiry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelTeamSubscriptionImpl {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamBillingService teamBillingService;
    private final ResponseBuilderService responseBuilder;
    private final SanitizeLogging sanitizer;

    @Transactional
    public UnravelDocsResponse<TeamResponse> cancelSubscription(String teamId, User user) {
        log.info("Processing subscription cancellation for team {} by user {}", sanitizer.sanitizeLogging(teamId), sanitizer.sanitizeLogging(user.getEmail()));

        // Find the team
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        // Verify user is the owner
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (member.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Only the team owner can cancel the subscription");
        }

        // Check if already cancelled
        if (team.getSubscriptionStatus() == TeamSubscriptionStatus.CANCELLED) {
            log.info("Team {} subscription is already cancelled", sanitizer.sanitizeLogging(teamId));
            return buildResponse(team, "Subscription is already cancelled");
        }

        // Check if expired
        if (team.getSubscriptionStatus() == TeamSubscriptionStatus.EXPIRED) {
            throw new ForbiddenException("Cannot cancel an expired subscription");
        }

        // Cancel with payment gateway
        boolean gatewaySuccess = teamBillingService.cancelSubscription(team);
        if (!gatewaySuccess) {
            log.warn("Failed to cancel subscription in payment gateway for team {}", sanitizer.sanitizeLogging(teamId));
        }

        // Update team subscription status
        team.setAutoRenew(false);
        team.setCancellationRequestedAt(OffsetDateTime.now());
        team.setSubscriptionStatus(TeamSubscriptionStatus.CANCELLED);

        // Set subscription end date based on current period
        if (team.getSubscriptionStatus() == TeamSubscriptionStatus.TRIAL) {
            // If still in trial, subscription ends when trial ends
            team.setSubscriptionEndsAt(team.getTrialEndsAt());
        } else if (team.getNextBillingDate() != null) {
            // If active, subscription ends at next billing date
            team.setSubscriptionEndsAt(team.getNextBillingDate());
        } else {
            // Fallback: end immediately
            team.setSubscriptionEndsAt(OffsetDateTime.now());
        }

        teamRepository.save(team);

        log.info("Successfully cancelled subscription for team {} - access until {}",
                sanitizer.sanitizeLogging(teamId), sanitizer.sanitizeLoggingObject(team.getSubscriptionEndsAt()));

        return buildResponse(team, "Subscription cancelled successfully. You can continue using the service until "
                + team.getSubscriptionEndsAt().toLocalDate());
    }

    private UnravelDocsResponse<TeamResponse> buildResponse(Team team, String message) {
        TeamMember ownerMember = teamMemberRepository.findFirstByTeamIdAndRole(team.getId(), MemberRole.OWNER)
                .orElse(null);

        long memberCount = teamMemberRepository.countByTeamId(team.getId());

        TeamResponse response = TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .teamCode(team.getTeamCode())
                .subscriptionType(getPlanDisplayName(team))
                .billingCycle(team.getBillingCycle().getDisplayName())
                .subscriptionStatus(team.getSubscriptionStatus().getDisplayName())
                .subscriptionPrice(team.getSubscriptionPrice())
                .currency(team.getCurrency())
                .isActive(team.isActive())
                .isVerified(team.isVerified())
                .isClosed(team.isClosed())
                .autoRenew(team.isAutoRenew())
                .trialEndsAt(team.getTrialEndsAt())
                .nextBillingDate(team.getNextBillingDate())
                .subscriptionEndsAt(team.getSubscriptionEndsAt())
                .cancellationRequestedAt(team.getCancellationRequestedAt())
                .currentMemberCount((int) memberCount)
                .maxMembers(team.getMaxMembers())
                .monthlyDocumentLimit(team.getMonthlyDocumentLimit())
                .isOwner(ownerMember != null && ownerMember.getUser().getId().equals(team.getCreatedBy().getId()))
                .createdAt(team.getCreatedAt())
                .build();

        return responseBuilder.buildUserResponse(response, HttpStatus.OK, message);
    }

    private String getPlanDisplayName(Team team) {
        if (team.getPlan() != null) {
            return team.getPlan().getDisplayName();
        }
        // Fallback: format enum name nicely
        return team.getSubscriptionType().name().replace("_", " ");
    }
}
