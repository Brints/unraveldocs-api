package com.extractor.unraveldocs.organization.impl;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import com.extractor.unraveldocs.organization.datamodel.InvitationStatus;
import com.extractor.unraveldocs.organization.datamodel.OrganizationMemberRole;
import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import com.extractor.unraveldocs.organization.dto.request.InviteMemberRequest;
import com.extractor.unraveldocs.organization.dto.response.OrganizationMemberResponse;
import com.extractor.unraveldocs.organization.model.Organization;
import com.extractor.unraveldocs.organization.model.OrganizationInvitation;
import com.extractor.unraveldocs.organization.model.OrganizationMember;
import com.extractor.unraveldocs.organization.repository.OrganizationInvitationRepository;
import com.extractor.unraveldocs.organization.repository.OrganizationMemberRepository;
import com.extractor.unraveldocs.organization.repository.OrganizationRepository;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final GenerateVerificationToken tokenGenerator;
    private final EmailOrchestratorService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final int INVITATION_EXPIRY_DAYS = 7;
    private static final int MAX_MEMBERS = 10;

    @Transactional
    public UnravelDocsResponse<String> inviteMember(String orgId, InviteMemberRequest request, String adminId) {
        log.info("Inviting {} to organization {} by {}", request.getEmail(), orgId, adminId);

        // 1. Get organization
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (org.isClosed()) {
            throw new ForbiddenException("Cannot invite members to a closed organization");
        }

        // 2. Verify this is an Enterprise organization
        if (org.getSubscriptionType() != OrganizationSubscriptionType.ENTERPRISE) {
            throw new ForbiddenException("Email invitations are only available for Enterprise organizations");
        }

        // 3. Verify admin has permission
        OrganizationMember admin = memberRepository.findByOrganizationIdAndUserId(orgId, adminId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this organization"));

        if (!admin.canManageMembers()) {
            throw new ForbiddenException("Only admins and owners can invite members");
        }

        // 4. Check member limit
        long currentCount = memberRepository.countByOrganizationId(orgId);
        if (currentCount >= MAX_MEMBERS) {
            throw new BadRequestException("Organization has reached the maximum member limit of " + MAX_MEMBERS);
        }

        // 5. Check if email already has pending invitation
        if (invitationRepository.existsByOrganizationIdAndEmailAndStatus(orgId, request.getEmail(),
                InvitationStatus.PENDING)) {
            throw new ConflictException("An invitation has already been sent to this email");
        }

        // 6. Check if user with this email is already a member
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingUser != null && memberRepository.existsByOrganizationIdAndUserId(orgId, existingUser.getId())) {
            throw new ConflictException("User with this email is already a member");
        }

        // 7. Generate invitation token
        String token = tokenGenerator.generateVerificationToken();

        // 8. Create invitation
        OrganizationInvitation invitation = new OrganizationInvitation();
        invitation.setOrganization(org);
        invitation.setEmail(request.getEmail());
        invitation.setInvitationToken(token);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(OffsetDateTime.now().plusDays(INVITATION_EXPIRY_DAYS));
        invitation.setInvitedById(adminId);
        invitationRepository.save(invitation);

        // 9. Send invitation email
        String invitationLink = baseUrl + "/api/v1/organizations/invitations/" + token + "/accept";
        sendInvitationEmail(request.getEmail(), org, admin.getUser(), invitationLink);

        log.info("Invitation sent to {} for organization {}", request.getEmail(), orgId);

        return new UnravelDocsResponse<>(
                HttpStatus.CREATED.value(),
                "Success",
                "Invitation sent successfully",
                invitationLink);
    }

    @Transactional
    @CacheEvict(value = { "organizations", "org-members" }, key = "#result.data != null ? #result.data.id : ''")
    public UnravelDocsResponse<OrganizationMemberResponse> acceptInvitation(String token, String userId) {
        log.info("Accepting invitation with token for user {}", userId);

        // 1. Find invitation
        OrganizationInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        // 2. Validate invitation
        if (!invitation.canBeAccepted()) {
            if (invitation.isExpired()) {
                invitation.markAsExpired();
                invitationRepository.save(invitation);
                throw new BadRequestException("This invitation has expired");
            }
            throw new BadRequestException("This invitation is no longer valid");
        }

        // 3. Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 4. Verify email matches
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ForbiddenException("This invitation was sent to a different email address");
        }

        // 5. Get organization
        Organization org = invitation.getOrganization();
        if (org.isClosed()) {
            throw new ForbiddenException("This organization is closed");
        }

        // 6. Check if already a member
        if (memberRepository.existsByOrganizationIdAndUserId(org.getId(), userId)) {
            invitation.markAsAccepted();
            invitationRepository.save(invitation);
            throw new ConflictException("You are already a member of this organization");
        }

        // 7. Check member limit
        long currentCount = memberRepository.countByOrganizationId(org.getId());
        if (currentCount >= MAX_MEMBERS) {
            throw new BadRequestException("Organization has reached the maximum member limit");
        }

        // 8. Mark invitation as accepted
        invitation.markAsAccepted();
        invitationRepository.save(invitation);

        // 9. Add user as member
        User invitedBy = userRepository.findById(invitation.getInvitedById()).orElse(null);
        OrganizationMember newMember = new OrganizationMember();
        newMember.setOrganization(org);
        newMember.setUser(user);
        newMember.setRole(OrganizationMemberRole.MEMBER);
        newMember.setInvitedBy(invitedBy);
        OrganizationMember savedMember = memberRepository.save(newMember);

        log.info("User {} accepted invitation and joined organization {}", userId, org.getId());

        OrganizationMemberResponse response = buildMemberResponse(savedMember);

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "You have successfully joined " + org.getName(),
                response);
    }

    private void sendInvitationEmail(String email, Organization org, User invitedBy, String invitationLink) {
        try {
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("organizationName", org.getName());
            templateModel.put("invitedByName", invitedBy.getFirstName() + " " + invitedBy.getLastName());
            templateModel.put("invitationLink", invitationLink);
            templateModel.put("expiryDays", INVITATION_EXPIRY_DAYS);

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(email)
                    .subject("You're Invited to Join " + org.getName() + " - UnravelDocs")
                    .templateName("organization-invitation")
                    .templateModel(templateModel)
                    .build();

            emailService.sendEmail(emailMessage);
            log.debug("Invitation email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", email, e.getMessage());
        }
    }

    private OrganizationMemberResponse buildMemberResponse(OrganizationMember member) {
        return OrganizationMemberResponse.builder()
                .id(member.getId())
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .firstName(member.getUser().getFirstName())
                .lastName(member.getUser().getLastName())
                .email(member.getUser().getEmail())
                .profilePicture(member.getUser().getProfilePicture())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .invitedByName(member.getInvitedBy() != null
                        ? member.getInvitedBy().getFirstName() + " " + member.getInvitedBy().getLastName()
                        : null)
                .build();
    }
}
