package com.extractor.unraveldocs.pushnotification.datamodel;

/**
 * Enum representing all types of notifications that can be sent to users.
 * Users can configure their preferences for each category of notifications.
 */
public enum NotificationType {
    // Document events
    DOCUMENT_UPLOAD_SUCCESS("Document Upload Success", "document"),
    DOCUMENT_UPLOAD_FAILED("Document Upload Failed", "document"),
    DOCUMENT_DELETED("Document Deleted", "document"),

    // OCR events
    OCR_PROCESSING_STARTED("OCR Processing Started", "ocr"),
    OCR_PROCESSING_COMPLETED("OCR Processing Completed", "ocr"),
    OCR_PROCESSING_FAILED("OCR Processing Failed", "ocr"),

    // Storage events
    STORAGE_WARNING_80("Storage Warning 80%", "storage"),
    STORAGE_WARNING_90("Storage Warning 90%", "storage"),
    STORAGE_WARNING_95("Storage Warning 95%", "storage"),
    STORAGE_LIMIT_REACHED("Storage Limit Reached", "storage"),

    // Payment events
    PAYMENT_SUCCESS("Payment Success", "payment"),
    PAYMENT_FAILED("Payment Failed", "payment"),
    PAYMENT_REFUNDED("Payment Refunded", "payment"),

    // Subscription events
    SUBSCRIPTION_EXPIRING_7_DAYS("Subscription Expiring in 7 Days", "subscription"),
    SUBSCRIPTION_EXPIRING_3_DAYS("Subscription Expiring in 3 Days", "subscription"),
    SUBSCRIPTION_EXPIRING_1_DAY("Subscription Expiring Tomorrow", "subscription"),
    SUBSCRIPTION_EXPIRED("Subscription Expired", "subscription"),
    SUBSCRIPTION_RENEWED("Subscription Renewed", "subscription"),
    SUBSCRIPTION_UPGRADED("Subscription Upgraded", "subscription"),
    SUBSCRIPTION_DOWNGRADED("Subscription Downgraded", "subscription"),
    TRIAL_EXPIRING_SOON("Trial Expiring Soon", "subscription"),
    TRIAL_EXPIRED("Trial Expired", "subscription"),
    TRIAL_ACTIVATED("Trial Activated", "subscription"),

    // Team events
    TEAM_INVITATION_RECEIVED("Team Invitation Received", "team"),
    TEAM_MEMBER_ADDED("Team Member Added", "team"),
    TEAM_MEMBER_REMOVED("Team Member Removed", "team"),
    TEAM_ROLE_CHANGED("Team Role Changed", "team"),

    // Coupon events
    COUPON_RECEIVED("Coupon Received", "coupon"),
    COUPON_EXPIRING_7_DAYS("Coupon Expiring in 7 Days", "coupon"),
    COUPON_EXPIRING_3_DAYS("Coupon Expiring in 3 Days", "coupon"),
    COUPON_EXPIRING_1_DAY("Coupon Expiring Tomorrow", "coupon"),
    COUPON_EXPIRED("Coupon Expired", "coupon"),
    COUPON_APPLIED("Coupon Applied", "coupon"),

    // System
    SYSTEM_ANNOUNCEMENT("System Announcement", "system"),
    WELCOME("Welcome", "system");

    private final String displayName;
    private final String category;

    NotificationType(String displayName, String category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    /**
     * Check if this notification type belongs to a given category.
     */
    public boolean isCategory(String categoryName) {
        return this.category.equalsIgnoreCase(categoryName);
    }
}
