package com.extractor.unraveldocs.team.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import com.extractor.unraveldocs.team.datamodel.TeamBillingCycle;
import com.extractor.unraveldocs.team.datamodel.TeamSubscriptionType;
import com.extractor.unraveldocs.team.dto.request.CreateTeamRequest;
import com.extractor.unraveldocs.team.model.TeamOtpVerification;
import com.extractor.unraveldocs.team.repository.TeamOtpRepository;
import com.extractor.unraveldocs.team.repository.TeamRepository;
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
 * Implementation for initiating team creation.
 * Sends OTP to user's email for verification before creating the team.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateTeamCreationImpl {

    private final TeamRepository teamRepository;
    private final TeamOtpRepository otpRepository;
    private final GenerateVerificationToken tokenGenerator;
    private final EmailOrchestratorService emailService;
    private final SanitizeLogging sanitizer;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 15;

    /**
     * Initiates team creation by sending OTP to user's email.
     * Any verified user can create a team - no subscription requirement.
     *
     * @param request The team creation request containing name, subscription type,
     *                etc.
     * @param user    The authenticated user creating the team
     * @return Response indicating OTP was sent
     */
    @Transactional
    public UnravelDocsResponse<Void> initiateCreation(CreateTeamRequest request, User user) {
        log.info("Initiating team creation for user: {}", sanitizer.sanitizeLogging(user.getId()));

        // 1. Validate user is active and verified
        if (!user.isActive() || !user.isVerified()) {
            throw new ForbiddenException("Only active and verified users can create teams");
        }

        // 2. Validate team name is unique
        if (teamRepository.existsByName(request.name())) {
            throw new ConflictException("Team with this name already exists");
        }

        // 3. Validate subscription type
        validateSubscriptionType(request.subscriptionType());

        // 4. Validate billing cycle
        validateBillingCycle(request.billingCycle());

        // 5. Mark all previous OTPs as used
        otpRepository.markAllOtpsAsUsedForUser(user.getId());

        // 6. Generate OTP
        String otp = tokenGenerator.generateOtp(OTP_LENGTH);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // 7. Create OTP verification record
        TeamOtpVerification otpVerification = new TeamOtpVerification();
        otpVerification.setUserId(user.getId());
        otpVerification.setTeamName(request.name());
        otpVerification.setTeamDescription(request.description());
        otpVerification.setSubscriptionType(request.subscriptionType());
        otpVerification.setBillingCycle(request.billingCycle());
        otpVerification.setPaymentGateway(request.paymentGateway());
        otpVerification.setPaymentToken(request.paymentToken());
        otpVerification.setOtp(otp);
        otpVerification.setExpiresAt(expiresAt);
        otpRepository.save(otpVerification);

        // 8. Send OTP email
        sendOtpEmail(user, otp, request.name());

        log.info("OTP sent successfully for team creation. User: {}", sanitizer.sanitizeLogging(user.getId()));

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "OTP has been sent to your email. Please verify to complete team creation.",
                null);
    }

    private void validateSubscriptionType(TeamSubscriptionType type) {
        if (type == null) {
            throw new BadRequestException("Team subscription type is required");
        }
    }

    private void validateBillingCycle(TeamBillingCycle cycle) {
        if (cycle == null) {
            throw new BadRequestException("Billing cycle is required");
        }
    }

    private void sendOtpEmail(User user, String otp, String teamName) {
        try {
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("firstName", user.getFirstName());
            templateModel.put("otp", otp);
            templateModel.put("teamName", teamName);
            templateModel.put("expiryMinutes", OTP_EXPIRY_MINUTES);

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(user.getEmail())
                    .subject("Verify Your Team Creation - UnravelDocs")
                    .templateName("team-otp")
                    .templateModel(templateModel)
                    .build();

            emailService.sendEmail(emailMessage);
            log.debug("OTP email sent to: {}", sanitizer.sanitizeLogging(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send OTP email to user: {}", sanitizer.sanitizeLogging(user.getEmail()), e);
            // Don't throw - OTP is saved, user can request resend
        }
    }
}
