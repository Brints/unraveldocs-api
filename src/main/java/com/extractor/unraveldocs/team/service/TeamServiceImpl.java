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
 * Implementation of TeamService that delegates to specialized implementation
 * classes.
 * This service is completely independent from the Organization module.
 */
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final InitiateTeamCreationImpl initiateTeamCreation;
    private final VerifyOtpAndCreateTeamImpl verifyOtpAndCreateTeam;
    private final TeamMemberManagementImpl memberManagement;
    private final TeamAdminPromotionImpl adminPromotion;
    private final TeamInvitationServiceImpl invitationService;
    private final TeamViewImpl teamView;
    private final CloseTeamImpl closeTeam;
    private final CancelTeamSubscriptionImpl cancelTeamSubscription;

    // ========== Team Creation ==========

    @Override
    public UnravelDocsResponse<Void> initiateTeamCreation(CreateTeamRequest request, User user) {
        return initiateTeamCreation.initiateCreation(request, user);
    }

    @Override
    public UnravelDocsResponse<TeamResponse> verifyOtpAndCreateTeam(VerifyTeamOtpRequest request, User user) {
        return verifyOtpAndCreateTeam.verifyAndCreate(request, user);
    }

    // ========== Team View ==========

    @Override
    public UnravelDocsResponse<TeamResponse> getTeamById(String teamId, User user) {
        return teamView.getTeam(teamId, user);
    }

    @Override
    public UnravelDocsResponse<List<TeamResponse>> getMyTeams(User user) {
        return teamView.getMyTeams(user);
    }

    @Override
    public UnravelDocsResponse<List<TeamMemberResponse>> getTeamMembers(String teamId, User user) {
        return teamView.getMembers(teamId, user);
    }

    // ========== Member Management ==========

    @Override
    public UnravelDocsResponse<TeamMemberResponse> addMember(String teamId, AddTeamMemberRequest request, User user) {
        return memberManagement.addMember(teamId, request, user);
    }

    @Override
    public UnravelDocsResponse<Void> removeMember(String teamId, String memberId, User user) {
        return memberManagement.removeMember(teamId, memberId, user);
    }

    @Override
    public UnravelDocsResponse<Void> removeMembers(String teamId, RemoveTeamMembersRequest request, User user) {
        return memberManagement.removeMembers(teamId, request, user);
    }

    @Override
    public UnravelDocsResponse<TeamMemberResponse> promoteToAdmin(String teamId, String memberId, User user) {
        return adminPromotion.promoteToAdmin(teamId, memberId, user);
    }

    // ========== Invitations ==========

    @Override
    public UnravelDocsResponse<String> sendInvitation(String teamId, InviteTeamMemberRequest request, User user) {
        return invitationService.inviteMember(teamId, request, user);
    }

    @Override
    public UnravelDocsResponse<TeamMemberResponse> acceptInvitation(String token, User user) {
        return invitationService.acceptInvitation(token, user);
    }

    // ========== Subscription Management ==========

    @Override
    public UnravelDocsResponse<TeamResponse> cancelSubscription(String teamId, User user) {
        return cancelTeamSubscription.cancelSubscription(teamId, user);
    }

    // ========== Team Lifecycle ==========

    @Override
    public UnravelDocsResponse<Void> closeTeam(String teamId, User user) {
        return closeTeam.close(teamId, user);
    }

    @Override
    public UnravelDocsResponse<TeamResponse> reactivateTeam(String teamId, User user) {
        return closeTeam.reactivate(teamId, user);
    }
}
