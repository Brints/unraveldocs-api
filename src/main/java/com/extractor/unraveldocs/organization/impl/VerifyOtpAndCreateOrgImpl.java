package com.extractor.unraveldocs.organization.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import com.extractor.unraveldocs.organization.datamodel.OrganizationSubscriptionType;
import com.extractor.unraveldocs.organization.dto.request.VerifyOrgOtpRequest;
import com.extractor.unraveldocs.organization.datamodel.OrganizationMemberRole;
import com.extractor.unraveldocs.organization.dto.response.OrganizationResponse;
import com.extractor.unraveldocs.organization.model.Organization;
import com.extractor.unraveldocs.organization.model.OrganizationMember;
import com.extractor.unraveldocs.organization.model.OrganizationOtpVerification;
import com.extractor.unraveldocs.organization.repository.OrganizationMemberRepository;
import com.extractor.unraveldocs.organization.repository.OrganizationOtpRepository;
import com.extractor.unraveldocs.organization.repository.OrganizationRepository;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import com.extractor.unraveldocs.utils.generatetoken.GenerateVerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyOtpAndCreateOrgImpl {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationOtpRepository otpRepository;
    private final GenerateVerificationToken tokenGenerator;
    private final EmailOrchestratorService emailService;
    private final SanitizeLogging sanitizer;

    private static final int TRIAL_DAYS = 10;
    private static final int PREMIUM_MONTHLY_DOC_LIMIT = 200;

    @Transactional
    @CacheEvict(value = "organizations", allEntries = true)
    public UnravelDocsResponse<OrganizationResponse> verifyAndCreate(VerifyOrgOtpRequest request, String userId) {
        log.info("Verifying OTP for team creation. User: {}", sanitizer.sanitizeLogging(userId));

        // 1. Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 2. Find valid OTP
        OrganizationOtpVerification otpVerification = otpRepository.findValidOtp(
                userId, request.getOtp(), OffsetDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        // 3. Mark OTP as used
        otpVerification.markAsUsed();
        otpRepository.save(otpVerification);

        // 4. Generate unique org code
        String orgCode = generateUniqueOrgCode();

        // 5. Create team
        Organization organization = new Organization();
        organization.setName(otpVerification.getOrganizationName());
        organization.setDescription(otpVerification.getOrganizationDescription());
        organization.setOrgCode(orgCode);
        organization.setSubscriptionType(otpVerification.getSubscriptionType());
        organization.setPaymentGateway(otpVerification.getPaymentGateway());
        organization.setCreatedBy(user);
        organization.setActive(true);
        organization.setVerified(true);
        organization.setClosed(false);

        // Set document limits based on subscription type
        if (OrganizationSubscriptionType.PREMIUM.equals(otpVerification.getSubscriptionType())) {
            organization.setMonthlyDocumentLimit(PREMIUM_MONTHLY_DOC_LIMIT);
        } else {
            organization.setMonthlyDocumentLimit(null); // Unlimited for Enterprise
        }

        // Set trial period (10 days free)
        organization.setTrialEndsAt(OffsetDateTime.now().plusDays(TRIAL_DAYS));
        organization.setHasUsedTrial(true);

        Organization savedOrg = organizationRepository.save(organization);

        // 6. Add creator as OWNER
        OrganizationMember ownerMember = new OrganizationMember();
        ownerMember.setOrganization(savedOrg);
        ownerMember.setUser(user);
        ownerMember.setRole(OrganizationMemberRole.OWNER);
        memberRepository.save(ownerMember);

        // 7. Update user's isOrganizationAdmin flag
        user.setOrganizationAdmin(true);
        userRepository.save(user);

        // 8. Send confirmation email
        sendConfirmationEmail(user, savedOrg);

        log.info("Organization created successfully: {} ({})", sanitizer.sanitizeLogging(savedOrg.getName()), sanitizer.sanitizeLogging(savedOrg.getId()));

        OrganizationResponse response = buildOrganizationResponse(savedOrg, user.getId(), 1);

        return new UnravelDocsResponse<>(
                HttpStatus.CREATED.value(),
                "Success",
                "Organization created successfully. You have a 10-day free trial.",
                response);
    }

    private String generateUniqueOrgCode() {
        String code;
        int attempts = 0;
        do {
            code = tokenGenerator.generateVerificationToken().substring(0, 8).toUpperCase();
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Failed to generate unique org code");
            }
        } while (organizationRepository.existsByOrgCode(code));
        return code;
    }

    private void sendConfirmationEmail(User user, Organization org) {
        try {
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("firstName", user.getFirstName());
            templateModel.put("organizationName", org.getName());
            templateModel.put("orgCode", org.getOrgCode());
            templateModel.put("subscriptionType", org.getSubscriptionType().getDisplayName());
            templateModel.put("trialEndDate", org.getTrialEndsAt().toLocalDate().toString());
            templateModel.put("documentLimit", org.getMonthlyDocumentLimit() != null
                    ? org.getMonthlyDocumentLimit() + " documents/month"
                    : "Unlimited");

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(user.getEmail())
                    .subject("Your Organization is Ready - UnravelDocs")
                    .templateName("team-created")
                    .templateModel(templateModel)
                    .build();

            emailService.sendEmail(emailMessage);
            log.debug("Organization confirmation email sent to: {}", sanitizer.sanitizeLogging(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send team confirmation email: {}", e.getMessage());
        }
    }

    private OrganizationResponse buildOrganizationResponse(Organization org, String viewerId, int memberCount) {
        boolean isOwner = org.getCreatedBy().getId().equals(viewerId);
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .description(org.getDescription())
                .orgCode(org.getOrgCode())
                .subscriptionType(org.getSubscriptionType())
                .paymentGateway(org.getPaymentGateway())
                .inTrial(org.isInTrial())
                .trialEndsAt(org.getTrialEndsAt())
                .active(org.isActive())
                .verified(org.isVerified())
                .closed(org.isClosed())
                .maxMembers(org.getMaxMembers())
                .currentMemberCount(memberCount)
                .monthlyDocumentLimit(org.getMonthlyDocumentLimit())
                .monthlyDocumentUploadCount(org.getMonthlyDocumentUploadCount())
                .createdById(org.getCreatedBy().getId())
                .createdByName(org.getCreatedBy().getFirstName() + " " + org.getCreatedBy().getLastName())
                .isOwner(isOwner)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .closedAt(org.getClosedAt())
                .build();
    }
}
