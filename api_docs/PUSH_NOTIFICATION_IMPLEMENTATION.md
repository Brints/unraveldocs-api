# Push Notification System Implementation Plan

A comprehensive real-time push notification system for UnravelDocs to keep users informed about important activities and system events.

## Overview

This implementation will add a **push notification system** that notifies users about:
- ✅ Document upload success/failure
- ✅ OCR processing success/failure
- ⚠️ Storage space running low (80%, 90%, 95% thresholds)
- 💳 Payment success/failure
- 📅 Subscription plan expiring soon (7-day, 3-day, 1-day warnings)
- 🔄 Subscription renewal reminders
- 👥 Team-related activities (for team members)

---

## Technology Stack

> [!IMPORTANT]
> **Multi-Provider Architecture**
> 
> This implementation uses a **provider-agnostic design** with three notification providers:
> 
> **Primary: Firebase Cloud Messaging (FCM)**
> - Free tier - No cost for sending push notifications
> - Cross-platform - Supports Android, iOS, and Web
> - Reliable delivery - Google's infrastructure ensures high delivery rates
> - Easy Spring Boot integration via Firebase Admin SDK
> 
> **Alternative 1: OneSignal**
> - Rich marketing features and analytics
> - Easy dashboard for managing campaigns
> - Generous free tier
> 
> **Alternative 2: AWS SNS**
> - Already in your AWS ecosystem
> - Omnichannel support (push, SMS, email)
> - Enterprise-grade scalability

Each provider is implemented in **separate packages** for clean organization, making it easy to switch providers in the future.

**Target Platforms:** Web (Angular), Android, iOS

---

## Database Schema

### V37__add_notification_tables.sql

New tables for storing notifications and user preferences:

```sql
-- Notification entity table
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

-- User device tokens for push notifications
CREATE TABLE user_device_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_token VARCHAR(512) NOT NULL,
    device_type VARCHAR(20) NOT NULL, -- ANDROID, IOS, WEB
    device_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_device_tokens_token ON user_device_tokens(device_token);
CREATE INDEX idx_device_tokens_user_id ON user_device_tokens(user_id);

-- User notification preferences
CREATE TABLE notification_preferences (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    push_enabled BOOLEAN DEFAULT TRUE,
    email_enabled BOOLEAN DEFAULT TRUE,
    document_notifications BOOLEAN DEFAULT TRUE,
    ocr_notifications BOOLEAN DEFAULT TRUE,
    payment_notifications BOOLEAN DEFAULT TRUE,
    storage_notifications BOOLEAN DEFAULT TRUE,
    subscription_notifications BOOLEAN DEFAULT TRUE,
    team_notifications BOOLEAN DEFAULT TRUE,
    quiet_hours_enabled BOOLEAN DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## Package Structure

```
pushnotification/
├── config/
│   ├── FirebaseConfig.java              # Firebase Admin SDK initialization
│   ├── OneSignalConfig.java             # OneSignal configuration
│   └── AwsSnsConfig.java                # AWS SNS configuration
├── controller/
│   └── NotificationController.java      # REST API endpoints
├── dto/
│   ├── request/
│   │   ├── RegisterDeviceRequest.java   # Device token registration
│   │   └── UpdatePreferencesRequest.java
│   └── response/
│       ├── NotificationResponse.java
│       └── NotificationPreferencesResponse.java
├── datamodel/
│   ├── NotificationType.java            # Enum of notification types
│   ├── DeviceType.java                  # ANDROID, IOS, WEB
│   └── NotificationProvider.java        # FCM, ONESIGNAL, AWS_SNS
├── kafka/
│   ├── NotificationKafkaProducer.java   # Publishes notifications to Kafka
│   └── NotificationKafkaConsumer.java   # Consumes and sends notifications
├── provider/
│   ├── NotificationProviderService.java # Provider interface
│   ├── firebase/
│   │   └── FirebaseNotificationProvider.java
│   ├── onesignal/
│   │   └── OneSignalNotificationProvider.java
│   └── sns/
│       └── AwsSnsNotificationProvider.java
├── impl/
│   ├── NotificationServiceImpl.java     # Main service (uses active provider)
│   ├── DeviceTokenServiceImpl.java
│   └── NotificationPreferencesServiceImpl.java
├── interfaces/
│   ├── NotificationService.java
│   ├── DeviceTokenService.java
│   └── NotificationPreferencesService.java
├── jobs/
│   ├── SubscriptionExpiryNotificationJob.java
│   └── StorageWarningNotificationJob.java
├── listener/
│   └── NotificationEventListener.java   # Listens for Kafka events
├── model/
│   ├── Notification.java                # JPA entity
│   ├── UserDeviceToken.java             # JPA entity
│   └── NotificationPreferences.java     # JPA entity
└── repository/
    ├── NotificationRepository.java
    ├── DeviceTokenRepository.java
    └── NotificationPreferencesRepository.java
```

---

## Notification Types

```java
public enum NotificationType {
    // Document events
    DOCUMENT_UPLOAD_SUCCESS,
    DOCUMENT_UPLOAD_FAILED,
    DOCUMENT_DELETED,
    
    // OCR events
    OCR_PROCESSING_STARTED,
    OCR_PROCESSING_COMPLETED,
    OCR_PROCESSING_FAILED,
    
