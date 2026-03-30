package com.extractor.unraveldocs.admin.impl;

import com.extractor.unraveldocs.admin.dto.response.AdminUserDetailDto;
import com.extractor.unraveldocs.admin.interfaces.GetUserProfileByAdminService;
import com.extractor.unraveldocs.credit.model.UserCreditBalance;
import com.extractor.unraveldocs.credit.repository.UserCreditBalanceRepository;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class GetUserProfileByAdmin implements GetUserProfileByAdminService {
    private final ResponseBuilderService responseBuilder;
    private final UserRepository userRepository;
    private final UserCreditBalanceRepository creditBalanceRepository;

    @Override
    public UnravelDocsResponse<AdminUserDetailDto> getUserProfileByAdmin(String userId) {
        AdminUserDetailDto data = getCachedAdminUserData(userId);

        return responseBuilder.buildUserResponse(
                data,
                HttpStatus.OK,
                "Enriched user profile retrieved successfully");
    }

    @Cacheable(value = "adminUserProfileData", key = "#userId")
    public AdminUserDetailDto getCachedAdminUserData(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserCreditBalance creditBalance = creditBalanceRepository.findByUserId(userId).orElse(null);

        return mapToDetailDto(user, creditBalance);
    }

    private AdminUserDetailDto mapToDetailDto(User user, UserCreditBalance credit) {
        // Map Profile
        AdminUserDetailDto.Profile profile = AdminUserDetailDto.Profile.builder()
                .id(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .country(user.getCountry())
                .profession(user.getProfession())
                .organization(user.getOrganization())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();

        // Map Status Flags
        AdminUserDetailDto.StatusFlags flags = AdminUserDetailDto.StatusFlags.builder()
                .isActive(user.isActive())
                .isVerified(user.isVerified())
                .isPlatformAdmin(user.isPlatformAdmin())
                .isOrganizationAdmin(user.isOrganizationAdmin())
                .termsAccepted(user.isTermsAccepted())
                .marketingOptIn(user.isMarketingOptIn())
                .build();

        // Map Login Security
        LoginAttempts la = user.getLoginAttempts();
        AdminUserDetailDto.LoginSecurity security = AdminUserDetailDto.LoginSecurity.builder()
                .attempts(la != null ? la.getLoginAttempts() : 0)
                .isBlocked(la != null && la.isBlocked())
                .blockedUntil(la != null && la.getBlockedUntil() != null ? la.getBlockedUntil().atOffset(ZoneOffset.UTC) : null)
                .build();

        // Map Subscription and Quotas
        UserSubscription sub = user.getSubscription();
        AdminUserDetailDto.Subscription subDto = null;
        AdminUserDetailDto.UsageQuotas quotasDto = null;

        if (sub != null) {
            subDto = AdminUserDetailDto.Subscription.builder()
                    .planName(sub.getPlan() != null && sub.getPlan().getName() != null ? sub.getPlan().getName().name() : "Unknown")
                    .status(sub.getStatus())
                    .periodStart(sub.getCurrentPeriodStart())
                    .periodEnd(sub.getCurrentPeriodEnd())
                    .autoRenew(sub.isAutoRenew())
                    .trialEndsAt(sub.getTrialEndsAt())
                    .source(sub.getSubscriptionSource() != null ? sub.getSubscriptionSource().name() : null)
                    .gatewaySubscriptionId(sub.getPaymentGatewaySubscriptionId())
                    .build();

            quotasDto = AdminUserDetailDto.UsageQuotas.builder()
                    .storageUsed(sub.getStorageUsed())
                    .storageLimit(sub.getPlan() != null ? sub.getPlan().getStorageLimit() : 0L)
                    .ocrUsed(sub.getOcrPagesUsed())
                    .ocrLimit(sub.getPlan() != null ? sub.getPlan().getOcrPageLimit() : 0)
                    .aiUsed(sub.getAiOperationsUsed())
                    .aiLimit(sub.getPlan() != null ? sub.getPlan().getAiOperationsLimit() : 0)
                    .docsUploaded(sub.getMonthlyDocumentsUploaded())
                    .docsLimit(sub.getPlan() != null ? sub.getPlan().getDocumentUploadLimit() : 0)
                    .quotaResetDate(sub.getQuotaResetDate())
                    .build();
        }

        // Map Credit Balance
        AdminUserDetailDto.CreditBalance creditDto = null;
        if (credit != null) {
            creditDto = AdminUserDetailDto.CreditBalance.builder()
                    .balance(credit.getBalance())
                    .totalPurchased(credit.getTotalPurchased())
                    .totalUsed(credit.getTotalUsed())
                    .build();
        } else {
            creditDto = AdminUserDetailDto.CreditBalance.builder().balance(0).totalPurchased(0).totalUsed(0).build();
        }

        return AdminUserDetailDto.builder()
                .profile(profile)
                .statusFlags(flags)
                .loginSecurity(security)
                .subscription(subDto)
                .usageQuotas(quotasDto)
                .creditBalance(creditDto)
                .build();
    }
}
