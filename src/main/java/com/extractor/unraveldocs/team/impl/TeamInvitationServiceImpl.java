package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import com.extractor.unraveldocs.shared.datamodel.InvitationStatus;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import com.extractor.unraveldocs.team.dto.request.InviteTeamMemberRequest;
import com.extractor.unraveldocs.team.dto.response.TeamMemberResponse;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamInvitation;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.repository.TeamInvitationRepository;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for team invitation operations.
 * Handles sending and accepting team invitations.
 * Only available for Enterprise subscription.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamInvitationServiceImpl {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final GenerateVerificationToken tokenGenerator;
    private final EmailOrchestratorService emailService;
    private final ResponseBuilderService responseBuilder;
    private final SanitizeLogging sanitizer;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    private static final int TOKEN_LENGTH = 32;
    private static final int INVITATION_EXPIRY_DAYS = 7;

    /**
     * Sends an invitation email to join the team.
     * Only the owner or admin can send invitations.
     * Only available for Enterprise subscription.
     *
     * @param teamId     The team ID
     * @param request    The invitation request containing the email
     * @param actingUser The user sending the invitation
     * @return Response with the invitation token
     */
    @Transactional
    public UnravelDocsResponse<String> inviteMember(String teamId, InviteTeamMemberRequest request, User actingUser) {
        log.info(
                "Sending invitation to {} for team {} by user {}",
                sanitizer.sanitizeLogging(request.email()),
                sanitizer.sanitizeLogging(teamId),
                sanitizer.sanitizeLogging(actingUser.getId()));

        // 1. Get team and validate
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));

        if (team.isClosed()) {
            throw new ForbiddenException("This team has been closed");
        }

        if (!team.isAccessAllowed()) {
            throw new ForbiddenException("Team subscription is not active");
        }

        // 2. Verify this is an Enterprise team (invitations are Enterprise-only)
        if (team.getSubscriptionType() != TeamSubscriptionType.TEAM_ENTERPRISE) {
            throw new ForbiddenException("Email invitations are only available for Enterprise teams. " +
                    "Premium teams can manually add members using their email.");
        }

        // 3. Verify the acting user is owner or admin
        TeamMember actingMember = teamMemberRepository.findByTeamIdAndUserId(teamId, actingUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (!actingMember.canManageMembers()) {
            throw new ForbiddenException("Only team owner or admins can send invitations");
        }

        // 4. Check team isn't full
        long currentMemberCount = teamMemberRepository.countByTeamId(teamId);
        if (currentMemberCount >= team.getMaxMembers()) {
            throw new BadRequestException("Team has reached the maximum member limit of " + team.getMaxMembers());
        }

        // 5. Check if user is already a member
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            if (teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId()).isPresent()) {
                throw new ConflictException("This user is already a member of the team");
            }
        });

        // 6. Check if there's already a pending invitation
        invitationRepository.findPendingByTeamIdAndEmail(teamId, request.email(), OffsetDateTime.now())
                .ifPresent(invitation -> {
                    throw new ConflictException("An invitation has already been sent to this email. " +
                            "It expires on " + invitation.getExpiresAt().toLocalDate());
                });

        // 7. Generate invitation token
        String token = tokenGenerator.generateToken(TOKEN_LENGTH);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(INVITATION_EXPIRY_DAYS);

        // 8. Create invitation record
        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeam(team);
        invitation.setEmail(request.email());
        invitation.setInvitationToken(token);
        invitation.setExpiresAt(expiresAt);
        invitation.setInvitedById(actingUser.getId());
        invitationRepository.save(invitation);

        // 9. Send invitation email
        sendInvitationEmail(request.email(), team, actingUser, token);

        log.info(
                "Invitation sent successfully to {} for team {}",
                sanitizer.sanitizeLogging(request.email()),
                sanitizer.sanitizeLogging(teamId));

        String inviteLink = baseUrl + "/teams/invite/" + token;
        return responseBuilder.buildUserResponse(inviteLink, HttpStatus.CREATED,
                "Invitation sent to " + request.email());
    }

    /**
     * Accepts a team invitation.
     *
     * @param token The invitation token
     * @param user  The user accepting the invitation
     * @return Response with the member details
     */
    @Transactional
    public UnravelDocsResponse<TeamMemberResponse> acceptInvitation(String token, User user) {
        log.info(
                "Accepting invitation with token for user {}",
                sanitizer.sanitizeLogging(user.getId()));

        // 1. Find the invitation
        TeamInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new NotFoundException("Invalid invitation token"));

        // 2. Validate invitation status
        if (invitation.getStatus().equals(InvitationStatus.ACCEPTED)) {
            throw new BadRequestException("This invitation has already been used");
        }

        if (invitation.isExpired()) {
            throw new BadRequestException("This invitation has expired");
        }

        // 3. Validate email matches (case insensitive)
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ForbiddenException("This invitation was sent to a different email address");
        }

        Team team = invitation.getTeam();

        // 4. Check team is still active
        if (team.isClosed()) {
            throw new ForbiddenException("This team has been closed");
        }

        if (!team.isAccessAllowed()) {
            throw new ForbiddenException("The team's subscription is not active");
        }

        // 5. Check user isn't already a member
        if (teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId()).isPresent()) {
            throw new ConflictException("You are already a member of this team");
        }

        // 6. Check team isn't full
        long currentMemberCount = teamMemberRepository.countByTeamId(team.getId());
        if (currentMemberCount >= team.getMaxMembers()) {
            throw new BadRequestException("This team has reached the maximum member limit");
        }

        // 7. Mark invitation as used
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        // 8. Add user as member
        User invitedBy = userRepository.findById(invitation.getInvitedById()).orElse(null);

        TeamMember newMember = new TeamMember();
        newMember.setTeam(team);
        newMember.setUser(user);
        newMember.setRole(MemberRole.MEMBER);
        newMember.setInvitedBy(invitedBy);
        teamMemberRepository.save(newMember);

        log.info(
                "User {} joined team {} via invitation",
                sanitizer.sanitizeLogging(user.getId()),
                sanitizer.sanitizeLogging(team.getId()));

        TeamMemberResponse response = TeamMemberResponse.builder()
                .id(newMember.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(MemberRole.MEMBER.name())
                .joinedAt(newMember.getJoinedAt())
                .build();

        return responseBuilder.buildUserResponse(response, HttpStatus.OK,
                "Welcome to " + team.getName() + "!");
    }

    private void sendInvitationEmail(String toEmail, Team team, User inviter, String token) {
        try {
            String inviteLink = baseUrl + "/teams/invite/" + token;

            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("teamName", team.getName());
            templateModel.put("inviterName", inviter.getFirstName() + " " + inviter.getLastName());
            templateModel.put("inviteLink", inviteLink);
            templateModel.put("expiryDays", INVITATION_EXPIRY_DAYS);

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(toEmail)
                    .subject("You've Been Invited to Join " + team.getName() + " - UnravelDocs")
                    .templateName("team-invitation")
                    .templateModel(templateModel)
                    .build();

            emailService.sendEmail(emailMessage);
            log.debug(
                    "Invitation email sent to: {}",
                    sanitizer.sanitizeLogging(toEmail));
        } catch (Exception e) {
            log.error(
                    "Failed to send invitation email to: {}",
                    sanitizer.sanitizeLogging(toEmail), e);
            // Don't throw - invitation is saved, can be resent
        }
    }
}
