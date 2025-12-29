package com.extractor.unraveldocs.organization.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.organization.datamodel.OrganizationMemberRole;
import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import com.extractor.unraveldocs.organization.dto.response.OrganizationMemberResponse;
import com.extractor.unraveldocs.team.dto.request.PromoteToAdminRequest;
import com.extractor.unraveldocs.organization.model.Organization;
import com.extractor.unraveldocs.organization.model.OrganizationMember;
import com.extractor.unraveldocs.organization.repository.OrganizationMemberRepository;
import com.extractor.unraveldocs.organization.repository.OrganizationRepository;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPromotionImpl {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private SanitizeLogging sanitizer;

    @Transactional
    @CacheEvict(value = { "organizations", "org-members" }, key = "#orgId")
    public UnravelDocsResponse<OrganizationMemberResponse> promoteToAdmin(String orgId, PromoteToAdminRequest request,
                                                                          String ownerId) {
        log.info("Promoting member {} to admin in team {} by owner {}", sanitizer.sanitizeLogging(request.getUserId()), sanitizer.sanitizeLogging(orgId), sanitizer.sanitizeLogging(ownerId));

        // 1. Get team
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (org.isClosed()) {
            throw new ForbiddenException("Cannot promote members in a closed team");
        }

        if (!OrganizationSubscriptionType.ENTERPRISE.equals(org.getSubscriptionType())) {
            throw new ForbiddenException("Admin promotions are only available for enterprise teams");
        }

        // 3. Verify requester is the owner
        OrganizationMember owner = memberRepository.findByOrganizationIdAndUserId(orgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));

        if (!owner.isOwner()) {
            throw new ForbiddenException("Only the team owner can promote members to admin");
        }

        // 4. Get member to promote
        OrganizationMember memberToPromote = memberRepository.findByOrganizationIdAndUserId(orgId, request.getUserId())
                .orElseThrow(() -> new NotFoundException("Member not found in this team"));

        // 5. Cannot promote self or another owner
        if (memberToPromote.isOwner()) {
            throw new BadRequestException("Cannot change the owner's role");
        }

        if (memberToPromote.isAdmin()) {
            throw new BadRequestException("Member is already an admin");
        }

        // 6. Promote to admin
        memberToPromote.setRole(OrganizationMemberRole.ADMIN);
        OrganizationMember updatedMember = memberRepository.save(memberToPromote);

        // 7. Update user's isOrganizationAdmin flag
        User user = memberToPromote.getUser();
        user.setOrganizationAdmin(true);
        userRepository.save(user);

        log.info("Member {} promoted to admin in team {} successfully", sanitizer.sanitizeLogging(request.getUserId()), sanitizer.sanitizeLogging(orgId));

        OrganizationMemberResponse response = buildMemberResponse(updatedMember);

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "Member promoted to admin successfully",
                response);
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
