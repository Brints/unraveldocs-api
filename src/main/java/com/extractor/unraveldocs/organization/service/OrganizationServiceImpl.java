package com.extractor.unraveldocs.organization.service;

import com.extractor.unraveldocs.organization.impl.*;
import com.extractor.unraveldocs.organization.dto.request.*;
import com.extractor.unraveldocs.organization.dto.response.OrganizationMemberResponse;
import com.extractor.unraveldocs.organization.dto.response.OrganizationResponse;
import com.extractor.unraveldocs.team.dto.request.PromoteToAdminRequest;
import com.extractor.unraveldocs.organization.interfaces.OrganizationService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Main team service that delegates to specialized implementation
 * classes.
 */
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final InitiateOrgCreationImpl initiateOrgCreation;
    private final VerifyOtpAndCreateOrgImpl verifyOtpAndCreateOrg;
    private final MemberManagementImpl memberManagement;
    private final AdminPromotionImpl adminPromotion;
    private final InvitationServiceImpl invitationService;
    private final OrganizationViewImpl organizationView;
    private final CloseOrganizationImpl closeOrganization;

    // ============== Organization Creation Flow ==============

    @Override
    public UnravelDocsResponse<String> initiateOrganizationCreation(CreateOrganizationRequest request, String userId) {
        return initiateOrgCreation.initiateCreation(request, userId);
    }

    @Override
    public UnravelDocsResponse<OrganizationResponse> verifyOtpAndCreateOrganization(VerifyOrgOtpRequest request,
            String userId) {
        return verifyOtpAndCreateOrg.verifyAndCreate(request, userId);
    }

    // ============== Member Management ==============

    @Override
    public UnravelDocsResponse<OrganizationMemberResponse> addMember(String orgId, AddMemberRequest request,
            String adminId) {
        return memberManagement.addMember(orgId, request, adminId);
    }

    @Override
    public UnravelDocsResponse<Void> removeMember(String orgId, String memberId, String adminId) {
        return memberManagement.removeMember(orgId, memberId, adminId);
    }

    @Override
    public UnravelDocsResponse<Void> removeMembers(String orgId, RemoveMembersRequest request, String adminId) {
        return memberManagement.removeMembers(orgId, request, adminId);
    }

    // ============== Admin Promotion (Enterprise Only) ==============

    @Override
    public UnravelDocsResponse<OrganizationMemberResponse> promoteToAdmin(String orgId, PromoteToAdminRequest request,
            String ownerId) {
        return adminPromotion.promoteToAdmin(orgId, request, ownerId);
    }

    // ============== Invitation System (Enterprise Only) ==============

    @Override
    public UnravelDocsResponse<String> inviteMember(String orgId, InviteMemberRequest request, String adminId) {
        return invitationService.inviteMember(orgId, request, adminId);
    }

    @Override
    public UnravelDocsResponse<OrganizationMemberResponse> acceptInvitation(String token, String userId) {
        return invitationService.acceptInvitation(token, userId);
    }

    // ============== View Operations ==============

    @Override
    public UnravelDocsResponse<OrganizationResponse> getOrganization(String orgId, String userId) {
        return organizationView.getOrganization(orgId, userId);
    }

    @Override
    public UnravelDocsResponse<List<OrganizationMemberResponse>> getMembers(String orgId, String userId) {
        return organizationView.getMembers(orgId, userId);
    }

    @Override
    public UnravelDocsResponse<List<OrganizationResponse>> getMyOrganizations(String userId) {
        return organizationView.getMyOrganizations(userId);
    }

    // ============== Organization Lifecycle ==============

    @Override
    public UnravelDocsResponse<Void> closeOrganization(String orgId, String ownerId) {
        return closeOrganization.close(orgId, ownerId);
    }

    @Override
    public UnravelDocsResponse<OrganizationResponse> reactivateOrganization(String orgId, String ownerId) {
        return closeOrganization.reactivate(orgId, ownerId);
    }
}
