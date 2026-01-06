package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import com.extractor.unraveldocs.shared.datamodel.MemberRole;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionStatus;
import com.extractor.unraveldocs.team.dto.request.VerifyTeamOtpRequest;
import com.extractor.unraveldocs.team.dto.response.TeamResponse;
import com.extractor.unraveldocs.team.model.Team;
import com.extractor.unraveldocs.team.model.TeamMember;
import com.extractor.unraveldocs.team.model.TeamOtpVerification;
import com.extractor.unraveldocs.team.model.TeamSubscriptionPlan;
import com.extractor.unraveldocs.team.repository.TeamMemberRepository;
import com.extractor.unraveldocs.team.repository.TeamOtpRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
import com.extractor.unraveldocs.team.service.TeamSubscriptionPlanService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for verifying OTP and creating a team.
 * Completes the team creation flow after OTP verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyOtpAndCreateTeamImpl {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamOtpRepository otpRepository;
    private final TeamSubscriptionPlanService planService;
    private final GenerateVerificationToken tokenGenerator;
    private final EmailOrchestratorService emailService;
    private final ResponseBuilderService responseBuilder;
    private final SanitizeLogging sanitizer;

    private static final int TEAM_CODE_LENGTH = 8;

    /**
     * Verifies the OTP and creates the team.
     *
     * @param request The OTP verification request
     * @param user    The authenticated user
     * @return Response with the created team details
     */
    @Transactional
    public UnravelDocsResponse<TeamResponse> verifyAndCreate(VerifyTeamOtpRequest request, User user) {
        log.info("Verifying OTP and creating team for user: {}", sanitizer.sanitizeLogging(user.getId()));

        // 1. Find and validate OTP
        TeamOtpVerification otpVerification = otpRepository
                .findValidOtp(user.getId(), request.otp(), OffsetDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        // 2. Mark OTP as used
        otpVerification.markAsUsed();
        otpRepository.save(otpVerification);

        // 3. Get subscription plan from database
        TeamSubscriptionPlan plan = planService.getPlanByName(otpVerification.getSubscriptionType().name());

        // 4. Generate unique team code
        String teamCode = generateUniqueTeamCode();

        // 5. Create team
        Team team = new Team();
        team.setName(otpVerification.getTeamName());
        team.setDescription(otpVerification.getTeamDescription());
        team.setTeamCode(teamCode);
        team.setSubscriptionType(otpVerification.getSubscriptionType());
        team.setPlan(plan);
        team.setBillingCycle(otpVerification.getBillingCycle());
        team.setSubscriptionStatus(TeamSubscriptionStatus.TRIAL);
        team.setPaymentGateway(otpVerification.getPaymentGateway());
        team.setCreatedBy(user);

        // Trial setup
        int trialDays = plan.getTrialDays() != null ? plan.getTrialDays() : 10;
        team.setTrialEndsAt(OffsetDateTime.now().plusDays(trialDays));
        team.setHasUsedTrial(true);

        // Set limits from plan
        team.setMaxMembers(plan.getMaxMembers());
        team.setMonthlyDocumentLimit(plan.getMonthlyDocumentLimit());

        // Set pricing
        team.setSubscriptionPrice(plan.getPrice(otpVerification.getBillingCycle()));
        team.setCurrency(plan.getCurrency());

        // Activation
        team.setActive(true);
        team.setVerified(true);

        teamRepository.save(team);

        // 6. Add creator as OWNER
        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeam(team);
        ownerMember.setUser(user);
        ownerMember.setRole(MemberRole.OWNER);
        teamMemberRepository.save(ownerMember);

        // 7. Send confirmation email
        sendTeamCreatedEmail(user, team);

        log.info("Team created successfully: {} (Code: {})", sanitizer.sanitizeLogging(team.getId()),
                sanitizer.sanitizeLogging(teamCode));

        // 8. Build response
        TeamResponse response = buildTeamResponse(team, 1, true);
        return responseBuilder.buildUserResponse(response, HttpStatus.CREATED, "Team created successfully");
    }

    private String generateUniqueTeamCode() {
        String code;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            // Use generateToken for team codes as they can be longer than 6 characters
            // Generate a token and take first TEAM_CODE_LENGTH characters
            String rawToken = tokenGenerator.generateToken(TEAM_CODE_LENGTH);
            code = rawToken.substring(0, Math.min(rawToken.length(), TEAM_CODE_LENGTH)).toUpperCase();
            attempts++;
        } while (teamRepository.existsByTeamCode(code) && attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new RuntimeException("Failed to generate unique team code after " + maxAttempts + " attempts");
        }

        return code;
    }

    private void sendTeamCreatedEmail(User user, Team team) {
        try {
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("firstName", user.getFirstName());
            templateModel.put("teamName", team.getName());
            templateModel.put("teamCode", team.getTeamCode());
            templateModel.put("subscriptionType", team.getSubscriptionType().name().replace("_", " "));
            templateModel.put("trialEndsAt", team.getTrialEndsAt().toLocalDate().toString());

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(user.getEmail())
                    .subject("Your Team Has Been Created - UnravelDocs")
                    .templateName("team-created")
                    .templateModel(templateModel)
                    .build();

            emailService.sendEmail(emailMessage);
            log.debug("Team created email sent to: {}", sanitizer.sanitizeLogging(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send team created email to user: {}",
                    sanitizer.sanitizeLogging(user.getEmail()), e);
        }
    }

    private TeamResponse buildTeamResponse(Team team, int memberCount, boolean isOwner) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .teamCode(team.getTeamCode())
                .subscriptionType(team.getPlan() != null ? team.getPlan().getDisplayName()
                        : team.getSubscriptionType().name().replace("_", " "))
                .billingCycle(team.getBillingCycle().getDisplayName())
                .subscriptionStatus(team.getSubscriptionStatus().getDisplayName())
                .subscriptionPrice(team.getSubscriptionPrice())
                .currency(team.getCurrency())
                .isActive(team.isActive())
                .isVerified(team.isVerified())
                .isClosed(team.isClosed())
                .autoRenew(team.isAutoRenew())
                .trialEndsAt(team.getTrialEndsAt())
                .nextBillingDate(team.getNextBillingDate())
                .subscriptionEndsAt(team.getSubscriptionEndsAt())
                .cancellationRequestedAt(team.getCancellationRequestedAt())
                .createdAt(team.getCreatedAt())
                .currentMemberCount(memberCount)
                .maxMembers(team.getMaxMembers())
                .monthlyDocumentLimit(team.getMonthlyDocumentLimit())
                .isOwner(isOwner)
                .build();
    }
}
