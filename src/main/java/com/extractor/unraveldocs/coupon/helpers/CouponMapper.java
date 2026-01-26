package com.extractor.unraveldocs.coupon.helpers;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.CouponTemplateData;
import com.extractor.unraveldocs.coupon.dto.CouponUsageData;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.model.CouponTemplate;
import com.extractor.unraveldocs.coupon.model.CouponUsage;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper utility for converting between coupon entities and DTOs.
 */
@Component
public class CouponMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public CouponData toCouponData(Coupon coupon) {
        if (coupon == null)
            return null;

        OffsetDateTime now = OffsetDateTime.now();
        boolean isExpired = coupon.getValidUntil().isBefore(now);
        boolean isCurrentlyValid = coupon.isActive() &&
                now.isAfter(coupon.getValidFrom()) &&
                now.isBefore(coupon.getValidUntil());

        return CouponData.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .isCustomCode(coupon.isCustomCode())
                .description(coupon.getDescription())
                .recipientCategory(coupon.getRecipientCategory())
                .discountPercentage(coupon.getDiscountPercentage())
                .minPurchaseAmount(coupon.getMinPurchaseAmount())
                .maxUsageCount(coupon.getMaxUsageCount())
                .maxUsagePerUser(coupon.getMaxUsagePerUser())
                .currentUsageCount(coupon.getCurrentUsageCount())
                .isActive(coupon.isActive())
                .validFrom(formatDateTime(coupon.getValidFrom()))
                .validUntil(formatDateTime(coupon.getValidUntil()))
                .isExpired(isExpired)
                .isCurrentlyValid(isCurrentlyValid)
                .templateId(coupon.getTemplate() != null ? coupon.getTemplate().getId() : null)
                .templateName(coupon.getTemplate() != null ? coupon.getTemplate().getName() : null)
                .createdById(coupon.getCreatedBy() != null ? coupon.getCreatedBy().getId() : null)
                .createdByName(coupon.getCreatedBy() != null
                        ? coupon.getCreatedBy().getFirstName() + " " + coupon.getCreatedBy().getLastName()
                        : null)
                .createdAt(formatDateTime(coupon.getCreatedAt()))
                .updatedAt(formatDateTime(coupon.getUpdatedAt()))
                .build();
    }

    public CouponTemplateData toTemplateData(CouponTemplate template, int couponsCreatedCount) {
        if (template == null)
            return null;

        return CouponTemplateData.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .discountPercentage(template.getDiscountPercentage())
                .minPurchaseAmount(template.getMinPurchaseAmount())
                .recipientCategory(template.getRecipientCategory())
                .maxUsageCount(template.getMaxUsageCount())
                .maxUsagePerUser(template.getMaxUsagePerUser())
                .validityDays(template.getValidityDays())
                .isActive(template.isActive())
                .couponsCreatedCount(couponsCreatedCount)
                .createdById(template.getCreatedBy() != null ? template.getCreatedBy().getId() : null)
                .createdByName(template.getCreatedBy() != null
                        ? template.getCreatedBy().getFirstName() + " " + template.getCreatedBy().getLastName()
                        : null)
                .createdAt(formatDateTime(template.getCreatedAt()))
                .updatedAt(formatDateTime(template.getUpdatedAt()))
                .build();
    }

    public CouponUsageData toUsageData(CouponUsage usage) {
        if (usage == null)
            return null;

        return CouponUsageData.builder()
                .id(usage.getId())
                .couponId(usage.getCoupon() != null ? usage.getCoupon().getId() : null)
                .couponCode(usage.getCoupon() != null ? usage.getCoupon().getCode() : null)
                .userId(usage.getUser() != null ? usage.getUser().getId() : null)
                .userEmail(usage.getUser() != null ? usage.getUser().getEmail() : null)
                .userName(usage.getUser() != null ? usage.getUser().getFirstName() + " " + usage.getUser().getLastName()
                        : null)
                .originalAmount(usage.getOriginalAmount())
                .discountAmount(usage.getDiscountAmount())
                .finalAmount(usage.getFinalAmount())
                .paymentReference(usage.getPaymentReference())
                .subscriptionPlan(usage.getSubscriptionPlan())
                .usedAt(formatDateTime(usage.getUsedAt()))
                .build();
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(FORMATTER) : null;
    }
}
