package com.extractor.unraveldocs.coupon.service.impl;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.request.ApplyCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.CouponValidationResponse;
import com.extractor.unraveldocs.coupon.dto.response.DiscountCalculationData;
import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.helpers.CouponMapper;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.model.CouponUsage;
import com.extractor.unraveldocs.coupon.repository.CouponRecipientRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.repository.CouponUsageRepository;
import com.extractor.unraveldocs.coupon.service.CouponValidationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponValidationServiceImpl implements CouponValidationService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponRecipientRepository couponRecipientRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final CouponMapper couponMapper;
    private final SanitizeLogging sanitizer;

    @Override
    public CouponValidationResponse validateCoupon(String couponCode, User user) {
        log.info("Validating coupon: {} for user: {}",
                sanitizer.sanitizeLogging(couponCode),
                sanitizer.sanitizeLogging(user.getEmail()));

        // Find coupon
        Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase()).orElse(null);
        if (coupon == null) {
            return CouponValidationResponse.invalid("Coupon not found", "COUPON_NOT_FOUND");
        }

        // Check if active
        if (!coupon.isActive()) {
            return CouponValidationResponse.invalid("Coupon is inactive", "COUPON_INACTIVE");
        }

        // Check date range
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(coupon.getValidFrom())) {
            return CouponValidationResponse.invalid("Coupon is not yet valid", "COUPON_NOT_YET_VALID");
        }
        if (now.isAfter(coupon.getValidUntil())) {
            return CouponValidationResponse.invalid("Coupon has expired", "COUPON_EXPIRED");
        }

        // Check total usage limit
        if (coupon.hasReachedUsageLimit()) {
            return CouponValidationResponse.invalid("Coupon usage limit reached", "USAGE_LIMIT_REACHED");
        }

        // Check per-user usage limit
        if (hasExceededPerUserLimit(coupon.getId(), user.getId(), coupon.getMaxUsagePerUser())) {
            return CouponValidationResponse.invalid(
                    "You have already used this coupon the maximum number of times",
                    "PER_USER_LIMIT_REACHED");
        }

        // Check recipient eligibility
        if (!isUserEligibleForCoupon(user, coupon)) {
            return CouponValidationResponse.invalid(
                    "You are not eligible for this coupon",
                    "USER_NOT_ELIGIBLE");
        }

        CouponData couponData = couponMapper.toCouponData(coupon);
        return CouponValidationResponse.valid(couponData);
    }

    @Override
    public DiscountCalculationData applyCouponToAmount(ApplyCouponRequest request, User user) {
        log.info("Applying coupon: {} to amount: {}",
                sanitizer.sanitizeLogging(request.getCouponCode()),
                sanitizer.sanitizeLoggingObject(request.getAmount()));

        // Validate coupon first
        CouponValidationResponse validation = validateCoupon(request.getCouponCode(), user);
        if (!validation.isValid()) {
            return null;
        }

        Coupon coupon = couponRepository.findByCode(request.getCouponCode().toUpperCase()).orElseThrow();

        // Check minimum purchase amount
        if (coupon.getMinPurchaseAmount() != null &&
                request.getAmount().compareTo(coupon.getMinPurchaseAmount()) < 0) {
            log.info("Amount {} is below minimum purchase amount {}",
                    sanitizer.sanitizeLoggingObject(request.getAmount()),
                    sanitizer.sanitizeLoggingObject(coupon.getMinPurchaseAmount()));
            return DiscountCalculationData.builder()
                    .couponCode(coupon.getCode())
                    .originalAmount(request.getAmount())
                    .minPurchaseAmount(coupon.getMinPurchaseAmount())
                    .minPurchaseRequirementMet(false)
                    .build();
        }

        // Calculate discount
        BigDecimal discountAmount = request.getAmount()
                .multiply(coupon.getDiscountPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal finalAmount = request.getAmount().subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return DiscountCalculationData.builder()
                .couponCode(coupon.getCode())
                .originalAmount(request.getAmount())
                .discountPercentage(coupon.getDiscountPercentage())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .minPurchaseAmount(coupon.getMinPurchaseAmount())
                .minPurchaseRequirementMet(true)
                .build();
    }

    @Override
    @Transactional
    public void recordCouponUsage(
            Coupon coupon,
            User user,
            BigDecimal originalAmount,
            BigDecimal finalAmount,
            String paymentReference,
            String subscriptionPlan) {

        log.info("Recording coupon usage: {} for user: {}",
                sanitizer.sanitizeLogging(coupon.getCode()),
                sanitizer.sanitizeLogging(user.getEmail()));

        BigDecimal discountAmount = originalAmount.subtract(finalAmount);

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .paymentReference(paymentReference)
                .subscriptionPlan(subscriptionPlan)
                .usedAt(OffsetDateTime.now())
                .build();

        couponUsageRepository.save(usage);

        // Increment coupon usage count
        coupon.incrementUsageCount();
        couponRepository.save(coupon);

        log.info("Coupon usage recorded. New usage count: {}",
                sanitizer.sanitizeLoggingInteger(coupon.getCurrentUsageCount()));
    }

    @Override
    public boolean isUserEligibleForCoupon(User user, Coupon coupon) {
        RecipientCategory category = coupon.getRecipientCategory();

        return switch (category) {
            case ALL_PAID_USERS -> hasActivePaidSubscription(user);
            case INDIVIDUAL_PLAN -> hasSubscriptionPlan(user, "INDIVIDUAL");
            case TEAM_PLAN -> hasSubscriptionPlan(user, "TEAM");
            case ENTERPRISE_PLAN -> hasSubscriptionPlan(user, "ENTERPRISE");
            case FREE_TIER_ACTIVE -> isFreeTierWithHighActivity(user);
            case EXPIRED_SUBSCRIPTION -> hasRecentlyExpiredSubscription(user);
            case NEW_USERS -> isNewUser(user);
            case HIGH_ACTIVITY_USERS -> isHighActivityUser(user);
            case SPECIFIC_USERS -> couponRecipientRepository.existsByCouponIdAndUserId(coupon.getId(), user.getId());
        };
    }

    @Override
    public boolean hasExceededPerUserLimit(String couponId, String userId, int maxUsagePerUser) {
        int userUsageCount = couponUsageRepository.countByCouponIdAndUserId(couponId, userId);
        return userUsageCount >= maxUsagePerUser;
    }

    private boolean hasActivePaidSubscription(User user) {
        return userSubscriptionRepository.findByUserId(user.getId())
                .map(sub -> isSubscriptionActive(sub) &&
                        sub.getPlan().getName() != SubscriptionPlans.FREE)
                .orElse(false);
    }

    private boolean hasSubscriptionPlan(User user, String planName) {
        return userSubscriptionRepository.findByUserId(user.getId())
                .map(sub -> isSubscriptionActive(sub) &&
                        planName.equalsIgnoreCase(sub.getPlan().getName().name()))
                .orElse(false);
    }

    private boolean isFreeTierWithHighActivity(User user) {
        var subscription = userSubscriptionRepository.findByUserId(user.getId()).orElse(null);
        if (subscription == null || subscription.getPlan().getName() != SubscriptionPlans.FREE) {
            return false;
        }
        // Check if user has 20+ OCR operations in last 6 months
        Integer ocrPagesUsed = subscription.getOcrPagesUsed();
        return ocrPagesUsed != null && ocrPagesUsed >= 20;
    }

    private boolean hasRecentlyExpiredSubscription(User user) {
        var subscription = userSubscriptionRepository.findByUserId(user.getId()).orElse(null);
        if (subscription == null || isSubscriptionActive(subscription)) {
            return false;
        }
        // Check if expired within last 3 months
        OffsetDateTime threeMonthsAgo = OffsetDateTime.now().minusMonths(3);
        return subscription.getCurrentPeriodEnd() != null &&
                subscription.getCurrentPeriodEnd().isAfter(threeMonthsAgo);
    }

    private boolean isNewUser(User user) {
        // Users registered within last 30 days
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        return user.getCreatedAt() != null && user.getCreatedAt().isAfter(thirtyDaysAgo);
    }

    private boolean isHighActivityUser(User user) {
        var subscription = userSubscriptionRepository.findByUserId(user.getId()).orElse(null);
        if (subscription == null) {
            return false;
        }
        // High activity: using more than 50% of plan limits
        Integer ocrPagesUsed = subscription.getOcrPagesUsed();
        Integer ocrPageLimit = subscription.getPlan().getOcrPageLimit();
        if (ocrPagesUsed == null || ocrPageLimit == null || ocrPageLimit == 0) {
            return false;
        }
        return (double) ocrPagesUsed / ocrPageLimit > 0.5;
    }

    /**
     * Helper method to check if subscription is active based on status.
     */
    private boolean isSubscriptionActive(UserSubscription subscription) {
        return "ACTIVE".equalsIgnoreCase(subscription.getStatus()) ||
                "active".equalsIgnoreCase(subscription.getStatus());
    }
}
