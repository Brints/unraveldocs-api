package com.extractor.unraveldocs.brokers.rabbitmq.events;

public class EventTypes {
    // User Events
    public static final String USER_REGISTERED = "UserRegistered";
    public static final String USER_DELETION_SCHEDULED = "UserDeletionScheduled";
    public static final String USER_DELETED = "UserDeleted";
    public static final String PASSWORD_CHANGED = "PasswordChanged";
    public static final String PASSWORD_RESET_REQUESTED = "PasswordResetRequested";
    public static final String PASSWORD_RESET_SUCCESSFUL = "PasswordResetSuccessful";
    public static final String WELCOME_EVENT = "WelcomeEvent";
    public static final String ADMIN_CREATED = "AdminCreated";

    // OCR Events
    public static final String OCR_REQUESTED = "OcrRequested";

    // Elasticsearch Events
    public static final String ES_DOCUMENT_INDEX = "EsDocumentIndex";
    public static final String ES_USER_INDEX = "EsUserIndex";
    public static final String ES_PAYMENT_INDEX = "EsPaymentIndex";
    public static final String ES_SUBSCRIPTION_INDEX = "EsSubscriptionIndex";

    // Team Events
    public static final String TEAM_TRIAL_EXPIRING = "TeamTrialExpiring";
    public static final String TEAM_SUBSCRIPTION_CHARGED = "TeamSubscriptionCharged";
    public static final String TEAM_SUBSCRIPTION_FAILED = "TeamSubscriptionFailed";
    public static final String TEAM_CREATED = "TeamCreated";

    private EventTypes() {
    } // Prevent instantiation
}
