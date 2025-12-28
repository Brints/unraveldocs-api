package com.extractor.unraveldocs.team.interfaces;

import com.extractor.unraveldocs.team.dto.request.*;
import com.extractor.unraveldocs.team.dto.response.*;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;

import java.util.List;

/**
 * Service interface for team operations.
 */
public interface TeamService {

    // ========== Team Creation ==========

    /**
     * Initiate team creation by sending OTP to user's email.
     */
    UnravelDocsResponse<Void> initiateTeamCreation(CreateTeamRequest request, User user);

    /**
     * Verify OTP and create the team.
     */
    UnravelDocsResponse<TeamResponse> verifyOtpAndCreateTeam(VerifyTeamOtpRequest request, User user);

    // ========== Team View ==========

    /**
     * Get team details by ID.
     */
    UnravelDocsResponse<TeamResponse> getTeamById(String teamId, User user);

    /**
     * Get all teams the user belongs to.
     */
    UnravelDocsResponse<List<TeamResponse>> getMyTeams(User user);

    /**
     * Get all members of a team.
     */
    UnravelDocsResponse<List<TeamMemberResponse>> getTeamMembers(String teamId, User user);

    // ========== Member Management ==========

    /**
     * Add a member to the team.
     */
    UnravelDocsResponse<TeamMemberResponse> addMember(String teamId, AddTeamMemberRequest request, User user);

    /**
     * Remove a member from the team.
     */
    UnravelDocsResponse<Void> removeMember(String teamId, String memberId, User user);

    /**
     * Batch remove members from the team.
     */
    UnravelDocsResponse<Void> removeMembers(String teamId, RemoveTeamMembersRequest request, User user);

    /**
     * Promote a member to admin (Enterprise only).
     */
    UnravelDocsResponse<TeamMemberResponse> promoteToAdmin(String teamId, String memberId, User user);

    // ========== Invitations ==========

    /**
     * Send an invitation email (Enterprise only).
     */
    UnravelDocsResponse<String> sendInvitation(String teamId, InviteTeamMemberRequest request, User user);

    /**
     * Accept an invitation.
     */
    UnravelDocsResponse<TeamMemberResponse> acceptInvitation(String token, User user);

    // ========== Subscription Management ==========

    /**
     * Cancel team subscription. User can still use services until subscription
     * ends.
     */
    UnravelDocsResponse<TeamResponse> cancelSubscription(String teamId, User user);

    // ========== Team Lifecycle ==========

    /**
     * Close the team. Members remain but lose access.
     */
    UnravelDocsResponse<Void> closeTeam(String teamId, User user);

    /**
     * Reactivate a closed team.
     */
    UnravelDocsResponse<TeamResponse> reactivateTeam(String teamId, User user);
}
