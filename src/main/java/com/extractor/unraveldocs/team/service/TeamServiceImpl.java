package com.extractor.unraveldocs.team.service;

import com.extractor.unraveldocs.team.dto.request.*;
import com.extractor.unraveldocs.team.dto.response.*;
import com.extractor.unraveldocs.team.impl.*;
import com.extractor.unraveldocs.team.interfaces.TeamService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of TeamService that delegates to specialized implementation classes.
 */
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final InitiateOrgCreationImpl initiateOrgCreation;
    private final VerifyOtpAndCreateOrgImpl verifyOtpAndCreateOrg;
    private final MemberManagementImpl memberManagement;
    private final AdminPromotionImpl adminPromotion;
    private final InvitationServiceImpl invitationService;
    private final OrganizationViewImpl organizationView;
    private final CloseOrganizationImpl closeOrganization;
    private final CancelTeamSubscriptionImpl cancelTeamSubscription;

    // ========== Team Creation ==========

    @Override
    public UnravelDocsResponse<Void> initiateTeamCreation(CreateTeamRequest request, User user) {
        CreateOrganizationRequest orgRequest = CreateOrganizationRequest.builder()
                .name(request.getName())
                .subscriptionType(request.getSubscriptionType())
                .billingCycle(request.getBillingCycle())
                .currency(request.getCurrency())
                .build();
        UnravelDocsResponse<String> response = initiateOrgCreation.initiateCreation(orgRequest, user.getId());
        return UnravelDocsResponse.<Void>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .build();
    }

    @Override
    public UnravelDocsResponse<TeamResponse> verifyOtpAndCreateTeam(VerifyTeamOtpRequest request, User user) {
        VerifyOrgOtpRequest orgRequest = VerifyOrgOtpRequest.builder()
                .otp(request.getOtp())
                .build();
        UnravelDocsResponse<OrganizationResponse> response = verifyOtpAndCreateOrg.verifyAndCreate(orgRequest, user.getId());
        return mapToTeamResponse(response);
    }

    // ========== Team View ==========

    @Override
    public UnravelDocsResponse<TeamResponse> getTeamById(String teamId, User user) {
        UnravelDocsResponse<OrganizationResponse> response = organizationView.getOrganization(teamId, user.getId());
        return mapToTeamResponse(response);
    }

    @Override
    public UnravelDocsResponse<List<TeamResponse>> getMyTeams(User user) {
        UnravelDocsResponse<List<OrganizationResponse>> response = organizationView.getMyOrganizations(user.getId());
        if (response.getData() == null) {
            return UnravelDocsResponse.<List<TeamResponse>>builder()
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .build();
        }
        List<TeamResponse> teams = response.getData().stream()
                .map(this::mapOrgToTeamResponse)
                .toList();
        return UnravelDocsResponse.<List<TeamResponse>>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .data(teams)
                .build();
    }

    @Override
    public UnravelDocsResponse<List<TeamMemberResponse>> getTeamMembers(String teamId, User user) {
        UnravelDocsResponse<List<OrganizationMemberResponse>> response = organizationView.getMembers(teamId, user.getId());
        if (response.getData() == null) {
            return UnravelDocsResponse.<List<TeamMemberResponse>>builder()
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .build();
        }
        List<TeamMemberResponse> members = response.getData().stream()
                .map(this::mapOrgMemberToTeamMember)
                .toList();
        return UnravelDocsResponse.<List<TeamMemberResponse>>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .data(members)
                .build();
    }

    // ========== Member Management ==========

    @Override
    public UnravelDocsResponse<TeamMemberResponse> addMember(String teamId, AddTeamMemberRequest request, User user) {
        AddMemberRequest orgRequest = AddMemberRequest.builder()
                .email(request.getEmail())
                .build();
        UnravelDocsResponse<OrganizationMemberResponse> response = memberManagement.addMember(teamId, orgRequest, user.getId());
        return mapToTeamMemberResponse(response);
    }

    @Override
    public UnravelDocsResponse<Void> removeMember(String teamId, String memberId, User user) {
        return memberManagement.removeMember(teamId, memberId, user.getId());
    }

    @Override
    public UnravelDocsResponse<Void> removeMembers(String teamId, RemoveTeamMembersRequest request, User user) {
        RemoveMembersRequest orgRequest = RemoveMembersRequest.builder()
                .memberIds(request.getMemberIds())
                .build();
        return memberManagement.removeMembers(teamId, orgRequest, user.getId());
    }

    @Override
    public UnravelDocsResponse<TeamMemberResponse> promoteToAdmin(String teamId, String memberId, User user) {
        PromoteToAdminRequest orgRequest = PromoteToAdminRequest.builder()
                .userId(memberId)
                .build();
        UnravelDocsResponse<OrganizationMemberResponse> response = adminPromotion.promoteToAdmin(teamId, orgRequest, user.getId());
        return mapToTeamMemberResponse(response);
    }

    // ========== Invitations ==========

    @Override
    public UnravelDocsResponse<String> sendInvitation(String teamId, InviteTeamMemberRequest request, User user) {
        InviteMemberRequest orgRequest = InviteMemberRequest.builder()
                .email(request.getEmail())
                .build();
        return invitationService.inviteMember(teamId, orgRequest, user.getId());
    }

    @Override
    public UnravelDocsResponse<TeamMemberResponse> acceptInvitation(String token, User user) {
        UnravelDocsResponse<OrganizationMemberResponse> response = invitationService.acceptInvitation(token, user.getId());
        return mapToTeamMemberResponse(response);
    }

    // ========== Subscription Management ==========

    @Override
    public UnravelDocsResponse<TeamResponse> cancelSubscription(String teamId, User user) {
        UnravelDocsResponse<OrganizationResponse> response = cancelTeamSubscription.cancel(teamId, user.getId());
        return mapToTeamResponse(response);
    }

    // ========== Team Lifecycle ==========

    @Override
    public UnravelDocsResponse<Void> closeTeam(String teamId, User user) {
        return closeOrganization.close(teamId, user.getId());
    }

    @Override
    public UnravelDocsResponse<TeamResponse> reactivateTeam(String teamId, User user) {
        UnravelDocsResponse<OrganizationResponse> response = closeOrganization.reactivate(teamId, user.getId());
        return mapToTeamResponse(response);
    }

    // ========== Helper Methods ==========

    private UnravelDocsResponse<TeamResponse> mapToTeamResponse(UnravelDocsResponse<OrganizationResponse> response) {
        if (response.getData() == null) {
            return UnravelDocsResponse.<TeamResponse>builder()
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .build();
        }
        return UnravelDocsResponse.<TeamResponse>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .data(mapOrgToTeamResponse(response.getData()))
                .build();
    }

    private TeamResponse mapOrgToTeamResponse(OrganizationResponse org) {
        return TeamResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .description(org.getDescription())
                .teamCode(org.getOrgCode())
                .subscriptionType(org.getSubscriptionType() != null ? org.getSubscriptionType().name() : null)
                .isActive(org.isActive())
                .isVerified(org.isVerified())
                .isClosed(org.isClosed())
                .currentMemberCount(org.getCurrentMemberCount() != null ? org.getCurrentMemberCount() : 0)
                .maxMembers(org.getMaxMembers() != null ? org.getMaxMembers() : 0)
                .monthlyDocumentLimit(org.getMonthlyDocumentLimit())
                .trialEndsAt(org.getTrialEndsAt())
                .createdAt(org.getCreatedAt())
                .isOwner(org.isOwner())
                .build();
    }

    private UnravelDocsResponse<TeamMemberResponse> mapToTeamMemberResponse(UnravelDocsResponse<OrganizationMemberResponse> response) {
        if (response.getData() == null) {
            return UnravelDocsResponse.<TeamMemberResponse>builder()
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .build();
        }
        return UnravelDocsResponse.<TeamMemberResponse>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .data(mapOrgMemberToTeamMember(response.getData()))
                .build();
    }

    private TeamMemberResponse mapOrgMemberToTeamMember(OrganizationMemberResponse member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .email(member.getEmail())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .role(member.getRole() != null ? member.getRole().name() : null)
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
