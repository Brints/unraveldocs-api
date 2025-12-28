package com.extractor.unraveldocs.organization.controller;

import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.organization.dto.request.*;
import com.extractor.unraveldocs.organization.dto.response.OrganizationMemberResponse;
import com.extractor.unraveldocs.organization.dto.response.OrganizationResponse;
import com.extractor.unraveldocs.organization.interfaces.OrganizationService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management APIs")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(UserDetails authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ForbiddenException("Please login to perform this action.");
        }
        return userRepository.findByEmail(authenticatedUser.getUsername())
                .orElseThrow(() -> new ForbiddenException("User not found"));
    }

    // ============== Organization Creation Flow ==============

    @PostMapping("/initiate")
    @Operation(summary = "Initiate organization creation", description = "Sends OTP to user email to verify organization creation. Requires Premium or Enterprise subscription.")
    public ResponseEntity<UnravelDocsResponse<String>> initiateOrganizationCreation(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<String> response = organizationService.initiateOrganizationCreation(
                request, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP and create organization", description = "Verifies the OTP and creates the organization with 10-day free trial.")
    public ResponseEntity<UnravelDocsResponse<OrganizationResponse>> verifyOtpAndCreateOrganization(
            @Valid @RequestBody VerifyOrgOtpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<OrganizationResponse> response = organizationService.verifyOtpAndCreateOrganization(
                request, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // ============== View Operations ==============

    @GetMapping("/{orgId}")
    @Operation(summary = "Get organization details", description = "Returns organization details. Only accessible by members.")
    public ResponseEntity<UnravelDocsResponse<OrganizationResponse>> getOrganization(
            @PathVariable String orgId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<OrganizationResponse> response = organizationService.getOrganization(
                orgId, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my organizations", description = "Returns all organizations the user belongs to.")
    public ResponseEntity<UnravelDocsResponse<List<OrganizationResponse>>> getMyOrganizations(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<List<OrganizationResponse>> response = organizationService.getMyOrganizations(
                user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/{orgId}/members")
    @Operation(summary = "Get organization members", description = "Returns members list. Email is masked for non-owners (except own email).")
    public ResponseEntity<UnravelDocsResponse<List<OrganizationMemberResponse>>> getMembers(
            @PathVariable String orgId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<List<OrganizationMemberResponse>> response = organizationService.getMembers(
                orgId, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // ============== Member Management ==============

    @PostMapping("/{orgId}/members")
    @Operation(summary = "Add member to organization", description = "Adds a user as a member. Only ADMIN or OWNER can add members. Max 10 members.")
    public ResponseEntity<UnravelDocsResponse<OrganizationMemberResponse>> addMember(
            @PathVariable String orgId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<OrganizationMemberResponse> response = organizationService.addMember(
                orgId, request, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/{orgId}/members/{memberId}")
    @Operation(summary = "Remove member from organization", description = "Removes a single member. Only ADMIN or OWNER can remove.")
    public ResponseEntity<UnravelDocsResponse<Void>> removeMember(
            @PathVariable String orgId,
            @PathVariable String memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<Void> response = organizationService.removeMember(
                orgId, memberId, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/{orgId}/members/batch")
    @Operation(summary = "Batch remove members", description = "Removes multiple members at once. Only ADMIN or OWNER can remove.")
    public ResponseEntity<UnravelDocsResponse<Void>> removeMembers(
            @PathVariable String orgId,
            @Valid @RequestBody RemoveMembersRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<Void> response = organizationService.removeMembers(
                orgId, request, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // ============== Admin Promotion (Enterprise Only) ==============

    @PostMapping("/{orgId}/members/{memberId}/promote")
    @Operation(summary = "Promote member to admin", description = "Promotes a member to admin. Only OWNER can promote. Enterprise only.")
    public ResponseEntity<UnravelDocsResponse<OrganizationMemberResponse>> promoteToAdmin(
            @PathVariable String orgId,
            @PathVariable String memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        PromoteToAdminRequest request = new PromoteToAdminRequest(memberId);
        UnravelDocsResponse<OrganizationMemberResponse> response = organizationService.promoteToAdmin(
                orgId, request, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // ============== Invitation System (Enterprise Only) ==============

    @PostMapping("/{orgId}/invitations")
    @Operation(summary = "Send invitation", description = "Sends email invitation to join organization. Enterprise only.")
    public ResponseEntity<UnravelDocsResponse<String>> inviteMember(
            @PathVariable String orgId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<String> response = organizationService.inviteMember(
                orgId, request, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/invitations/{token}/accept")
    @Operation(summary = "Accept invitation", description = "Accepts an invitation using the token from email link.")
    public ResponseEntity<UnravelDocsResponse<OrganizationMemberResponse>> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<OrganizationMemberResponse> response = organizationService.acceptInvitation(
                token, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // ============== Organization Lifecycle ==============

    @DeleteMapping("/{orgId}")
    @Operation(summary = "Close organization", description = "Closes the organization. Only OWNER can close. Members remain but lose access.")
    public ResponseEntity<UnravelDocsResponse<Void>> closeOrganization(
            @PathVariable String orgId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<Void> response = organizationService.closeOrganization(
                orgId, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/{orgId}/reactivate")
    @Operation(summary = "Reactivate organization", description = "Reactivates a closed organization. Only OWNER can reactivate.")
    public ResponseEntity<UnravelDocsResponse<OrganizationResponse>> reactivateOrganization(
            @PathVariable String orgId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<OrganizationResponse> response = organizationService.reactivateOrganization(
                orgId, user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
