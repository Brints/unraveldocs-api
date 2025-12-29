package com.extractor.unraveldocs.organization.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.ConflictException;
import com.extractor.unraveldocs.exceptions.custom.ForbiddenException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import com.extractor.unraveldocs.organization.dto.request.CreateOrganizationRequest;
import com.extractor.unraveldocs.organization.model.OrganizationOtpVerification;
import com.extractor.unraveldocs.organization.repository.OrganizationOtpRepository;
import com.extractor.unraveldocs.organization.repository.OrganizationRepository;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateOrgCreationImpl {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationOtpRepository otpRepository;
    private final GenerateVerificationToken tokenGenerator;
    private final EmailOrchestratorService emailService;
    private final SanitizeLogging sanitizer;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 15;

    @Transactional
    public UnravelDocsResponse<String> initiateCreation(CreateOrganizationRequest request, String userId) {
        log.info("Initiating team creation for user: {}", sanitizer.sanitizeLogging(userId));

        // 1. Validate user exists, is active and verified
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isActive() || !user.isVerified()) {
            throw new ForbiddenException("Only active and verified users can create organizations");
        }

        // 2. Validate user has Premium or Enterprise subscription
        validateUserSubscription(user);

        // 3. Validate team name is unique
        if (organizationRepository.existsByName(request.getName())) {
            throw new ConflictException("Organization with this name already exists");
        }

        // 4. Validate subscription type from request
        validateSubscriptionType(request.getSubscriptionType());

        // 5. Mark all previous OTPs as used
        otpRepository.markAllOtpsAsUsedForUser(userId);

        // 6. Generate OTP
        String otp = tokenGenerator.generateOtp(OTP_LENGTH);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // 7. Create OTP verification record
        OrganizationOtpVerification otpVerification = new OrganizationOtpVerification();
        otpVerification.setUserId(userId);
        otpVerification.setOrganizationName(request.getName());
        otpVerification.setOrganizationDescription(request.getDescription());
        otpVerification.setSubscriptionType(request.getSubscriptionType());
        otpVerification.setPaymentGateway(request.getPaymentGateway());
        otpVerification.setPaymentToken(request.getPaymentToken());
        otpVerification.setOtp(otp);
        otpVerification.setExpiresAt(expiresAt);
        otpRepository.save(otpVerification);

        // 8. Send OTP email
        sendOtpEmail(user, otp, request.getName());

        log.info("OTP sent successfully for team creation. User: {}", sanitizer.sanitizeLogging(userId));

        return new UnravelDocsResponse<>(
                HttpStatus.OK.value(),
                "Success",
                "OTP has been sent to your email. Please verify to complete team creation.",
                null);
    }

    private void validateUserSubscription(User user) {
        if (user.getSubscription() == null || user.getSubscription().getPlan() == null) {
            throw new ForbiddenException("You need a Pro or Business subscription to create an team");
        }

        SubscriptionPlans planName = user.getSubscription().getPlan().getName();
        boolean isPremiumOrEnterprise = planName == SubscriptionPlans.PRO_MONTHLY ||
                planName == SubscriptionPlans.PRO_YEARLY ||
                planName == SubscriptionPlans.BUSINESS_MONTHLY ||
                planName == SubscriptionPlans.BUSINESS_YEARLY;

        if (!isPremiumOrEnterprise) {
            throw new ForbiddenException("Only Pro or Business subscribers can create organizations");
        }
    }

    private void validateSubscriptionType(OrganizationSubscriptionType type) {
        if (type == null) {
            throw new BadRequestException("Organization subscription type is required");
        }
    }

    private void sendOtpEmail(User user, String otp, String orgName) {
        try {
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("firstName", user.getFirstName());
            templateModel.put("otp", otp);
            templateModel.put("organizationName", orgName);
            templateModel.put("expiryMinutes", OTP_EXPIRY_MINUTES);

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(user.getEmail())
                    .subject("Verify Your Organization Creation - UnravelDocs")
                    .templateName("team-otp")
                    .templateModel(templateModel)
                    .build();

            emailService.sendEmail(emailMessage);
            log.debug("OTP email sent to: {}", sanitizer.sanitizeLogging(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send OTP email to user: {}", sanitizer.sanitizeLogging(user.getEmail()), e);
            // Don't throw - OTP is saved
        }
    }
}