    // Storage events
    STORAGE_WARNING_80,
    STORAGE_WARNING_90,
    STORAGE_WARNING_95,
    STORAGE_LIMIT_REACHED,
    
    // Payment events
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    
    // Subscription events
    SUBSCRIPTION_EXPIRING_7_DAYS,
    SUBSCRIPTION_EXPIRING_3_DAYS,
    SUBSCRIPTION_EXPIRING_1_DAY,
    SUBSCRIPTION_EXPIRED,
    SUBSCRIPTION_RENEWED,
    SUBSCRIPTION_UPGRADED,
    SUBSCRIPTION_DOWNGRADED,
    TRIAL_EXPIRING_SOON,
    TRIAL_EXPIRED,
    
    // Team events
    TEAM_INVITATION_RECEIVED,
    TEAM_MEMBER_ADDED,
    TEAM_MEMBER_REMOVED,
    TEAM_ROLE_CHANGED,
    
    // System
    SYSTEM_ANNOUNCEMENT,
    WELCOME
}
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/notifications/device` | Register device token |
| DELETE | `/api/v1/notifications/device/{tokenId}` | Unregister device |
| GET | `/api/v1/notifications` | Get user notifications (paginated) |
| GET | `/api/v1/notifications/unread-count` | Get unread notification count |
| PATCH | `/api/v1/notifications/{id}/read` | Mark notification as read |
| PATCH | `/api/v1/notifications/read-all` | Mark all as read |
| DELETE | `/api/v1/notifications/{id}` | Delete notification |
| GET | `/api/v1/notifications/preferences` | Get notification preferences |
| PUT | `/api/v1/notifications/preferences` | Update notification preferences |

---

## Configuration

### Environment Variables

```properties
# ==================== Push Notification Configuration ====================
# Active provider: FCM, ONESIGNAL, or AWS_SNS
NOTIFICATION_ACTIVE_PROVIDER=FCM

# ==================== Firebase Cloud Messaging ====================
FIREBASE_ENABLED=true
FIREBASE_CREDENTIALS_PATH=classpath:firebase-service-account.json
# Or use inline JSON (for Docker/CI environments)
# FIREBASE_CREDENTIALS_JSON={"type":"service_account","project_id":"..."}

# ==================== OneSignal ====================
ONESIGNAL_ENABLED=false
ONESIGNAL_APP_ID=your-onesignal-app-id
ONESIGNAL_API_KEY=your-onesignal-rest-api-key

# ==================== AWS SNS (uses existing AWS credentials) ====================
AWS_SNS_ENABLED=false
AWS_SNS_PLATFORM_APPLICATION_ARN=arn:aws:sns:region:account-id:app/platform/app-name
```

### Maven Dependency

```xml
<!-- Firebase Admin SDK for Push Notifications -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.7.0</version>
</dependency>
```

---

## Kafka Integration

Notifications are processed asynchronously via Kafka:

```java
// Producer publishes notification events to Kafka topic
public void publishNotification(NotificationEvent event) {
    kafkaTemplate.send("notification-events", event.getUserId(), event);
}

// Consumer processes and sends notifications
@KafkaListener(topics = "notification-events")
public void handleNotification(NotificationEvent event) {
    if (preferencesService.isNotificationTypeEnabled(event.getUserId(), event.getType())) {
        notificationService.sendToUser(event.getUserId(), event.getType(), 
            event.getTitle(), event.getMessage(), event.getData());
    }
}
```

---

## Scheduled Jobs

### SubscriptionExpiryNotificationJob

Runs daily to check for:
- Subscriptions expiring in 7 days → Send reminder
- Subscriptions expiring in 3 days → Send warning
- Subscriptions expiring in 1 day → Send urgent warning
- Trials ending soon → Send trial expiry notification

### StorageWarningNotificationJob

Runs daily to check user storage usage:
- 80% usage → Friendly reminder
- 90% usage → Warning
- 95% usage → Critical warning
- Tracked to avoid sending duplicate warnings

---

## Integration Points

| Service | Trigger | Notification Type |
|---------|---------|-------------------|
| `DocumentUploadService` | After upload completes | DOCUMENT_UPLOAD_SUCCESS/FAILED |
| `ProcessOcr` | After OCR completes | OCR_PROCESSING_COMPLETED/FAILED |
| `StorageAllocationService` | When checking storage | STORAGE_WARNING_* |
| `StripeWebhookService` | Payment events | PAYMENT_SUCCESS/FAILED |
| `PaystackWebhookService` | Payment events | PAYMENT_SUCCESS/FAILED |
| Subscription jobs | Period end approaching | SUBSCRIPTION_EXPIRING_* |
| Team services | Member changes | TEAM_MEMBER_ADDED/REMOVED |

---

## Setup Requirements

### Firebase Cloud Messaging (Primary)
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Generate a service account JSON file
3. Add the JSON file to `src/main/resources/firebase-service-account.json`
4. For mobile apps (future): Configure Android/iOS apps in Firebase Console

### OneSignal (Alternative)
1. Create account at [onesignal.com](https://onesignal.com)
2. Create an app and get App ID and REST API Key
3. Configure in `.env` file

### AWS SNS (Alternative)
1. Uses existing AWS credentials from your `.env`
2. Create platform applications in AWS Console for each platform
3. Configure ARN in `.env` file
