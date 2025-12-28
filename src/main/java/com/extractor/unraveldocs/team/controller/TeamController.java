package com.extractor.unraveldocs.team.controller;

import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.team.dto.request.*;
import com.extractor.unraveldocs.team.dto.response.*;
import com.extractor.unraveldocs.team.interfaces.TeamService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for team management operations.
 * Handles team creation, member management, invitations, and lifecycle
 * operations.
 */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Team Management", description = "Endpoints for team operations")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    // ========== Team Creation ==========

    @PostMapping("/initiate")
    @Operation(summary = "Initiate team creation", description = "Sends OTP to verify team creation")
    public ResponseEntity<UnravelDocsResponse<Void>> initiateTeamCreation(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<Void> response = teamService.initiateTeamCreation(request, user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP and create team", description = "Completes team creation after OTP verification")
    public ResponseEntity<UnravelDocsResponse<TeamResponse>> verifyOtpAndCreateTeam(
            @Valid @RequestBody VerifyTeamOtpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<TeamResponse> response = teamService.verifyOtpAndCreateTeam(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== Team View ==========

    @GetMapping("/{teamId}")
    @Operation(summary = "Get team details", description = "Returns team details for members")
    public ResponseEntity<UnravelDocsResponse<TeamResponse>> getTeamById(
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<TeamResponse> response = teamService.getTeamById(teamId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my teams", description = "Returns all teams the user belongs to")
    public ResponseEntity<UnravelDocsResponse<List<TeamResponse>>> getMyTeams(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<List<TeamResponse>> response = teamService.getMyTeams(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}/members")
    @Operation(summary = "Get team members", description = "Returns all members of a team (emails masked for non-owners)")
    public ResponseEntity<UnravelDocsResponse<List<TeamMemberResponse>>> getTeamMembers(
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<List<TeamMemberResponse>> response = teamService.getTeamMembers(teamId, user);
        return ResponseEntity.ok(response);
    }

    // ========== Member Management ==========

    @PostMapping("/{teamId}/members")
    @Operation(summary = "Add member", description = "Adds a user to the team (requires admin/owner role)")
    public ResponseEntity<UnravelDocsResponse<TeamMemberResponse>> addMember(
            @PathVariable String teamId,
            @Valid @RequestBody AddTeamMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<TeamMemberResponse> response = teamService.addMember(teamId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    @Operation(summary = "Remove member", description = "Removes a member from the team (requires admin/owner role)")
    public ResponseEntity<UnravelDocsResponse<Void>> removeMember(
            @PathVariable String teamId,
            @PathVariable String memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<Void> response = teamService.removeMember(teamId, memberId, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}/members/batch")
    @Operation(summary = "Batch remove members", description = "Removes multiple members from the team")
    public ResponseEntity<UnravelDocsResponse<Void>> removeMembers(
            @PathVariable String teamId,
            @Valid @RequestBody RemoveTeamMembersRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<Void> response = teamService.removeMembers(teamId, request, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/members/{memberId}/promote")
    @Operation(summary = "Promote to admin", description = "Promotes a member to admin role (Enterprise only, owner required)")
    public ResponseEntity<UnravelDocsResponse<TeamMemberResponse>> promoteToAdmin(
            @PathVariable String teamId,
            @PathVariable String memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<TeamMemberResponse> response = teamService.promoteToAdmin(teamId, memberId, user);
        return ResponseEntity.ok(response);
    }

    // ========== Invitations ==========

    @PostMapping("/{teamId}/invitations")
    @Operation(summary = "Send invitation", description = "Sends email invitation to join team (Enterprise only)")
    public ResponseEntity<UnravelDocsResponse<String>> sendInvitation(
            @PathVariable String teamId,
            @Valid @RequestBody InviteTeamMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<String> response = teamService.sendInvitation(teamId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/invitations/{token}/accept")
    @Operation(summary = "Accept invitation", description = "Accepts a team invitation using the token")
    public ResponseEntity<UnravelDocsResponse<TeamMemberResponse>> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<TeamMemberResponse> response = teamService.acceptInvitation(token, user);
        return ResponseEntity.ok(response);
    }

    // ========== Subscription Management ==========

    @PostMapping("/{teamId}/cancel")
    @Operation(summary = "Cancel subscription", description = "Cancels subscription but service continues until period ends")
    public ResponseEntity<UnravelDocsResponse<TeamResponse>> cancelSubscription(
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<TeamResponse> response = teamService.cancelSubscription(teamId, user);
        return ResponseEntity.ok(response);
    }

    // ========== Team Lifecycle ==========

    @DeleteMapping("/{teamId}")
    @Operation(summary = "Close team", description = "Closes the team. Members remain but lose access until reactivated")
    public ResponseEntity<UnravelDocsResponse<Void>> closeTeam(
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<Void> response = teamService.closeTeam(teamId, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/reactivate")
    @Operation(summary = "Reactivate team", description = "Reactivates a closed team")
    public ResponseEntity<UnravelDocsResponse<TeamResponse>> reactivateTeam(
            @PathVariable String teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        UnravelDocsResponse<TeamResponse> response = teamService.reactivateTeam(teamId, user);
        return ResponseEntity.ok(response);
    }

    // ========== Helper Methods ==========

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
