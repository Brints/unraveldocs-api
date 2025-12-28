package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.team.dto.response.OrganizationResponse;
import com.extractor.unraveldocs.team.model.Organization;
import com.extractor.unraveldocs.team.model.OrganizationMember;
import com.extractor.unraveldocs.team.repository.OrganizationMemberRepository;
import com.extractor.unraveldocs.team.repository.OrganizationRepository;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloseOrganizationImpl {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;

    @Transactional
    @CacheEvict(value = { "organizations", "org-members" }, key = "#orgId")
    public UnravelDocsResponse<Void> close(String orgId, String ownerId) {
        log.info("Closing organization {} by owner {}", orgId, ownerId);

        // 1. Get organization
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        // 2. Verify requester is the owner
        OrganizationMember owner = memberRepository.findByOrganizationIdAndUserId(orgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this organization"));

        if (!owner.isOwner()) {
            throw new ForbiddenException("Only the organization owner can close the organization");
        }

        if (org.isClosed()) {
            return new UnravelDocsResponse<>(
                    HttpStatus.OK.value(),
                    "Success",
                    "Organization is already closed",
                    null);
        }

        // 3. Close team (members remain but lose access)
        org.setClosed(true);
        org.setClosedAt(OffsetDateTime.now());
        org.setActive(false);
        organizationRepository.save(org);

        log.info("Organization {} closed successfully", orgId);

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "Organization closed successfully. Members cannot access it until reactivated.",
                null);
    }

    @Transactional
    @CacheEvict(value = { "organizations", "org-members" }, key = "#orgId")
    public UnravelDocsResponse<OrganizationResponse> reactivate(String orgId, String ownerId) {
        log.info("Reactivating team {} by owner {}", orgId, ownerId);

        // 1. Get team
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        // 2. Verify requester is the owner
        OrganizationMember owner = memberRepository.findByOrganizationIdAndUserId(orgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this organization"));

        if (!owner.isOwner()) {
            throw new ForbiddenException("Only the organization owner can reactivate the organization");
        }

        if (!org.isClosed()) {
            return new UnravelDocsResponse<>(
                    HttpStatus.OK.value(),
                    "Success",
                    "Organization is already active",
                    buildOrganizationResponse(org, ownerId));
        }

        // 3. Reactivate organization
        org.setClosed(false);
        org.setClosedAt(null);
        org.setActive(true);
        Organization savedOrg = organizationRepository.save(org);

        log.info("Organization {} reactivated successfully", orgId);

        int memberCount = (int) memberRepository.countByOrganizationId(orgId);
        OrganizationResponse response = buildOrganizationResponse(savedOrg, ownerId, memberCount);

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "Organization reactivated successfully",
                response);
    }

    private OrganizationResponse buildOrganizationResponse(Organization org, String viewerId) {
        int memberCount = (int) memberRepository.countByOrganizationId(org.getId());
        return buildOrganizationResponse(org, viewerId, memberCount);
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
                .createdByName(org.getCreatedBy().getFirstName() + " " + org.getCreatedBy().getLastName())
                .isOwner(isOwner)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .closedAt(org.getClosedAt())
                .build();
    }
}
