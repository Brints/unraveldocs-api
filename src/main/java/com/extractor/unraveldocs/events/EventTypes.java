package com.extractor.unraveldocs.events;

public class EventTypes {
    // User Events
    public static final String USER_REGISTERED = "UserRegistered";
    public static final String USER_DELETION_SCHEDULED = "UserDeletionScheduled";
    public static final String USER_DELETED = "UserDeleted";
    public static final String PASSWORD_CHANGED = "PasswordChanged";
    public static final String PASSWORD_RESET_REQUESTED = "PasswordResetRequested";
    public static final String PASSWORD_RESET_SUCCESSFUL = "PasswordResetSuccessful";
    public static final String WELCOME_EVENT = "WelcomeEvent";

    // OCR Events
    public static final String OCR_REQUESTED = "OcrRequested";

    private EventTypes() {} // Prevent instantiation
}
