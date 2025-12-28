package com.extractor.unraveldocs.organization.impl;

import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.organization.dto.response.OrganizationMemberResponse;
import com.extractor.unraveldocs.organization.dto.response.OrganizationResponse;
import com.extractor.unraveldocs.organization.model.Organization;
import com.extractor.unraveldocs.organization.model.OrganizationMember;
import com.extractor.unraveldocs.organization.repository.OrganizationMemberRepository;
import com.extractor.unraveldocs.organization.repository.OrganizationRepository;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationViewImpl {

        private final OrganizationRepository organizationRepository;
        private final OrganizationMemberRepository memberRepository;

        @Transactional(readOnly = true)
        @Cacheable(value = "organizations", key = "#orgId")
        public UnravelDocsResponse<OrganizationResponse> getOrganization(String orgId, String userId) {
                log.debug("Getting organization {} for user {}", orgId, userId);

                // 1. Get organization
                Organization org = organizationRepository.findById(orgId)
                                .orElseThrow(() -> new NotFoundException("Organization not found"));

                // 2. Verify user is a member (variable used for access check)
                verifyMembership(orgId, userId);

                // 3. Get member count
                long memberCount = memberRepository.countByOrganizationId(orgId);

                OrganizationResponse response = buildOrganizationResponse(org, userId, (int) memberCount);

                return new UnravelDocsResponse<>(
                                HttpStatus.OK.value(),
                                "Success",
                                "Organization details retrieved successfully",
                                response);
        }

        @Transactional(readOnly = true)
        @Cacheable(value = "org-members", key = "#orgId")
        public UnravelDocsResponse<List<OrganizationMemberResponse>> getMembers(String orgId, String userId) {
                log.debug("Getting members of organization {} for user {}", orgId, userId);

                // 1. Verify organization exists
                verifyOrganizationExists(orgId);

                // 2. Verify user is a member and check if owner
                OrganizationMember viewerMember = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                                .orElseThrow(() -> new ForbiddenException("You are not a member of this organization"));

                // 3. Check if viewer is the owner (can see all emails)
                boolean isOwner = viewerMember.isOwner();

                // 4. Get all members
                List<OrganizationMember> members = memberRepository.findByOrganizationId(orgId);

                // 5. Build response with email masking
                List<OrganizationMemberResponse> responses = members.stream()
                                .map(member -> buildMemberResponse(member, userId, isOwner))
                                .toList();

                return new UnravelDocsResponse<>(
                                HttpStatus.OK.value(),
                                "Success",
                                "Members retrieved successfully",
                                responses);
        }

        @Transactional(readOnly = true)
        public UnravelDocsResponse<List<OrganizationResponse>> getMyOrganizations(String userId) {
                log.debug("Getting organizations for user {}", userId);

                // Get all organizations the user belongs to
                List<Organization> organizations = organizationRepository.findAllOrganizationsForUser(userId);

                List<OrganizationResponse> responses = organizations.stream()
                                .map(org -> {
                                        int memberCount = (int) memberRepository.countByOrganizationId(org.getId());
                                        return buildOrganizationResponse(org, userId, memberCount);
                                })
                                .toList();

                return new UnravelDocsResponse<>(
                                HttpStatus.OK.value(),
                                "Success",
                                "Organizations retrieved successfully",
                                responses);
        }

        private OrganizationResponse buildOrganizationResponse(Organization org, String viewerId, int memberCount) {
                boolean isOwner = org.getCreatedBy().getId().equals(viewerId);
                return OrganizationResponse.builder()
                                .id(org.getId())
                                .name(org.getName())
                                .description(org.getDescription())
                                .orgCode(org.getOrgCode())
                                .subscriptionType(org.getSubscriptionType())
                                .paymentGateway(org.getPaymentGateway())
                                .inTrial(org.isInTrial())
                                .trialEndsAt(org.getTrialEndsAt())
                                .active(org.isActive())
                                .verified(org.isVerified())
                                .closed(org.isClosed())
                                .maxMembers(org.getMaxMembers())
                                .currentMemberCount(memberCount)
                                .monthlyDocumentLimit(org.getMonthlyDocumentLimit())
                                .monthlyDocumentUploadCount(org.getMonthlyDocumentUploadCount())
                                .createdById(org.getCreatedBy().getId())
                                .createdByName(org.getCreatedBy().getFirstName() + " "
                                                + org.getCreatedBy().getLastName())
                                .isOwner(isOwner)
                                .createdAt(org.getCreatedAt())
                                .updatedAt(org.getUpdatedAt())
                                .closedAt(org.getClosedAt())
                                .build();
        }

        private void verifyOrganizationExists(String orgId) {
                if (!organizationRepository.existsById(orgId)) {
                        throw new NotFoundException("Organization not found");
                }
        }

        private void verifyMembership(String orgId, String userId) {
                if (!memberRepository.existsByOrganizationIdAndUserId(orgId, userId)) {
                        throw new ForbiddenException("You are not a member of this organization");
                }
        }

        private OrganizationMemberResponse buildMemberResponse(OrganizationMember member, String viewerId,
                        boolean isOwner) {
                boolean isSelf = member.getUser().getId().equals(viewerId);
                boolean canViewFullEmail = isOwner || isSelf;

                OrganizationMemberResponse response = OrganizationMemberResponse.builder()
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
                                                ? member.getInvitedBy().getFirstName() + " "
                                                                + member.getInvitedBy().getLastName()
                                                : null)
                                .build();

                // Apply email masking if viewer is not owner and not viewing self
                return response.withMaskedEmail(canViewFullEmail);
        }
}
