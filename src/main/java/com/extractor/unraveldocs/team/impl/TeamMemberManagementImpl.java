package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.dto.request.AddTeamMemberRequest;
import com.extractor.unraveldocs.team.dto.request.RemoveTeamMembersRequest;
import com.extractor.unraveldocs.team.dto.response.TeamMemberResponse;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation for team member management operations.
 * Handles adding and removing members from a team.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamMemberManagementImpl {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ResponseBuilderService responseBuilder;
    private final SanitizeLogging sanitizer;

    /**
     * Adds a new member to the team.
     * Only the team owner can add members.
     *
     * @param teamId     The team ID
     * @param request    The add member request containing the user's email
     * @param actingUser The user performing the action
     * @return Response with the new member details
     */
    @Transactional
    public UnravelDocsResponse<TeamMemberResponse> addMember(String teamId, AddTeamMemberRequest request,
            User actingUser) {
        log.info("Adding member {} to team {} by user {}",
                sanitizer.sanitizeLogging(request.email()),
                sanitizer.sanitizeLogging(teamId),
                sanitizer.sanitizeLogging(actingUser.getId()));

        // 1. Get team and validate it's active
        Team team = getActiveTeam(teamId);

        // 2. Verify the acting user is the owner (only owner can add members)
        TeamMember actingMember = getTeamMember(teamId, actingUser.getId());
        if (actingMember.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("Only the team owner can add members");
        }

        // 3. Check team isn't full
        long currentMemberCount = teamMemberRepository.countByTeamId(teamId);
        if (currentMemberCount >= team.getMaxMembers()) {
            throw new BadRequestException("Team has reached the maximum member limit of " + team.getMaxMembers());
        }

        // 4. Find user to add by email
        User userToAdd = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("User with email " + request.email() + " not found"));

        // 5. Check user isn't already a member
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userToAdd.getId()).isPresent()) {
            throw new ConflictException("User is already a member of this team");
        }

        // 6. Create new member
        TeamMember newMember = new TeamMember();
        newMember.setTeam(team);
        newMember.setUser(userToAdd);
        newMember.setRole(MemberRole.MEMBER);
        newMember.setInvitedBy(actingUser);
        teamMemberRepository.save(newMember);

        log.info(
                "Successfully added user {} to team {}",
                sanitizer.sanitizeLogging(userToAdd.getId()),
                sanitizer.sanitizeLogging(teamId));

        TeamMemberResponse response = buildMemberResponse(newMember, false);
        return responseBuilder.buildUserResponse(
                response, HttpStatus.CREATED, "Member added successfully");
    }

    /**
     * Removes a member from the team.
     * Owner can remove anyone. Admin can remove non-owner members.
     *
     * @param teamId     The team ID
     * @param memberId   The member ID to remove
     * @param actingUser The user performing the action
     * @return Response indicating success
     */
    @Transactional
    public UnravelDocsResponse<Void> removeMember(String teamId, String memberId, User actingUser) {
        log.info("Removing member {} from team {} by user {}",
                sanitizer.sanitizeLogging(memberId), sanitizer.sanitizeLogging(teamId),
                sanitizer.sanitizeLogging(actingUser.getId()));

        // 1. Validate team exists and is active
        getActiveTeam(teamId);

        // 2. Verify the acting user is owner or admin
        TeamMember actingMember = getTeamMember(teamId, actingUser.getId());
        if (!actingMember.canManageMembers()) {
            throw new ForbiddenException("You don't have permission to remove members");
        }

        // 3. Get member to remove
        TeamMember memberToRemove = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Team member not found"));

        // 4. Validate member belongs to this team
        if (!memberToRemove.getTeam().getId().equals(teamId)) {
            throw new BadRequestException("Member does not belong to this team");
        }

        // 5. Cannot remove owner
        if (memberToRemove.getRole() == MemberRole.OWNER) {
            throw new ForbiddenException("Cannot remove the team owner");
        }

        // 6. Admins cannot remove other admins (only owner can)
        if (actingMember.getRole() == MemberRole.ADMIN && memberToRemove.getRole() == MemberRole.ADMIN) {
            throw new ForbiddenException("Only the team owner can remove admins");
        }

        // 7. Cannot remove yourself
        if (memberToRemove.getUser().getId().equals(actingUser.getId())) {
            throw new BadRequestException("You cannot remove yourself. Use 'Leave Team' instead.");
        }

        // 8. Remove member
        teamMemberRepository.delete(memberToRemove);

        log.info("Successfully removed member {} from team {}", memberId, teamId);

        return responseBuilder.buildVoidResponse(HttpStatus.OK, "Member removed successfully");
    }

    /**
     * Removes multiple members from the team in batch.
     * Owner can remove anyone. Admin can remove non-owner/non-admin members.
     *
     * @param teamId     The team ID
     * @param request    The batch remove request containing member IDs
     * @param actingUser The user performing the action
     * @return Response indicating success
     */
    @Transactional
    public UnravelDocsResponse<Void> removeMembers(String teamId, RemoveTeamMembersRequest request, User actingUser) {
        log.info(
                "Batch removing {} members from team {} by user {}",
                sanitizer.sanitizeLoggingInteger(request.memberIds().size()),
                sanitizer.sanitizeLogging(teamId),
                sanitizer.sanitizeLogging(actingUser.getId()));

        if (request.memberIds().isEmpty()) {
            throw new BadRequestException("Member IDs list cannot be empty");
        }

        // 1. Validate team exists and is active
        getActiveTeam(teamId);

        // 2. Verify the acting user is owner or admin
        TeamMember actingMember = getTeamMember(teamId, actingUser.getId());
        if (!actingMember.canManageMembers()) {
            throw new ForbiddenException("You don't have permission to remove members");
        }

        // 3. Get all members to remove
        List<TeamMember> membersToRemove = teamMemberRepository.findAllById(request.memberIds());

        int removedCount = 0;
        for (TeamMember member : membersToRemove) {
            // Validate member belongs to this team
            if (!member.getTeam().getId().equals(teamId)) {
                continue;
            }
            // Cannot remove owner
            if (member.getRole() == MemberRole.OWNER) {
                continue;
            }
            // Admins cannot remove other admins
            if (actingMember.getRole() == MemberRole.ADMIN && member.getRole() == MemberRole.ADMIN) {
                continue;
            }
            // Cannot remove yourself
            if (member.getUser().getId().equals(actingUser.getId())) {
                continue;
            }

            teamMemberRepository.delete(member);
            removedCount++;
        }

        log.info("Successfully removed {} members from team {}", sanitizer.sanitizeLoggingInteger(removedCount),
                sanitizer.sanitizeLogging(teamId));

        return responseBuilder.buildVoidResponse(HttpStatus.OK,
                removedCount + " member(s) removed successfully");
    }

    // ========== Helper Methods ==========

    private Team getActiveTeam(String teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        if (team.isClosed()) {
            throw new ForbiddenException("This team has been closed");
        }

        if (!team.isAccessAllowed()) {
            throw new ForbiddenException("Team subscription is not active");
        }

        return team;
    }

    private TeamMember getTeamMember(String teamId, String userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));
    }

    private TeamMemberResponse buildMemberResponse(TeamMember member, boolean maskEmail) {
        String email = member.getUser().getEmail();
        if (maskEmail) {
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
