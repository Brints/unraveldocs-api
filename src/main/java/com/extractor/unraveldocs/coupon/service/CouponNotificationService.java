package com.extractor.unraveldocs.coupon.service;

import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.user.model.User;

import java.util.List;

/**
 * Service interface for coupon notification operations.
 */
public interface CouponNotificationService {

    /**
     * Notifies all eligible recipients about a new or updated coupon.
     * Determines recipients based on coupon's recipient category.
     */
    void notifyRecipients(Coupon coupon);

    /**
     * Notifies specific users about a coupon.
     */
    void notifyRecipients(Coupon coupon, List<User> recipients);

    /**
     * Sends a coupon notification email to a specific user.
     */
    void sendCouponEmail(User user, Coupon coupon);

    /**
     * Sends a coupon push notification to a specific user.
     */
    void sendCouponPushNotification(User user, Coupon coupon);

    /**
     * Notifies recipients about coupon expiration.
     */
    void sendExpirationNotifications(Coupon coupon, int daysUntilExpiry);

    /**
     * Notifies admin (coupon creator) about upcoming expiration.
     */
    void sendAdminExpirationNotification(Coupon coupon, int daysUntilExpiry);
}
