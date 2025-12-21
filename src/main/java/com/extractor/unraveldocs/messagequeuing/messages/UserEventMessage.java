package com.extractor.unraveldocs.messagequeuing.messages;

import java.time.Instant;
import java.util.Map;

/**
 * Message for user-related events.
 * Used to propagate user lifecycle events across services.
 *
 * @param eventType Type of user event
 * @param userId User ID
 * @param email User's email
 * @param eventData Additional event-specific data
 * @param eventTimestamp When the event occurred
 * @param ipAddress IP address where the event originated (for security events)
 * @param userAgent User agent string (for security events)
 */
public record UserEventMessage(
        UserEventType eventType,
        String userId,
        String email,
        Map<String, Object> eventData,
        Instant eventTimestamp,
        String ipAddress,
        String userAgent
) {
    
    public enum UserEventType {
        USER_REGISTERED,
        USER_VERIFIED,
        USER_LOGIN,
        USER_LOGOUT,
        USER_UPDATED,
        USER_DELETED,
        PASSWORD_CHANGED,
        PASSWORD_RESET_REQUESTED,
        PASSWORD_RESET_COMPLETED,
        ROLE_CHANGED,
        SUBSCRIPTION_CHANGED,
        SUSPICIOUS_ACTIVITY
    }
    
    /**
     * Create a user registration event.
     */
    public static UserEventMessage userRegistered(String userId, String email) {
        return new UserEventMessage(
                UserEventType.USER_REGISTERED,
                userId,
                email,
                Map.of(),
                Instant.now(),
                null,
                null
        );
    }
    
    /**
     * Create a user verification event.
     */
    public static UserEventMessage userVerified(String userId, String email) {
        return new UserEventMessage(
                UserEventType.USER_VERIFIED,
                userId,
                email,
                Map.of(),
                Instant.now(),
                null,
                null
        );
    }
    
    /**
     * Create a login event.
     */
    public static UserEventMessage loginEvent(
            String userId,
            String email,
            String ipAddress,
            String userAgent
    ) {
        return new UserEventMessage(
                UserEventType.USER_LOGIN,
                userId,
                email,
                Map.of(),
                Instant.now(),
                ipAddress,
                userAgent
        );
    }
    
    /**
     * Create a password change event.
     */
    public static UserEventMessage passwordChanged(String userId, String email) {
        return new UserEventMessage(
                UserEventType.PASSWORD_CHANGED,
                userId,
                email,
                Map.of(),
                Instant.now(),
                null,
                null
        );
    }
    
    /**
     * Create a suspicious activity event.
     */
    public static UserEventMessage suspiciousActivity(
            String userId,
            String email,
            String reason,
            String ipAddress,
            String userAgent
    ) {
        return new UserEventMessage(
                UserEventType.SUSPICIOUS_ACTIVITY,
                userId,
                email,
                Map.of("reason", reason),
                Instant.now(),
                ipAddress,
                userAgent
        );
    }
    
    /**
     * Add event data.
     */
    public UserEventMessage withEventData(Map<String, Object> eventData) {
        return new UserEventMessage(
                eventType,
                userId,
                email,
                eventData,
                eventTimestamp,
                ipAddress,
                userAgent
        );
    }
    
    /**
     * Add security context.
     */
    public UserEventMessage withSecurityContext(String ipAddress, String userAgent) {
        return new UserEventMessage(
                eventType,
                userId,
                email,
                eventData,
                eventTimestamp,
                ipAddress,
                userAgent
        );
    }
}
