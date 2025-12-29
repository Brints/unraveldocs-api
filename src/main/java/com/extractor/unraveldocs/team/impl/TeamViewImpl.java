package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.dto.response.TeamMemberResponse;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation for viewing team details and members.
 * Provides read-only operations for teams.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamViewImpl {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ResponseBuilderService responseBuilder;
    private final SanitizeLogging sanitizer;

    /**
     * Gets team details by ID.
     * Only accessible to team members.
     *
     * @param teamId The team ID
     * @param user   The authenticated user
     * @return Response with team details
     */
    @Transactional(readOnly = true)
    public UnravelDocsResponse<TeamResponse> getTeam(String teamId, User user) {
        log.debug(
                "Getting team {} for user {}",
                sanitizer.sanitizeLogging(teamId),
                sanitizer.sanitizeLogging(user.getId()));

        // 1. Get team
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        // 2. Verify user is a member
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        // 3. Get member count
        long memberCount = teamMemberRepository.countByTeamId(teamId);

        // 4. Build response
        boolean isOwner = member.getRole() == MemberRole.OWNER;
        TeamResponse response = buildTeamResponse(team, (int) memberCount, isOwner);

        return responseBuilder.buildUserResponse(response, HttpStatus.OK, "Team details retrieved successfully");
    }

    /**
     * Gets all teams the user belongs to.
     *
     * @param user The authenticated user
     * @return Response with list of teams
     */
    @Transactional(readOnly = true)
    public UnravelDocsResponse<List<TeamResponse>> getMyTeams(User user) {
        log.debug("Getting teams for user {}", sanitizer.sanitizeLogging(user.getId()));

        // Get all memberships for user
        List<TeamMember> memberships = teamMemberRepository.findByUserId(user.getId());

        // Build responses for each team
        List<TeamResponse> teams = memberships.stream()
                .map(membership -> {
                    Team team = membership.getTeam();
                    long memberCount = teamMemberRepository.countByTeamId(team.getId());
                    boolean isOwner = membership.getRole() == MemberRole.OWNER;
                    return buildTeamResponse(team, (int) memberCount, isOwner);
                })
                .collect(Collectors.toList());

        return responseBuilder.buildUserResponse(teams, HttpStatus.OK,
                teams.size() + " team(s) found");
    }

    /**
     * Gets all members of a team.
     * Emails are masked for non-owners (except their own email).
     *
     * @param teamId The team ID
     * @param user   The authenticated user
     * @return Response with list of members
     */
    @Transactional(readOnly = true)
    public UnravelDocsResponse<List<TeamMemberResponse>> getMembers(String teamId, User user) {
        log.debug(
                "Getting members for team {} by user {}",
                sanitizer.sanitizeLogging(teamId),
                sanitizer.sanitizeLogging(user.getId()));

        // 1. Get team
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new NotFoundException("Team not found"));

        // 2. Verify user is a member
        TeamMember requestingMember = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        // 3. Get all members
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);

        // 4. Determine if emails should be masked (only owner sees full emails)
        boolean isOwner = requestingMember.getRole() == MemberRole.OWNER;

        // 5. Build member responses
        List<TeamMemberResponse> memberResponses = members.stream()
                .map(member -> buildMemberResponse(member, user.getId(), isOwner))
                .collect(Collectors.toList());

        return responseBuilder.buildUserResponse(memberResponses, HttpStatus.OK,
                memberResponses.size() + " member(s) found");
    }

    // ========== Helper Methods ==========

    private TeamResponse buildTeamResponse(Team team, int memberCount, boolean isOwner) {
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
                .isOwner(isOwner)
                .build();
    }

    private TeamMemberResponse buildMemberResponse(TeamMember member, String requestingUserId, boolean isOwner) {
        String email = member.getUser().getEmail();

        // Don't mask if: owner is viewing, or viewing own email
        boolean shouldMask = !isOwner && !member.getUser().getId().equals(requestingUserId);
        if (shouldMask) {
            email = maskEmail(email);
        }

        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .email(email)
                .firstName(member.getUser().getFirstName())
                .lastName(member.getUser().getLastName())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }
}
