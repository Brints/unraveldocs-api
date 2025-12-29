package com.extractor.unraveldocs.organization.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.organization.datamodel.OrganizationMemberRole;
import com.extractor.unraveldocs.organization.dto.request.AddMemberRequest;
import com.extractor.unraveldocs.organization.dto.request.RemoveMembersRequest;
import com.extractor.unraveldocs.organization.dto.response.OrganizationMemberResponse;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberManagementImpl {

    private static final int MAX_MEMBERS = 10;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SanitizeLogging sanitizer;

    @Transactional
    @CacheEvict(value = {"organizations", "org-members"}, key = "#orgId")
    public UnravelDocsResponse<OrganizationMemberResponse> addMember(String orgId, AddMemberRequest request,
                                                                     String adminId) {
        log.info("Adding member {} to team {} by {}", sanitizer.sanitizeLogging(request.getUserId()), sanitizer.sanitizeLogging(orgId), sanitizer.sanitizeLogging(adminId));

        // 1. Get team
        Organization org = getActiveOrganization(orgId);

        // 2. Verify admin has permission
        OrganizationMember admin = getMemberWithPermission(orgId, adminId, true);
        if (!admin.canManageMembers()) {
            throw new ForbiddenException("Only admins and owners can add members");
        }

        // 3. Check member limit
        long currentCount = memberRepository.countByOrganizationId(orgId);
        if (currentCount >= MAX_MEMBERS) {
            throw new BadRequestException("Organization has reached the maximum member limit of " + MAX_MEMBERS);
        }

        // 4. Check if user is already a member
        if (memberRepository.existsByOrganizationIdAndUserId(orgId, request.getUserId())) {
            throw new ConflictException("User is already a member of this team");
        }

        // 5. Get user to add
        User userToAdd = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!userToAdd.isActive() || !userToAdd.isVerified()) {
            throw new BadRequestException("User must be active and verified to join an team");
        }

        // 6. Create member
        OrganizationMember newMember = new OrganizationMember();
        newMember.setOrganization(org);
        newMember.setUser(userToAdd);
        newMember.setRole(OrganizationMemberRole.MEMBER);
        newMember.setInvitedBy(admin.getUser());
        OrganizationMember savedMember = memberRepository.save(newMember);

        log.info("Member {} added to team {} successfully", sanitizer.sanitizeLogging(request.getUserId()), sanitizer.sanitizeLogging(orgId));

        return new UnravelDocsResponse<>(
                HttpStatus.CREATED.value(),
                "Success",
                "Member added successfully",
                buildMemberResponse(savedMember, admin.getUser()));
    }

    @Transactional
    @CacheEvict(value = {"organizations", "org-members"}, key = "#orgId")
    public UnravelDocsResponse<Void> removeMember(String orgId, String memberId, String adminId) {
        log.info("Removing member {} from team {} by {}", sanitizer.sanitizeLogging(memberId), sanitizer.sanitizeLogging(orgId), sanitizer.sanitizeLogging(adminId));

        // 1. Verify team exists
        getActiveOrganization(orgId);

        // 2. Verify admin has permission
        OrganizationMember admin = getMemberWithPermission(orgId, adminId, true);
        if (!admin.canManageMembers()) {
            throw new ForbiddenException("Only admins and owners can remove members");
        }

        // 3. Get member to remove
        OrganizationMember memberToRemove = memberRepository.findByOrganizationIdAndUserId(orgId, memberId)
                .orElseThrow(() -> new NotFoundException("Member not found in this team"));

        // 4. Cannot remove yourself
        if (memberId.equals(adminId)) {
            throw new BadRequestException("You cannot remove yourself from the team");
        }

        // 5. Cannot remove owner
        if (memberToRemove.isOwner()) {
            throw new ForbiddenException("Cannot remove the team owner");
        }

        // 6. If removing an admin, update their isOrganizationAdmin flag
        if (memberToRemove.isAdmin()) {
            updateUserAdminStatus(memberToRemove.getUser());
        }

        // 7. Delete member
        memberRepository.delete(memberToRemove);

        log.info("Member {} removed from team {} successfully", sanitizer.sanitizeLogging(memberId), sanitizer.sanitizeLogging(orgId));

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "Member removed successfully",
                null);
    }

    @Transactional
    @CacheEvict(value = {"organizations", "org-members"}, key = "#orgId")
    public UnravelDocsResponse<Void> removeMembers(String orgId, RemoveMembersRequest request, String adminId) {
        log.info("Batch removing {} members from team {} by {}", sanitizer.sanitizeLoggingInteger(request.getUserIds().size()), sanitizer.sanitizeLogging(orgId), sanitizer.sanitizeLogging(adminId));

        // 1. Verify team exists
        getActiveOrganization(orgId);

        // 2. Verify admin has permission
        OrganizationMember admin = getMemberWithPermission(orgId, adminId, true);
        if (!admin.canManageMembers()) {
            throw new ForbiddenException("Only admins and owners can remove members");
        }

        // 3. Cannot remove self
        if (request.getUserIds().contains(adminId)) {
            throw new BadRequestException("You cannot remove yourself from the team");
        }

        // 4. Check for owner in the list
        OrganizationMember owner = memberRepository
                .findFirstByOrganizationIdAndRole(orgId, OrganizationMemberRole.OWNER)
                .orElse(null);
        if (owner != null && request.getUserIds().contains(owner.getUser().getId())) {
            throw new ForbiddenException("Cannot remove the team owner");
        }

        // 5. Update admin status for removed admins
        List<OrganizationMember> membersToRemove = memberRepository.findByOrganizationIdAndRoleIn(
                orgId, List.of(OrganizationMemberRole.ADMIN));
        for (OrganizationMember member : membersToRemove) {
            if (request.getUserIds().contains(member.getUser().getId())) {
                updateUserAdminStatus(member.getUser());
            }
        }

        // 6. Batch delete
        memberRepository.deleteByOrganizationIdAndUserIdIn(orgId, request.getUserIds());

        log.info(
                "{} members removed from team {} successfully",
                sanitizer.sanitizeLoggingInteger(request.getUserIds().size()),
                sanitizer.sanitizeLogging(orgId));

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "Members removed successfully",
                null);
    }

    private Organization getActiveOrganization(String orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (org.isClosed()) {
            throw new ForbiddenException("This team is closed");
        }

        if (!org.isActive()) {
            throw new ForbiddenException("This team is not active");
        }

        return org;
    }

    private OrganizationMember getMemberWithPermission(String orgId, String userId, boolean requireMember) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(orgId, userId).orElse(null);
        if (requireMember && member == null) {
            throw new ForbiddenException("You are not a member of this team");
        }
        return member;
    }

    private void updateUserAdminStatus(User user) {
        // Check if user is admin in any other team
        List<OrganizationMember> adminMemberships = memberRepository.findByUserId(user.getId())
                .stream()
                .filter(m -> m.getRole() == OrganizationMemberRole.ADMIN || m.getRole() == OrganizationMemberRole.OWNER)
                .toList();

        if (adminMemberships.isEmpty()) {
            user.setOrganizationAdmin(false);
            userRepository.save(user);
        }
    }

    private OrganizationMemberResponse buildMemberResponse(OrganizationMember member, User invitedBy) {
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
                .invitedByName(invitedBy != null ? invitedBy.getFirstName() + " " + invitedBy.getLastName() : null)
                .build();
    }
}
