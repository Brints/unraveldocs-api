package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import com.extractor.unraveldocs.team.dto.response.TeamMemberResponse;
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

/**
 * Implementation for promoting team members to admin role.
 * Only the team owner can promote members.
 * Only available for Enterprise subscription.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamAdminPromotionImpl {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ResponseBuilderService responseBuilder;
    private final SanitizeLogging sanitizer;

    /**
     * Promotes a member to admin role.
     * Only the team owner can perform this action.
     * Only available for Enterprise subscription plans.
     *
     * @param teamId     The team ID
     * @param memberId   The ID of the member to promote
     * @param actingUser The user performing the action (must be owner)
     * @return Response with the updated member details
     */
    @Transactional
    public UnravelDocsResponse<TeamMemberResponse> promoteToAdmin(String teamId, String memberId, User actingUser) {
        log.info("Promoting member {} to admin in team {} by user {}",
                sanitizer.sanitizeLogging(memberId), sanitizer.sanitizeLogging(teamId), sanitizer.sanitizeLogging(actingUser.getId()));

        // 1. Get team and validate it's active
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        if (team.isClosed()) {
            throw new ForbiddenException("This team has been closed");
        }

        if (!team.isAccessAllowed()) {
            throw new ForbiddenException("Team subscription is not active");
        }

        // 2. Verify this is an Enterprise team (admin promotion is Enterprise-only)
        if (team.getSubscriptionType() != TeamSubscriptionType.TEAM_ENTERPRISE) {
            throw new ForbiddenException("Admin promotion is only available for Enterprise teams. " +
                    "Please upgrade your subscription to use this feature.");
        }

        // 3. Verify the acting user is the owner
        TeamMember actingMember = teamMemberRepository.findByTeamIdAndUserId(teamId, actingUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (actingMember.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Only the team owner can promote members to admin");
        }

        // 4. Get the member to promote
        TeamMember memberToPromote = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Team member not found"));

        // 5. Validate member belongs to this team
        if (!memberToPromote.getTeam().getId().equals(teamId)) {
            throw new BadRequestException("Member does not belong to this team");
        }

        // 6. Cannot promote yourself (owner is already highest role)
        if (memberToPromote.getUser().getId().equals(actingUser.getId())) {
            throw new BadRequestException("You are already the team owner");
        }

        // 7. Check if already an admin
        if (memberToPromote.getRole() == MemberRole.ADMIN) {
            throw new BadRequestException("This member is already an admin");
        }

        // 8. Cannot promote owner (shouldn't happen but safety check)
        if (memberToPromote.getRole() == MemberRole.OWNER) {
            throw new BadRequestException("Cannot promote the team owner");
        }

        // 9. Promote to admin
        memberToPromote.setRole(MemberRole.ADMIN);
        teamMemberRepository.save(memberToPromote);

        log.info("Successfully promoted member {} to admin in team {}", sanitizer.sanitizeLogging(memberId), sanitizer.sanitizeLogging(teamId));

        TeamMemberResponse response = buildMemberResponse(memberToPromote);
        return responseBuilder.buildUserResponse(response, HttpStatus.OK,
                memberToPromote.getUser().getFirstName() + " has been promoted to Admin");
    }

    private TeamMemberResponse buildMemberResponse(TeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .firstName(member.getUser().getFirstName())
                .lastName(member.getUser().getLastName())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
