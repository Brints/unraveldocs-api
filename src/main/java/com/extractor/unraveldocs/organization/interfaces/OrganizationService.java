package com.extractor.unraveldocs.organization.interfaces;

import com.extractor.unraveldocs.organization.dto.request.*;
import com.extractor.unraveldocs.organization.dto.response.OrganizationMemberResponse;
import com.extractor.unraveldocs.organization.dto.response.OrganizationResponse;
import com.extractor.unraveldocs.team.dto.request.PromoteToAdminRequest;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

import java.util.List;

/**
 * Service interface for team management operations.
 */
public interface OrganizationService {

        // ============== Organization Creation Flow ==============

        /**
         * Initiates team creation by generating an OTP and sending it to the
         * user's email.
         * The team is not created until the OTP is verified.
         *
         * @param request The team creation request
         * @param userId  The ID of the user creating the team
         * @return Response with a message indicating OTP was sent
         */
        UnravelDocsResponse<String> initiateOrganizationCreation(CreateOrganizationRequest request, String userId);

        /**
         * Verifies the OTP and creates the team.
         *
         * @param request The OTP verification request
         * @param userId  The ID of the user creating the team
         * @return Response with the created team details
         */
        UnravelDocsResponse<OrganizationResponse> verifyOtpAndCreateOrganization(VerifyOrgOtpRequest request,
                        String userId);

        // ============== Member Management ==============

        /**
         * Adds a user as a member to an team.
         * Only ADMIN or OWNER can add members.
         * Maximum 10 members per team.
         *
         * @param orgId   The team ID
         * @param request The add member request
         * @param adminId The ID of the user performing the action
         * @return Response with the new member details
         */
        UnravelDocsResponse<OrganizationMemberResponse> addMember(String orgId, AddMemberRequest request,
                        String adminId);

        /**
         * Removes a single member from an team.
         * Only ADMIN or OWNER can remove members.
         *
         * @param orgId    The team ID
         * @param memberId The ID of the member to remove
         * @param adminId  The ID of the user performing the action
         * @return Response indicating success
         */
        UnravelDocsResponse<Void> removeMember(String orgId, String memberId, String adminId);

        /**
         * Removes multiple members from an team in batch.
         * Only ADMIN or OWNER can remove members.
         *
         * @param orgId   The team ID
         * @param request The batch remove request containing user IDs
         * @param adminId The ID of the user performing the action
         * @return Response indicating success
         */
        UnravelDocsResponse<Void> removeMembers(String orgId, RemoveMembersRequest request, String adminId);

        // ============== Admin Promotion (Enterprise Only) ==============

        /**
         * Promotes a member to admin role.
         * Only OWNER can promote members.
         * Only available for ENTERPRISE subscription.
         *
         * @param orgId   The team ID
         * @param request The promote request containing user ID
         * @param ownerId The ID of the team owner
         * @return Response with the updated member details
         */
        UnravelDocsResponse<OrganizationMemberResponse> promoteToAdmin(String orgId, PromoteToAdminRequest request,
                        String ownerId);

        // ============== Invitation System (Enterprise Only) ==============

        /**
         * Sends an email invitation to join an team.
         * Only ADMIN or OWNER can send invitations.
         * Only available for ENTERPRISE subscription.
         *
         * @param orgId   The team ID
         * @param request The invitation request containing email
         * @param adminId The ID of the user sending the invitation
         * @return Response with the invitation link
         */
        UnravelDocsResponse<String> inviteMember(String orgId, InviteMemberRequest request, String adminId);

        /**
         * Accepts an invitation using the invitation token.
         *
         * @param token  The invitation token
         * @param userId The ID of the user accepting the invitation
         * @return Response with the member details
         */
        UnravelDocsResponse<OrganizationMemberResponse> acceptInvitation(String token, String userId);

        // ============== View Operations ==============

        /**
         * Gets team details.
         * Only members can view team details.
         *
         * @param orgId  The team ID
         * @param userId The ID of the user requesting the details
         * @return Response with team details
         */
        UnravelDocsResponse<OrganizationResponse> getOrganization(String orgId, String userId);

        /**
         * Gets all members of an team.
         * Only members can view the member list.
         * Email addresses are masked for non-owners (except for own email).
         *
         * @param orgId  The team ID
         * @param userId The ID of the user requesting the list
         * @return Response with list of members
         */
        UnravelDocsResponse<List<OrganizationMemberResponse>> getMembers(String orgId, String userId);

        /**
         * Gets all organizations the user belongs to.
         *
         * @param userId The ID of the user
         * @return Response with list of organizations
         */
        UnravelDocsResponse<List<OrganizationResponse>> getMyOrganizations(String userId);

        // ============== Organization Lifecycle ==============

        /**
         * Closes an team.
         * Only OWNER can close the team.
         * Members remain but lose access until reactivated.
         *
         * @param orgId   The team ID
         * @param ownerId The ID of the team owner
         * @return Response indicating success
         */
        UnravelDocsResponse<Void> closeOrganization(String orgId, String ownerId);

        /**
         * Reactivates a closed team.
         * Only OWNER can reactivate the team.
         *
         * @param orgId   The team ID
         * @param ownerId The ID of the team owner
         * @return Response with the reactivated team details
         */
        UnravelDocsResponse<OrganizationResponse> reactivateOrganization(String orgId, String ownerId);
}
