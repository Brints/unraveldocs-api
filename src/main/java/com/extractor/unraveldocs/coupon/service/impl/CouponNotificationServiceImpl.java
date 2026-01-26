package com.extractor.unraveldocs.coupon.service.impl;

import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.model.CouponRecipient;
import com.extractor.unraveldocs.coupon.repository.CouponRecipientRepository;
import com.extractor.unraveldocs.coupon.service.CouponNotificationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.subscription.datamodel.SubscriptionPlans;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponNotificationServiceImpl implements CouponNotificationService {

    private final CouponRecipientRepository couponRecipientRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final EmailOrchestratorService emailOrchestratorService;
    private final NotificationService notificationService;
    private final SanitizeLogging sanitizer;

    private static final String COUPON_EMAIL_TEMPLATE = "coupon-notification";
    private static final String COUPON_EXPIRY_EMAIL_TEMPLATE = "coupon-expiration";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    @Override
    @Async
    public void notifyRecipients(Coupon coupon) {
        log.info("Notifying recipients for coupon: {}", coupon.getCode());

        List<User> recipients = determineRecipients(coupon);
        log.info("Found {} eligible recipients for coupon: {}",
                sanitizer.sanitizeLoggingInteger(recipients.size()),
                sanitizer.sanitizeLogging(coupon.getCode()));

        notifyRecipients(coupon, recipients);
    }

    @Override
    @Async
    public void notifyRecipients(Coupon coupon, List<User> recipients) {
        for (User recipient : recipients) {
            try {
                sendCouponEmail(recipient, coupon);
                sendCouponPushNotification(recipient, coupon);

                // Update notification status for specific recipients
                if (coupon.getRecipientCategory() == RecipientCategory.SPECIFIC_USERS) {
                    couponRecipientRepository.findByCouponIdAndUserId(coupon.getId(), recipient.getId())
                            .ifPresent(r -> {
                                r.markAsNotified();
                                couponRecipientRepository.save(r);
                            });
                }
            } catch (Exception e) {
                log.error("Failed to notify user {} about coupon {}: {}",
                        sanitizer.sanitizeLogging(recipient.getEmail()),
                        sanitizer.sanitizeLogging(coupon.getCode()), e.getMessage());
            }
        }
    }

    @Override
    public void sendCouponEmail(User user, Coupon coupon) {
        log.info("Sending coupon email to user: {} for coupon: {}",
                sanitizer.sanitizeLogging(user.getEmail()),
                sanitizer.sanitizeLogging(coupon.getCode()));

        try {
            Map<String, Object> templateModel = buildCouponEmailModel(user, coupon);

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(user.getEmail())
                    .subject("🎉 You have a new coupon: " + coupon.getCode())
                    .templateName(COUPON_EMAIL_TEMPLATE)
                    .templateModel(templateModel)
                    .build();

            emailOrchestratorService.sendEmail(emailMessage);
            log.debug("Coupon email sent to: {}", sanitizer.sanitizeLogging(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send coupon email to {}: {}", sanitizer.sanitizeLogging(user.getEmail()), e.getMessage());
        }
    }

    @Override
    public void sendCouponPushNotification(User user, Coupon coupon) {
        log.info("Sending coupon push notification to user: {} for coupon: {}",
                sanitizer.sanitizeLogging(user.getEmail()),
                sanitizer.sanitizeLogging(coupon.getCode()));

        try {
            String title = "New Coupon Available! 🎉";
            String message = String.format("Use code %s and get %s%% off your next subscription!",
                    coupon.getCode(), coupon.getDiscountPercentage().intValue());

            Map<String, String> data = new HashMap<>();
            data.put("couponCode", coupon.getCode());
            data.put("discountPercentage", coupon.getDiscountPercentage().toString());
            data.put("validUntil", coupon.getValidUntil().format(DATE_FORMATTER));

            notificationService.sendToUser(
                    user.getId(),
                    NotificationType.COUPON_RECEIVED,
                    title,
                    message,
                    data);

            log.debug("Coupon push notification sent to: {}", sanitizer.sanitizeLogging(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send coupon push notification to {}: {}", sanitizer.sanitizeLogging(user.getEmail()), e.getMessage());
        }
    }

    @Override
    @Async
    public void sendExpirationNotifications(Coupon coupon, int daysUntilExpiry) {
        log.info("Sending expiration notifications for coupon: {} (expires in {} days)",
                sanitizer.sanitizeLogging(coupon.getCode()),
                sanitizer.sanitizeLoggingInteger(daysUntilExpiry));

        // Notify admin first
        sendAdminExpirationNotification(coupon, daysUntilExpiry);

        // Notify recipients who haven't used the coupon
        if (coupon.getRecipientCategory() == RecipientCategory.SPECIFIC_USERS) {
            var recipients = couponRecipientRepository.findExpiryUnnotifiedByCouponId(coupon.getId());
            for (CouponRecipient recipient : recipients) {
                try {
                    sendExpirationEmailToUser(recipient.getUser(), coupon, daysUntilExpiry);
                    sendExpirationPushToUser(recipient.getUser(), coupon, daysUntilExpiry);
                    recipient.markAsExpiryNotified();
                    couponRecipientRepository.save(recipient);
                } catch (Exception e) {
                    log.error("Failed to send expiration notification to user: {}",
                            sanitizer.sanitizeLogging(recipient.getUser().getEmail()), e);
                }
            }
        }
    }

    @Override
    public void sendAdminExpirationNotification(Coupon coupon, int daysUntilExpiry) {
        User creator = coupon.getCreatedBy();
        if (creator == null) {
            log.warn("No creator found for coupon: {}",
                    sanitizer.sanitizeLogging(coupon.getCode()));
            return;
        }

        log.info("Sending admin expiration notification to: {} for coupon: {}",
                sanitizer.sanitizeLogging(creator.getEmail()),
                sanitizer.sanitizeLogging(coupon.getCode()));

        try {
            // Send email to admin
            Map<String, Object> templateModel = buildAdminExpirationEmailModel(coupon, daysUntilExpiry);

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(creator.getEmail())
                    .subject(String.format("⚠️ Coupon %s expires in %d day(s)", coupon.getCode(), daysUntilExpiry))
                    .templateName(COUPON_EXPIRY_EMAIL_TEMPLATE)
                    .templateModel(templateModel)
                    .build();

            emailOrchestratorService.sendEmail(emailMessage);

            // Also send push notification
            String title = "Coupon Expiring Soon";
            String message = String.format("Coupon %s will expire in %d day(s). Current usage: %d",
                    coupon.getCode(), daysUntilExpiry, coupon.getCurrentUsageCount());

            Map<String, String> data = new HashMap<>();
            data.put("couponCode", coupon.getCode());
            data.put("couponId", coupon.getId());
            data.put("daysUntilExpiry", String.valueOf(daysUntilExpiry));

            notificationService.sendToUser(
                    creator.getId(),
                    getExpiryNotificationType(daysUntilExpiry),
                    title,
                    message,
                    data);
        } catch (Exception e) {
            log.error("Failed to send admin expiration notification: {}", e.getMessage());
        }
    }

    private void sendExpirationEmailToUser(User user, Coupon coupon, int daysUntilExpiry) {
        log.debug("Sending expiration email to user: {} for coupon: {}",
                sanitizer.sanitizeLogging(user.getEmail()),
                sanitizer.sanitizeLogging(coupon.getCode()));

        try {
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("userName", user.getFirstName() != null ? user.getFirstName() : "User");
            templateModel.put("couponCode", coupon.getCode());
            templateModel.put("discountPercentage", coupon.getDiscountPercentage().intValue());
            templateModel.put("daysUntilExpiry", daysUntilExpiry);
            templateModel.put("expiryDate", coupon.getValidUntil().format(DATE_FORMATTER));

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(user.getEmail())
                    .subject(String.format("⏰ Your coupon %s expires in %d day(s)!", coupon.getCode(), daysUntilExpiry))
                    .templateName(COUPON_EXPIRY_EMAIL_TEMPLATE)
                    .templateModel(templateModel)
                    .build();

            emailOrchestratorService.sendEmail(emailMessage);
        } catch (Exception e) {
            log.error("Failed to send expiration email to {}: {}",
                    sanitizer.sanitizeLogging(user.getEmail()), e.getMessage());
        }
    }

    private void sendExpirationPushToUser(User user, Coupon coupon, int daysUntilExpiry) {
        log.debug("Sending expiration push to user: {} for coupon: {}",
                sanitizer.sanitizeLogging(user.getEmail()),
                sanitizer.sanitizeLogging(coupon.getCode()));

        try {
            String title = "Coupon Expiring Soon! ⏰";
            String message = String.format("Your %s%% off coupon (%s) expires in %d day(s). Use it now!",
                    coupon.getDiscountPercentage().intValue(), coupon.getCode(), daysUntilExpiry);

            Map<String, String> data = new HashMap<>();
            data.put("couponCode", coupon.getCode());
            data.put("daysUntilExpiry", String.valueOf(daysUntilExpiry));
            data.put("discountPercentage", coupon.getDiscountPercentage().toString());

            notificationService.sendToUser(
                    user.getId(),
                    getExpiryNotificationType(daysUntilExpiry),
                    title,
                    message,
                    data);
        } catch (Exception e) {
            log.error("Failed to send expiration push to {}: {}",
                    sanitizer.sanitizeLogging(user.getEmail()), e.getMessage());
        }
    }

    // ========== Helper Methods ==========

    private Map<String, Object> buildCouponEmailModel(User user, Coupon coupon) {
        Map<String, Object> model = new HashMap<>();
        model.put("userName", user.getFirstName() != null ? user.getFirstName() : "User");
        model.put("couponCode", coupon.getCode());
        model.put("discountPercentage", coupon.getDiscountPercentage().intValue());
        model.put("validFrom", coupon.getValidFrom().format(DATE_FORMATTER));
        model.put("validUntil", coupon.getValidUntil().format(DATE_FORMATTER));
        model.put("description", coupon.getDescription());
        if (coupon.getMinPurchaseAmount() != null) {
            model.put("minPurchaseAmount", coupon.getMinPurchaseAmount());
        }
        return model;
    }

    private Map<String, Object> buildAdminExpirationEmailModel(Coupon coupon, int daysUntilExpiry) {
        Map<String, Object> model = new HashMap<>();
        model.put("couponCode", coupon.getCode());
        model.put("daysUntilExpiry", daysUntilExpiry);
        model.put("expiryDate", coupon.getValidUntil().format(DATE_FORMATTER));
        model.put("currentUsageCount", coupon.getCurrentUsageCount());
        model.put("maxUsageCount", coupon.getMaxUsageCount());
        model.put("discountPercentage", coupon.getDiscountPercentage().intValue());
        model.put("recipientCategory", coupon.getRecipientCategory().name());
        return model;
    }

    private NotificationType getExpiryNotificationType(int daysUntilExpiry) {
        return switch (daysUntilExpiry) {
            case 7 -> NotificationType.COUPON_EXPIRING_7_DAYS;
            case 3 -> NotificationType.COUPON_EXPIRING_3_DAYS;
            case 1 -> NotificationType.COUPON_EXPIRING_1_DAY;
            default -> NotificationType.COUPON_EXPIRING_7_DAYS; // fallback
        };
    }

    private List<User> determineRecipients(Coupon coupon) {
        RecipientCategory category = coupon.getRecipientCategory();

        return switch (category) {
            case ALL_PAID_USERS -> findAllPaidUsers();
            case INDIVIDUAL_PLAN -> findUsersByPlan("INDIVIDUAL");
            case TEAM_PLAN -> findUsersByPlan("TEAM");
            case ENTERPRISE_PLAN -> findUsersByPlan("ENTERPRISE");
            case FREE_TIER_ACTIVE -> findFreeTierActiveUsers();
            case EXPIRED_SUBSCRIPTION -> findExpiredSubscriptionUsers();
            case NEW_USERS -> findNewUsers();
            case HIGH_ACTIVITY_USERS -> findHighActivityUsers();
            case SPECIFIC_USERS -> couponRecipientRepository.findByCouponId(coupon.getId())
                    .stream()
                    .map(CouponRecipient::getUser)
                    .toList();
        };
    }

    private List<User> findAllPaidUsers() {
        return userSubscriptionRepository.findAll().stream()
                .filter(sub -> isSubscriptionActive(sub) &&
                        sub.getPlan().getName() != SubscriptionPlans.FREE)
                .map(UserSubscription::getUser)
                .toList();
    }

    private List<User> findUsersByPlan(String planName) {
        return userSubscriptionRepository.findAll().stream()
                .filter(sub -> isSubscriptionActive(sub) &&
                        planName.equalsIgnoreCase(sub.getPlan().getName().name()))
                .map(UserSubscription::getUser)
                .toList();
    }

    private List<User> findFreeTierActiveUsers() {
        return userSubscriptionRepository.findAll().stream()
                .filter(sub -> sub.getPlan().getName() == SubscriptionPlans.FREE &&
                        sub.getOcrPagesUsed() != null && sub.getOcrPagesUsed() >= 20)
                .map(UserSubscription::getUser)
                .toList();
    }

    private List<User> findExpiredSubscriptionUsers() {
        OffsetDateTime threeMonthsAgo = OffsetDateTime.now().minusMonths(3);
        return userSubscriptionRepository.findAll().stream()
                .filter(sub -> !isSubscriptionActive(sub) &&
                        sub.getCurrentPeriodEnd() != null &&
                        sub.getCurrentPeriodEnd().isAfter(threeMonthsAgo))
                .map(UserSubscription::getUser)
                .toList();
    }

    private List<User> findNewUsers() {
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        return userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(thirtyDaysAgo))
                .toList();
    }

    private List<User> findHighActivityUsers() {
        return userSubscriptionRepository.findAll().stream()
                .filter(sub -> {
                    Integer used = sub.getOcrPagesUsed();
                    Integer limit = sub.getPlan().getOcrPageLimit();
                    return used != null && limit != null && limit > 0 && (double) used / limit > 0.5;
                })
                .map(UserSubscription::getUser)
                .toList();
    }

    /**
     * Helper method to check if subscription is active based on status.
     */
    private boolean isSubscriptionActive(com.extractor.unraveldocs.subscription.model.UserSubscription subscription) {
        return "ACTIVE".equalsIgnoreCase(subscription.getStatus());
    }
}
