package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.dto.response.TeamResponse;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
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
 * Implementation for team lifecycle operations.
 * Handles closing and reactivating teams.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloseTeamImpl {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ResponseBuilderService responseBuilder;
    private final SanitizeLogging sanitizer;

    /**
     * Closes a team.
     * Only the team owner can close the team.
     * Members remain but lose access until reactivated.
     *
     * @param teamId     The team ID
     * @param actingUser The user performing the action (must be owner)
     * @return Response indicating success
     */
    @Transactional
    public UnravelDocsResponse<Void> close(String teamId, User actingUser) {
        log.info("Closing team {} by user {}", sanitizer.sanitizeLogging(teamId), sanitizer.sanitizeLogging(actingUser.getId()));

        // 1. Get team
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        // 2. Verify the acting user is the owner
        TeamMember actingMember = teamMemberRepository.findByTeamIdAndUserId(teamId, actingUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (actingMember.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Only the team owner can close the team");
        }

        // 3. Check if already closed
        if (team.isClosed()) {
            throw new BadRequestException("This team is already closed");
        }

        // 4. Close the team
        team.setClosed(true);
        team.setClosedAt(OffsetDateTime.now());
        team.setActive(false);
        teamRepository.save(team);

        log.info("Team {} has been closed successfully", sanitizer.sanitizeLogging(teamId));

        return responseBuilder.buildVoidResponse(HttpStatus.OK,
                "Team has been closed. Members will retain their membership but lose access until reactivated.");
    }

    /**
     * Reactivates a closed team.
     * Only the team owner can reactivate the team.
     *
     * @param teamId     The team ID
     * @param actingUser The user performing the action (must be owner)
     * @return Response with reactivated team details
     */
    @Transactional
    public UnravelDocsResponse<TeamResponse> reactivate(String teamId, User actingUser) {
        log.info("Reactivating team {} by user {}", sanitizer.sanitizeLogging(teamId), sanitizer.sanitizeLogging(actingUser.getId()));

        // 1. Get team
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        // 2. Verify the acting user is the owner
        TeamMember actingMember = teamMemberRepository.findByTeamIdAndUserId(teamId, actingUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (actingMember.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Only the team owner can reactivate the team");
        }

        // 3. Check if team is closed
        if (!team.isClosed()) {
            throw new BadRequestException("This team is not closed");
        }

        // 4. Check subscription status - can only reactivate if subscription is valid
        if (team.getSubscriptionStatus() == TeamSubscriptionStatus.EXPIRED) {
            throw new ForbiddenException("Cannot reactivate team with expired subscription. " +
                    "Please renew your subscription first.");
        }

        // 5. Reactivate the team
        team.setClosed(false);
        team.setClosedAt(null);
        team.setActive(true);
        teamRepository.save(team);

        log.info("Team {} has been reactivated successfully", sanitizer.sanitizeLogging(teamId));

        // 6. Get member count for response
        long memberCount = teamMemberRepository.countByTeamId(teamId);

        TeamResponse response = buildTeamResponse(team, (int) memberCount);
        return responseBuilder.buildUserResponse(response, HttpStatus.OK, "Team has been reactivated successfully");
    }

    private TeamResponse buildTeamResponse(Team team, int memberCount) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .teamCode(team.getTeamCode())
                .subscriptionType(team.getPlan() != null ? team.getPlan().getDisplayName()
                        : team.getSubscriptionType().name().replace("_", " "))
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
                .createdAt(team.getCreatedAt())
                .currentMemberCount(memberCount)
                .maxMembers(team.getMaxMembers())
                .monthlyDocumentLimit(team.getMonthlyDocumentLimit())
                .isOwner(true) // Only owner can reach this point
                .build();
    }
}
