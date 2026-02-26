# Push Notification Package — Documentation

> **Package:** `com.extractor.unraveldocs.pushnotification`  
> **Base URL:** `/api/v1/notifications`  
> **Last Updated:** February 26, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Architecture](#architecture)
4. [Enums & Data Models](#enums--data-models)
   - [NotificationType](#notificationtype)
   - [NotificationProviderType](#notificationprovidertype)
   - [DeviceType](#devicetype)
5. [Database Entities](#database-entities)
   - [Notification](#notification)
   - [NotificationPreferences](#notificationpreferences)
   - [UserDeviceToken](#userdevicetoken)
   - [StorageWarningSent](#storagewarningsent)
6. [Configuration](#configuration)
   - [NotificationConfig](#notificationconfig)
   - [FirebaseConfig](#firebaseconfig)
   - [AwsSnsConfig & OneSignalConfig](#awssnsconfig--onesignalconfig)
7. [Kafka Integration](#kafka-integration)
   - [NotificationEvent](#notificationevent)
   - [NotificationKafkaProducer](#notificationkafkaproducer)
   - [NotificationKafkaConsumer](#notificationkafkaconsumer)
8. [Provider System](#provider-system)
   - [NotificationProviderService Interface](#notificationproviderservice-interface)
   - [FirebaseNotificationProvider](#firebasenotificationprovider)
   - [OneSignalNotificationProvider](#onesignalnotificationprovider)
   - [AwsSnsNotificationProvider](#awssnsnotificationprovider)
9. [Services](#services)
   - [NotificationService](#notificationservice)
   - [DeviceTokenService](#devicetokenservice)
   - [NotificationPreferencesService](#notificationpreferencesservice)
10. [Repositories](#repositories)
11. [DTOs](#dtos)
    - [Request DTOs](#request-dtos)
    - [Response DTOs](#response-dtos)
12. [Endpoints](#endpoints)
    - [Device Token Management](#device-token-management)
    - [Notification Inbox](#notification-inbox)
    - [Preference Management](#preference-management)
13. [Scheduled Jobs](#scheduled-jobs)
    - [StorageWarningNotificationJob](#storagewarningnotificationjob)
    - [SubscriptionExpiryNotificationJob](#subscriptionexpirynotificationjob)
14. [Configuration Reference](#configuration-reference)
15. [Flow Diagrams](#flow-diagrams)

---

## Overview

The **Push Notification** package provides a complete, multi-provider push notification system. It spans device registration, user-preference gating, quiet hours, Kafka-backed async delivery, persistent notification inbox, and scheduled proactive alerts.

| Capability | Description |
|---|---|
| **Multi-Provider** | Pluggable provider system — FCM (Firebase), OneSignal, AWS SNS. Active provider configured via a single property. |
| **Kafka-Backed Delivery** | Notifications are published to a Kafka topic and consumed asynchronously. Falls back to logging if Kafka is absent. |
| **Persistent Inbox** | Every delivered notification is stored in PostgreSQL for the user's notification history, with read/unread state. |
| **Preference Gating** | Per-category opt-in flags (document, OCR, payment, storage, subscription, team, coupon) + global push toggle. |
| **Quiet Hours** | User-configurable quiet windows; the consumer skips delivery if the user is in their quiet window. |
| **Device Registry** | Multi-device support per user (up to `maxDevicesPerUser`, default 10). Token transfer on re-registration. |
| **Scheduled Alerts** | Two cron jobs: storage usage warnings (80/90/95% thresholds with deduplication) and subscription expiry countdowns (7/3/1 days). |

---

## Package Structure

```
pushnotification/
├── config/
│   ├── AwsSnsConfig.java                    # AWS SNS client bean (@ConditionalOnProperty aws.sns.enabled)
│   ├── FirebaseConfig.java                  # Firebase Admin SDK init; loads credentials from env var or file
│   ├── NotificationConfig.java              # @ConfigurationProperties(prefix="notification") — active provider, retention, topic, etc.
│   └── OneSignalConfig.java                 # OneSignal REST client config (@ConditionalOnProperty onesignal.enabled)
├── controller/
│   └── NotificationController.java          # REST: device registration, notification inbox, preferences — all under /api/v1/notifications
├── datamodel/
│   ├── DeviceType.java                      # Enum: ANDROID | IOS | WEB
│   ├── NotificationProviderType.java        # Enum: FCM | ONESIGNAL | AWS_SNS (with displayName)
│   └── NotificationType.java                # Enum: 55 notification types across 8 categories (document, ocr, storage, payment, subscription, team, coupon, system)
├── dto/
│   ├── request/
│   │   ├── RegisterDeviceRequest.java       # deviceToken (@NotBlank, max 512), deviceType (@NotNull), deviceName (optional, max 100)
│   │   └── UpdatePreferencesRequest.java    # Per-category boolean flags + optional quiet hours fields
│   └── response/
│       ├── DeviceTokenResponse.java         # id, deviceToken, deviceType, deviceName, isActive, createdAt, lastUsedAt
│       ├── NotificationPreferencesResponse.java  # All preference flags + quietHours start/end + audit timestamps
│       └── NotificationResponse.java        # id, type, typeDisplayName, category, title, message, data, isRead, createdAt, readAt
├── impl/
│   ├── DeviceTokenServiceImpl.java          # Registers/deactivates device tokens; enforces maxDevicesPerUser; transfers stale tokens
│   ├── NotificationPreferencesServiceImpl.java  # Get/update preferences; lazy-creates defaults; isNotificationTypeEnabled + isInQuietHours
│   └── NotificationServiceImpl.java         # Send (single/batch/topic); get/count/mark/delete notifications from inbox
├── interfaces/
│   ├── DeviceTokenService.java              # Service interface for device token operations
│   ├── NotificationPreferencesService.java  # Service interface for preference operations
│   └── NotificationService.java            # Service interface for send + inbox operations
├── jobs/
│   ├── StorageWarningNotificationJob.java   # @Scheduled daily 10AM: 80/90/95% storage threshold warnings with deduplication
│   └── SubscriptionExpiryNotificationJob.java  # @Scheduled daily 9AM: 7/3/1-day expiry warnings + trial expiry
├── kafka/
│   ├── NotificationEvent.java              # Serializable Kafka message: id, userId, type, title, message, data, timestamp
│   ├── NotificationKafkaConsumer.java       # @KafkaListener: checks prefs + quiet hours → persists → sends via active provider
│   └── NotificationKafkaProducer.java       # Publishes NotificationEvent to Kafka topic (keyed by userId)
├── model/
│   ├── Notification.java                   # @Entity(notifications): id, user, type, title, message, data(jsonb), isRead, createdAt, readAt
│   ├── NotificationPreferences.java         # @Entity(notification_preferences): per-category flags, quietHours, one-to-one user
│   ├── StorageWarningSent.java              # @Entity(storage_warning_sent): unique(userId, warningLevel) — deduplication guard
│   └── UserDeviceToken.java                # @Entity(user_device_tokens): deviceToken (unique, 512), deviceType, isActive, lastUsedAt
├── provider/
│   ├── NotificationProviderService.java     # Interface: send(), sendBatch(), sendToTopic(), subscribeToTopic(), isEnabled()
│   ├── firebase/
│   │   └── FirebaseNotificationProvider.java  # FCM: single send, batch (500-token chunks), topic send; handles stale token cleanup
│   ├── onesignal/
│   │   └── OneSignalNotificationProvider.java # OneSignal REST API integration
│   └── sns/
│       └── AwsSnsNotificationProvider.java    # AWS SNS publish integration
├── repository/
│   ├── DeviceTokenRepository.java           # findByUserIdAndIsActiveTrue, findByDeviceToken, countByUserIdAndIsActiveTrue, deactivateAllForUser
│   ├── NotificationPreferencesRepository.java # findByUserId, existsByUserId
│   ├── NotificationRepository.java          # findByUserIdOrderByCreatedAtDesc, countByUserIdAndIsReadFalse, markAllAsRead, deleteOlderThan
│   └── StorageWarningSentRepository.java    # existsByUserIdAndWarningLevel (deduplication check)
└── documentation/
    └── api_docs.md                          # This file
```

---

## Architecture

```
Domain Services / Scheduled Jobs
  │
  └─ notificationService.sendToUser(userId, type, title, message, data)
         │
         ├─ kafkaProducer != null?
         │     └─ YES → NotificationKafkaProducer.publishNotification()
         │                   │
         │                   └─ kafkaTemplate.send(kafkaTopic, userId, NotificationEvent)
         │                         │
         │                         ▼
         │              Kafka Topic: notification-events (keyed by userId)
         │                         │
         │                         ▼
         │              NotificationKafkaConsumer (@KafkaListener)
         │                         │
         │              ┌──────────▼────────────────────────────┐
         │              │ 1. Check preference: isEnabled(type)?  │
         │              │ 2. Check quiet hours?                   │
         │              │ 3. Persist to notifications table       │
         │              │ 4. Get active device tokens             │
         │              │ 5. provider.sendBatch(tokens, ...)      │
         │              └──────────┬────────────────────────────┘
         │                         │
         │              ┌──────────▼────────────────┐
         │              │  Active Provider            │
         │              │  FCM / OneSignal / AWS SNS  │
         │              └───────────────────────────┘
         │
         └─ kafkaProducer == null? → log only (graceful degradation)

User/Admin REST API
  │
  ├─ Device Registration → DeviceTokenService → user_device_tokens table
  ├─ Notification Inbox  → NotificationService → notifications table
  └─ Preferences         → NotificationPreferencesService → notification_preferences table
```

---

## Enums & Data Models

### `NotificationType`
**Package:** `com.extractor.unraveldocs.pushnotification.datamodel`

55 typed notification events across 8 categories. Each value carries a human-readable `displayName` and a `category` string used for preference gating.

**Category: `document`**

| Constant | Display Name |
|---|---|
| `DOCUMENT_UPLOAD_SUCCESS` | Document Upload Success |
| `DOCUMENT_UPLOAD_FAILED` | Document Upload Failed |
| `DOCUMENT_DELETED` | Document Deleted |

**Category: `ocr`**

| Constant | Display Name |
|---|---|
| `OCR_PROCESSING_STARTED` | OCR Processing Started |
| `OCR_PROCESSING_COMPLETED` | OCR Processing Completed |
| `OCR_PROCESSING_FAILED` | OCR Processing Failed |

**Category: `storage`**

| Constant | Display Name |
|---|---|
| `STORAGE_WARNING_80` | Storage Warning 80% |
| `STORAGE_WARNING_90` | Storage Warning 90% |
| `STORAGE_WARNING_95` | Storage Warning 95% |
| `STORAGE_LIMIT_REACHED` | Storage Limit Reached |

**Category: `payment`**

| Constant | Display Name |
|---|---|
| `PAYMENT_SUCCESS` | Payment Success |
| `PAYMENT_FAILED` | Payment Failed |
| `PAYMENT_REFUNDED` | Payment Refunded |

**Category: `subscription`**

| Constant | Display Name |
|---|---|
| `SUBSCRIPTION_EXPIRING_7_DAYS` | Subscription Expiring in 7 Days |
| `SUBSCRIPTION_EXPIRING_3_DAYS` | Subscription Expiring in 3 Days |
| `SUBSCRIPTION_EXPIRING_1_DAY` | Subscription Expiring Tomorrow |
| `SUBSCRIPTION_EXPIRED` | Subscription Expired |
| `SUBSCRIPTION_RENEWED` | Subscription Renewed |
| `SUBSCRIPTION_UPGRADED` | Subscription Upgraded |
| `SUBSCRIPTION_DOWNGRADED` | Subscription Downgraded |
| `TRIAL_EXPIRING_SOON` | Trial Expiring Soon |
| `TRIAL_EXPIRED` | Trial Expired |
| `TRIAL_ACTIVATED` | Trial Activated |

**Category: `team`**

| Constant | Display Name |
|---|---|
| `TEAM_INVITATION_RECEIVED` | Team Invitation Received |
| `TEAM_MEMBER_ADDED` | Team Member Added |
| `TEAM_MEMBER_REMOVED` | Team Member Removed |
| `TEAM_ROLE_CHANGED` | Team Role Changed |

**Category: `coupon`**

| Constant | Display Name |
|---|---|
| `COUPON_RECEIVED` | Coupon Received |
| `COUPON_EXPIRING_7_DAYS` | Coupon Expiring in 7 Days |
| `COUPON_EXPIRING_3_DAYS` | Coupon Expiring in 3 Days |
| `COUPON_EXPIRING_1_DAY` | Coupon Expiring Tomorrow |
| `COUPON_EXPIRED` | Coupon Expired |
| `COUPON_APPLIED` | Coupon Applied |

**Category: `credit`**

| Constant | Display Name |
|---|---|
| `CREDIT_PURCHASE_SUCCESS` | Credit Purchase Success |
| `CREDIT_BALANCE_LOW` | Credit Balance Low |
| `CREDIT_TRANSFER_SENT` | Credit Transfer Sent |
| `CREDIT_TRANSFER_RECEIVED` | Credit Transfer Received |

**Category: `system`**

| Constant | Display Name |
|---|---|
| `SYSTEM_ANNOUNCEMENT` | System Announcement |
| `WELCOME` | Welcome |

---

### `NotificationProviderType`

| Value | Display Name | Conditional Property |
|---|---|---|
| `FCM` | Firebase Cloud Messaging | `firebase.enabled=true` |
| `ONESIGNAL` | OneSignal | `onesignal.enabled=true` |
| `AWS_SNS` | AWS Simple Notification Service | `aws.sns.enabled=true` |

---

### `DeviceType`

| Value | Description |
|---|---|
| `ANDROID` | Android mobile device |
| `IOS` | Apple iOS device |
| `WEB` | Web browser (WebPush) |

---

## Database Entities

### `Notification`
**Table:** `notifications`  
**Indexes:** `user_id`, `is_read`, `created_at`, `type`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | PK, auto-generated | Notification UUID |
| `user_id` | `UUID` | FK → users, NOT NULL | Owner user |
| `type` | `VARCHAR(50)` | NOT NULL | `NotificationType` enum |
| `title` | `VARCHAR` | NOT NULL | Notification title |
| `message` | `TEXT` | NOT NULL | Full message body |
| `data` | `JSONB` | nullable | Extra key-value payload (e.g., `{"documentId": "..."}`) |
| `is_read` | `BOOLEAN` | NOT NULL, default `false` | Read state |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, auto-set | Creation timestamp |
| `read_at` | `TIMESTAMPTZ` | nullable | When the notification was marked read |

**Business methods:**
- `markAsRead()` — sets `isRead = true` and `readAt = OffsetDateTime.now()`

---

### `NotificationPreferences`
**Table:** `notification_preferences`  
**Relationship:** One-to-one with `User`

| Column | Type | Default | Description |
|---|---|---|---|
| `id` | `UUID` | PK | Preferences record UUID |
| `user_id` | `UUID` | FK unique | Owning user |
| `push_enabled` | `BOOLEAN` | `true` | Master push notification toggle |
| `email_enabled` | `BOOLEAN` | `true` | Email notification toggle |
| `document_notifications` | `BOOLEAN` | `true` | document category toggle |
| `ocr_notifications` | `BOOLEAN` | `true` | ocr category toggle |
| `payment_notifications` | `BOOLEAN` | `true` | payment category toggle |
| `storage_notifications` | `BOOLEAN` | `true` | storage category toggle |
| `subscription_notifications` | `BOOLEAN` | `true` | subscription category toggle |
| `team_notifications` | `BOOLEAN` | `true` | team category toggle |
| `coupon_notifications` | `BOOLEAN` | `true` | coupon category toggle |
| `quiet_hours_enabled` | `BOOLEAN` | `false` | Quiet window toggle |
| `quiet_hours_start` | `TIME` | nullable | Start of quiet window |
| `quiet_hours_end` | `TIME` | nullable | End of quiet window |
| `created_at` | `TIMESTAMPTZ` | auto | Creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | auto | Last update timestamp |

**Business methods:**
- `isNotificationTypeEnabled(NotificationType)` — returns `false` if `pushEnabled=false`; otherwise checks the matching category flag via `type.getCategory()` switch.
- `isInQuietHours()` — checks if current local time falls within `quietHoursStart` to `quietHoursEnd` (handles midnight wraparound).

**Category → column mapping:**

| `type.getCategory()` | Preference Field |
|---|---|
| `"document"` | `documentNotifications` |
| `"ocr"` | `ocrNotifications` |
| `"payment"` | `paymentNotifications` |
| `"storage"` | `storageNotifications` |
| `"subscription"` | `subscriptionNotifications` |
| `"team"` | `teamNotifications` |
| `"coupon"` | `couponNotifications` |
| `"system"` | always `true` (system messages bypass preferences) |

---

### `UserDeviceToken`
**Table:** `user_device_tokens`  
**Indexes:** `user_id`, `is_active`  
**Constraint:** `device_token` is `UNIQUE` across the entire table (one token = one user at a time)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | PK | Token record UUID |
| `user_id` | `UUID` | FK → users | Owning user |
| `device_token` | `VARCHAR(512)` | NOT NULL, UNIQUE | FCM/APNs/OneSignal token |
| `device_type` | `VARCHAR(20)` | NOT NULL | `ANDROID`, `IOS`, `WEB` |
| `device_name` | `VARCHAR(100)` | nullable | Human-friendly device name |
| `is_active` | `BOOLEAN` | NOT NULL, default `true` | Active state |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, auto | Registration timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, auto | Last update |
| `last_used_at` | `TIMESTAMPTZ` | nullable | Last notification delivery |

**Business methods:**
- `updateLastUsed()` — sets `lastUsedAt = OffsetDateTime.now()`
- `deactivate()` — sets `isActive = false`

---

### `StorageWarningSent`
**Table:** `storage_warning_sent`  
**Unique Constraint:** `(user_id, warning_level)` — prevents duplicate warnings per threshold per user

| Column | Type | Description |
|---|---|---|
| `id` | `UUID` | PK |
| `user_id` | `UUID` | FK → users |
| `warning_level` | `INTEGER` | `80`, `90`, or `95` |
| `sent_at` | `TIMESTAMPTZ` | When the warning was sent |

> **Purpose:** The `StorageWarningNotificationJob` checks `existsByUserIdAndWarningLevel()` before sending each threshold warning. If a record exists, the warning is skipped — preventing repeated alerts at the same threshold.

---

## Configuration

### `NotificationConfig`
**Prefix:** `notification`

| Property | Type | Default | Description |
|---|---|---|---|
| `notification.activeProvider` | `NotificationProviderType` | `FCM` | Which provider handles delivery |
| `notification.persistNotifications` | `boolean` | `true` | Whether to store notifications in the database |
| `notification.respectQuietHours` | `boolean` | `true` | Whether the consumer skips delivery during quiet windows |
| `notification.maxDevicesPerUser` | `int` | `10` | Maximum device tokens per user |
| `notification.notificationRetentionDays` | `int` | `90` | Days before notifications are eligible for cleanup |
| `notification.kafkaTopic` | `String` | `"notification-events"` | Kafka topic name for notification events |

---

### `FirebaseConfig`
**Conditional:** `@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")`

Initialises the Firebase Admin SDK on `@PostConstruct`. Credential resolution priority:

```
1. firebase.credentials.json  (inline JSON string — recommended for containers/CI)
      ↓ if blank
2. firebase.credentials.path  (file path — "classpath:..." or "file:..." or absolute)
      ↓ if blank
3. Application Default Credentials (ADC — for GCP-hosted environments)
```

Registers a `FirebaseMessaging` bean used by `FirebaseNotificationProvider`.

---

### `AwsSnsConfig & OneSignalConfig`
Both follow the same pattern — conditional on their respective `*.enabled` property, they expose a client bean that is injected into the corresponding provider implementation.

---

## Kafka Integration

### `NotificationEvent`
**Package:** `com.extractor.unraveldocs.pushnotification.kafka`

Serializable Kafka message envelope.

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Auto-generated UUID |
| `userId` | `String` | Recipient user ID (also used as Kafka message key) |
| `type` | `NotificationType` | Notification type enum |
| `title` | `String` | Notification title |
| `message` | `String` | Notification message body |
| `data` | `Map<String, String>` | Extra payload key-value pairs |
| `timestamp` | `Instant` | When the event was created |

**Factory method:** `NotificationEvent.create(userId, type, title, message, data)` — generates UUID and current timestamp.

---

### `NotificationKafkaProducer`
**Conditional:** `@ConditionalOnBean(KafkaTemplate.class)`

| Method | Description |
|---|---|
| `publishNotification(userId, type, title, message, data)` | Builds `NotificationEvent` and calls `publishEvent()` |
| `publishNotifications(userIds, type, title, message, data)` | Loops over each userId and calls `publishNotification()` |
| `publishEvent(NotificationEvent)` | Calls `kafkaTemplate.send(kafkaTopic, userId, event)` — keyed by `userId` for partition ordering |

**Send:** Async `whenComplete` callback — logs success at DEBUG, errors at ERROR. Does not throw; failures are swallowed to avoid blocking the calling service.

---

### `NotificationKafkaConsumer`
**Topic:** Configured via `${notification.kafka-topic:notification-events}`  
**Group ID:** `notification-processor`  
**Conditional:** `@ConditionalOnBean(KafkaTemplate.class)`

**Processing pipeline per event:**

```
1. preferencesService.isNotificationTypeEnabled(userId, type)
      └─ false → skip (log debug)

2. config.respectQuietHours && preferencesService.isInQuietHours(userId)
      └─ true → skip (log debug)

3. config.persistNotifications
      └─ true → persistNotification(event)
                    → userRepository.findById(userId)
                    → save Notification entity to database

4. providers.get(config.activeProvider)
      └─ null or !enabled → log warn, return

5. deviceTokenService.getActiveTokenEntities(userId)
      └─ empty → log debug, return

6. provider.sendBatch(tokenStrings, title, message, data)
      └─ log info with sent count
```

**Provider map:** Built at startup from all `NotificationProviderService` beans where `isEnabled() == true`. Key = `NotificationProviderType.valueOf(provider.getProviderName())`.

> **Error handling:** All exceptions are caught and logged at ERROR level. The consumer does **not** rethrow — notification failures never trigger Kafka retries or DLQ routing (fire-and-forget semantics).

---

## Provider System

### `NotificationProviderService` Interface

| Method | Return | Description |
|---|---|---|
| `send(deviceToken, title, message, data)` | `boolean` | Send to a single device; `true` = success |
| `sendBatch(deviceTokens, title, message, data)` | `int` | Send to multiple devices; returns success count |
| `sendToTopic(topic, title, message, data)` | `boolean` | Send to a subscribed topic |
| `subscribeToTopic(deviceToken, topic)` | `boolean` | Subscribe device to an FCM topic |
| `unsubscribeFromTopic(deviceToken, topic)` | `boolean` | Unsubscribe device from an FCM topic |
| `getProviderName()` | `String` | Returns provider type name (matches `NotificationProviderType` enum names) |
| `isEnabled()` | `boolean` | Whether the provider is configured and ready |

---

### `FirebaseNotificationProvider`
**Conditional:** `@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")`  
**Provider Name:** `"FCM"`

**`send()`:** Builds an FCM `Message` with `Notification` (title/body), data payload, `AndroidConfig`, `ApnsConfig`, `WebpushConfig`. Calls `firebaseMessaging.send(message)`.

**`sendBatch()`:** Splits token list into chunks of **500** (FCM hard limit per `sendEachForMulticast` call). For each chunk, builds `MulticastMessage` and calls `firebaseMessaging.sendEachForMulticast()`. Handles individual token failures from `BatchResponse` (stale token deactivation).

**`sendToTopic()`:** Builds FCM `Message` with `.setTopic(topic)` instead of a device token.

**Platform configs applied to every message:**
- **Android:** High priority, custom channel
- **APNs (iOS):** Badge count, sound, content-available flag
- **Webpush:** Actions, icon, badge

**Error handling:** `FirebaseMessagingException` — logs error; for `UNREGISTERED` error code, deactivates the stale device token from the database.

---

### `OneSignalNotificationProvider`
**Conditional:** `@ConditionalOnProperty(name = "onesignal.enabled", havingValue = "true")`  
**Provider Name:** `"ONESIGNAL"`

Uses OneSignal's REST API (`/notifications` endpoint) to send push notifications. Supports single send, batch (converted to external user IDs), and topic/segment-based sending.

---

### `AwsSnsNotificationProvider`
**Conditional:** `@ConditionalOnProperty(name = "aws.sns.enabled", havingValue = "true")`  
**Provider Name:** `"AWS_SNS"`

Uses AWS SDK `SnsClient.publish()` for individual sends. For batch operations, publishes to an SNS topic ARN. Topics correspond to device type segments.

---

## Services

### `NotificationService`
**Interface:** `com.extractor.unraveldocs.pushnotification.interfaces.NotificationService`  
**Implementation:** `NotificationServiceImpl`

| Method | Description |
|---|---|
| `sendToUser(userId, type, title, message, data)` | Publishes single-user `NotificationEvent` to Kafka (or logs if Kafka absent) |
| `sendToUsers(userIds, type, title, message, data)` | Publishes batch events; calls `kafkaProducer.publishNotifications()` |
| `sendToTopic(topic, type, title, message, data)` | Reserved for topic-based sends (implementation pending) |
| `getUserNotifications(userId, Pageable)` | Returns paginated `Page<NotificationResponse>` ordered by `createdAt` DESC |
| `getNotificationsByType(userId, type, Pageable)` | Returns paginated notifications filtered by `NotificationType` |
| `getUnreadNotifications(userId, Pageable)` | Returns unread notifications for user |
| `getUnreadCount(userId)` | Returns `long` count of unread notifications |
| `markAsRead(userId, notificationId)` | Marks notification as read — validates ownership before updating |
| `markAllAsRead(userId)` | Bulk-updates all unread notifications for user via JPQL `@Modifying` query |
| `deleteNotification(userId, notificationId)` | Deletes notification — validates ownership before deleting |
| `isNotificationTypeEnabled(userId, type)` | Delegates to `NotificationPreferencesService` |

> **Kafka fallback:** `NotificationKafkaProducer` is injected with `@Autowired(required = false)`. If Kafka is not configured, `kafkaProducer == null` and `sendToUser` / `sendToUsers` log a debug message instead.

---

### `DeviceTokenService`
**Interface:** `com.extractor.unraveldocs.pushnotification.interfaces.DeviceTokenService`  
**Implementation:** `DeviceTokenServiceImpl`

| Method | Description |
|---|---|
| `registerDevice(userId, RegisterDeviceRequest)` | Register or re-activate device token; enforces `maxDevicesPerUser`; transfers token if it belongs to another user |
| `unregisterDevice(userId, tokenId)` | Deactivates token by ID — validates user ownership |
| `unregisterByToken(deviceToken)` | Deactivates token by raw token string (e.g., on FCM `UNREGISTERED` error) |
| `getActiveDevices(userId)` | Returns `List<DeviceTokenResponse>` for active tokens only |
| `getAllDevices(userId)` | Returns all tokens (active + inactive) |
| `getActiveTokenEntities(userId)` | Returns raw `List<UserDeviceToken>` — used internally by consumer |
| `updateLastUsed(deviceToken)` | Updates `lastUsedAt` timestamp on the token entity |
| `deactivateAllDevices(userId)` | Bulk-deactivates all tokens for a user (e.g., on account deletion) |
| `isTokenActive(deviceToken)` | Returns `true` if the token exists and `isActive = true` |

**`registerDevice()` flow:**
```
1. Find user by userId (throws IllegalArgumentException if not found)
2. deviceTokenRepository.findByDeviceToken(request.deviceToken)
      └─ Exists?
            ├─ Different user? → transfer to current user
            └─ Reactivate + update deviceName + updateLastUsed()
3. Does not exist?
      ├─ Count active devices ≥ maxDevicesPerUser?
      │     └─ throw IllegalStateException("Maximum number of devices reached")
      └─ Create new UserDeviceToken and save
```

---

### `NotificationPreferencesService`
**Interface:** `com.extractor.unraveldocs.pushnotification.interfaces.NotificationPreferencesService`  
**Implementation:** `NotificationPreferencesServiceImpl`

| Method | Description |
|---|---|
| `getPreferences(userId)` | Gets (or creates default) preferences and maps to `NotificationPreferencesResponse` |
| `updatePreferences(userId, UpdatePreferencesRequest)` | Updates all preference fields; quiet hours fields are optional (only updated if non-null) |
| `isNotificationTypeEnabled(userId, type)` | Returns `true` if no preferences exist (fail-open default) |
| `isInQuietHours(userId)` | Returns `false` if no preferences exist |
| `isPushEnabled(userId)` | Returns `true` if no preferences exist |
| `createDefaultPreferences(userId)` | Idempotent — only creates if `existsByUserId = false` |

**Default preferences (all enabled, quiet hours off):** Created lazily on first `getPreferences()` or `updatePreferences()` call if no record exists, or explicitly via `createDefaultPreferences()` at user registration.

---

## Repositories

### `NotificationRepository`

| Method | Description |
|---|---|
| `findByUserIdOrderByCreatedAtDesc(userId, Pageable)` | Paginated inbox (newest first) |
| `findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, Pageable)` | Paginated inbox filtered by type |
| `findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, Pageable)` | Paginated unread inbox |
| `countByUserIdAndIsReadFalse(userId)` | Unread badge count |
| `markAllAsRead(userId, readAt)` | `@Modifying` JPQL bulk update; returns affected row count |
| `deleteOlderThan(cutoffDate)` | `@Modifying` JPQL bulk delete for cleanup jobs |
| `findByUserIdAndTypeAndCreatedAtAfter(userId, type, after)` | Deduplication check (recent notifications of same type) |

### `DeviceTokenRepository`

| Method | Description |
|---|---|
| `findByUserIdAndIsActiveTrue(userId)` | Active tokens for sending |
| `findByDeviceToken(deviceToken)` | Token lookup by raw string |
| `findByUserIdAndDeviceToken(userId, deviceToken)` | Token lookup scoped to user |
| `findByUserId(userId)` | All tokens (active + inactive) |
| `findByUserIdAndDeviceTypeAndIsActiveTrue(userId, type)` | Active tokens filtered by device type |
| `countByUserIdAndIsActiveTrue(userId)` | Device count for `maxDevicesPerUser` check |
| `deactivateAllForUser(userId)` | `@Modifying` JPQL bulk deactivation |

### `NotificationPreferencesRepository`
- `findByUserId(userId)` → `Optional<NotificationPreferences>`
- `existsByUserId(userId)` → `boolean`

### `StorageWarningSentRepository`
- `existsByUserIdAndWarningLevel(userId, warningLevel)` → `boolean` (deduplication guard)

---

## DTOs

### Request DTOs

#### `RegisterDeviceRequest`

| Field | Type | Constraints | Description |
|---|---|---|---|
| `deviceToken` | `String` | `@NotBlank`, max 512 | FCM/APNs/OneSignal token |
| `deviceType` | `DeviceType` | `@NotNull` | `ANDROID`, `IOS`, or `WEB` |
| `deviceName` | `String` | optional, max 100 | Human-friendly label (e.g., "My iPhone 15") |

#### `UpdatePreferencesRequest`

| Field | Type | Constraints | Description |
|---|---|---|---|
| `pushEnabled` | `Boolean` | `@NotNull` | Master push toggle |
| `emailEnabled` | `Boolean` | `@NotNull` | Email notification toggle |
| `documentNotifications` | `Boolean` | `@NotNull` | document category |
| `ocrNotifications` | `Boolean` | `@NotNull` | ocr category |
| `paymentNotifications` | `Boolean` | `@NotNull` | payment category |
| `storageNotifications` | `Boolean` | `@NotNull` | storage category |
| `subscriptionNotifications` | `Boolean` | `@NotNull` | subscription category |
| `teamNotifications` | `Boolean` | `@NotNull` | team category |
| `couponNotifications` | `Boolean` | `@NotNull` | coupon category |
| `quietHoursEnabled` | `Boolean` | optional | Quiet window master toggle |
| `quietHoursStart` | `LocalTime` | optional | Start of quiet window (e.g., `22:00`) |
| `quietHoursEnd` | `LocalTime` | optional | End of quiet window (e.g., `07:00`) |

---

### Response DTOs

#### `NotificationResponse`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Notification UUID |
| `type` | `NotificationType` | Enum value |
| `typeDisplayName` | `String` | Human-readable type name |
| `category` | `String` | Category string (`"document"`, `"payment"`, etc.) |
| `title` | `String` | Notification title |
| `message` | `String` | Message body |
| `data` | `Map<String, String>` | Extra payload |
| `isRead` | `boolean` | Read state |
| `createdAt` | `OffsetDateTime` | Creation timestamp |
| `readAt` | `OffsetDateTime` | When marked read (nullable) |

#### `DeviceTokenResponse`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Token record UUID |
| `deviceToken` | `String` | The raw token string |
| `deviceType` | `DeviceType` | `ANDROID`, `IOS`, `WEB` |
| `deviceName` | `String` | Optional friendly name |
| `isActive` | `boolean` | Active state |
| `createdAt` | `OffsetDateTime` | Registration time |
| `lastUsedAt` | `OffsetDateTime` | Last delivery time (nullable) |

#### `NotificationPreferencesResponse`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Preferences record UUID |
| `pushEnabled` | `boolean` | Master push toggle |
| `emailEnabled` | `boolean` | Email toggle |
| `documentNotifications` | `boolean` | document category |
| `ocrNotifications` | `boolean` | ocr category |
| `paymentNotifications` | `boolean` | payment category |
| `storageNotifications` | `boolean` | storage category |
| `subscriptionNotifications` | `boolean` | subscription category |
| `teamNotifications` | `boolean` | team category |
| `couponNotifications` | `boolean` | coupon category |
| `quietHoursEnabled` | `boolean` | Quiet window enabled |
| `quietHoursStart` | `LocalTime` | Quiet start (nullable) |
| `quietHoursEnd` | `LocalTime` | Quiet end (nullable) |
| `createdAt` | `OffsetDateTime` | Record creation |
| `updatedAt` | `OffsetDateTime` | Last update |

---

## Endpoints

**Base path:** `/api/v1/notifications`  
**Auth:** All endpoints require an authenticated user (`@AuthenticationPrincipal User`)

### Device Token Management

#### POST `/api/v1/notifications/device` — Register Device
Register a device token for push notifications.

**Request body:**
```json
{
  "deviceToken": "fcm-token-string...",
  "deviceType": "ANDROID",
  "deviceName": "Pixel 9 Pro"
}
```

**Response — `201 Created`:** `DeviceTokenResponse`
```json
{
  "id": "token-uuid",
  "deviceToken": "fcm-token-string...",
  "deviceType": "ANDROID",
  "deviceName": "Pixel 9 Pro",
  "isActive": true,
  "createdAt": "2026-02-26T10:00:00Z",
  "lastUsedAt": null
}
```

**Errors:**
- `400` — Validation failure (blank token, missing deviceType)
- `409` (via `IllegalStateException`) — Max devices limit reached

---

#### DELETE `/api/v1/notifications/device/{tokenId}` — Unregister Device

| Parameter | Type | Description |
|---|---|---|
| `tokenId` | `String` (path) | Token record UUID |

**Response — `204 No Content`**  
**Note:** Ownership validated — silently ignores if token doesn't belong to the user.

---

#### GET `/api/v1/notifications/devices` — Get Registered Devices
Returns all **active** device tokens for the authenticated user.

**Response — `200 OK`:** `List<DeviceTokenResponse>`

---

### Notification Inbox

#### GET `/api/v1/notifications` — Get All Notifications

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Page number |
| `size` | `int` | `20` | Page size |

**Response — `200 OK`:** `Page<NotificationResponse>` (newest first)

---

#### GET `/api/v1/notifications/unread` — Get Unread Notifications

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Page number |
| `size` | `int` | `20` | Page size |

**Response — `200 OK`:** `Page<NotificationResponse>` (unread only, newest first)

---

#### GET `/api/v1/notifications/by-type/{type}` — Get by Type

| Parameter | Type | Description |
|---|---|---|
| `type` | `NotificationType` (path) | Notification type enum value |
| `page` | `int` | Default `0` |
| `size` | `int` | Default `20` |

**Response — `200 OK`:** `Page<NotificationResponse>`

---

#### GET `/api/v1/notifications/unread-count` — Get Unread Count

**Response — `200 OK`:**
```json
{ "count": 7 }
```

---

#### PATCH `/api/v1/notifications/{id}/read` — Mark Single as Read

| Parameter | Type | Description |
|---|---|---|
| `id` | `String` (path) | Notification UUID |

**Response — `204 No Content`**

---

#### PATCH `/api/v1/notifications/read-all` — Mark All as Read

**Response — `204 No Content`**

---

#### DELETE `/api/v1/notifications/{id}` — Delete Notification

| Parameter | Type | Description |
|---|---|---|
| `id` | `String` (path) | Notification UUID |

**Response — `204 No Content`**  
**Note:** Ownership validated — silently ignores if notification doesn't belong to the user.

---

### Preference Management

#### GET `/api/v1/notifications/preferences` — Get Preferences

**Response — `200 OK`:** `NotificationPreferencesResponse`  
**Note:** Creates default preferences (all enabled) if none exist.

---

#### PUT `/api/v1/notifications/preferences` — Update Preferences

**Request body:** `UpdatePreferencesRequest`
```json
{
  "pushEnabled": true,
  "emailEnabled": true,
  "documentNotifications": true,
  "ocrNotifications": false,
  "paymentNotifications": true,
  "storageNotifications": true,
  "subscriptionNotifications": true,
  "teamNotifications": true,
  "couponNotifications": false,
  "quietHoursEnabled": true,
  "quietHoursStart": "22:00:00",
  "quietHoursEnd": "07:00:00"
}
```

**Response — `200 OK`:** `NotificationPreferencesResponse`

---

## Scheduled Jobs

### `StorageWarningNotificationJob`
**Condition:** `@ConditionalOnProperty(name = "spring.scheduling.enabled", havingValue = "true", matchIfMissing = true)`  
**Schedule:** `@Scheduled(cron = "0 0 10 * * *")` — daily at **10:00 AM**

Iterates over all `UserSubscription` records and checks storage usage against three thresholds. Uses `StorageWarningSent` as a deduplication guard — once a warning at a given level has been sent, it is never resent until the record is cleared.

**Threshold logic (checked highest-first):**

| Threshold | Notification Type | Title | Message |
|---|---|---|---|
| ≥ 95% | `STORAGE_WARNING_95` | "Storage Almost Full!" | "You've used 95% of your storage..." |
| ≥ 90% | `STORAGE_WARNING_90` | "Storage Warning" | "You've used 90% of your storage..." |
| ≥ 80% | `STORAGE_WARNING_80` | "Storage Warning" | "You're using 80% of your storage..." |

**`sendWarningIfNotSent()` flow:**
```
1. storageWarningSentRepository.existsByUserIdAndWarningLevel(userId, level)
      └─ true → skip (already sent for this threshold)
2. notificationService.sendToUser(userId, type, title, message, data)
3. storageWarningSentRepository.save(StorageWarningSent{userId, level})
4. return 1 (warning sent)
```

**Data payload:** `{"storageUsed": "...", "storageLimit": "...", "usagePercent": "..."}`

**Skip conditions:** `storageLimit == null`, `storageLimit <= 0`, or `storageUsed == null`.

---

### `SubscriptionExpiryNotificationJob`
**Condition:** `@ConditionalOnProperty(name = "spring.scheduling.enabled", havingValue = "true", matchIfMissing = true)`  
**Schedule:** `@Scheduled(cron = "0 0 9 * * *")` — daily at **9:00 AM**

Queries subscriptions expiring within specific windows and sends countdown notifications. Also sends a trial expiry warning.

**Expiry windows checked:**

| Window | Notification Type | Title |
|---|---|---|
| `now+7d` to `now+8d` | `SUBSCRIPTION_EXPIRING_7_DAYS` | "Subscription Expiring Soon" |
| `now+3d` to `now+4d` | `SUBSCRIPTION_EXPIRING_3_DAYS` | "Subscription Expiring" |
| `now+1d` to `now+2d` | `SUBSCRIPTION_EXPIRING_1_DAY` | "Subscription Expires Tomorrow" |

**Query:** `findByCurrentPeriodEndBetweenAndAutoRenewFalse(startWindow, endWindow)` — only notifies users who have auto-renew disabled (users with auto-renew enabled will be billed automatically).

**Data payload:** `{"subscriptionId": "...", "planName": "...", "expiryDate": "..."}`

**Trial expiry:** Queries trials expiring within 3 days, sends `TRIAL_EXPIRING_SOON` notification.

---

## Configuration Reference

```properties
# Active notification provider (FCM, ONESIGNAL, AWS_SNS)
notification.activeProvider=FCM
notification.persistNotifications=true
notification.respectQuietHours=true
notification.maxDevicesPerUser=10
notification.notificationRetentionDays=90
notification.kafkaTopic=notification-events

# Firebase (FCM)
firebase.enabled=true
# Option 1: Inline JSON (recommended for containers)
firebase.credentials.json={"type":"service_account","project_id":"..."}
# Option 2: File path
firebase.credentials.path=classpath:firebase-service-account.json

# OneSignal (optional)
onesignal.enabled=false
onesignal.app-id=your-app-id
onesignal.api-key=your-api-key

# AWS SNS (optional)
aws.sns.enabled=false
aws.region=us-east-1
aws.access-key=...
aws.secret-key=...
```

---

## Flow Diagrams

### Notification Send Flow (Happy Path)

```
Domain Service (e.g., DocumentUploadImpl)
  │
  └─ notificationService.sendToUser(userId, DOCUMENT_UPLOAD_SUCCESS, "Upload Complete", "...", data)
         │
         ├─ kafkaProducer != null
         │     └─ NotificationKafkaProducer.publishNotification(userId, type, title, message, data)
         │           │
         │           └─ kafkaTemplate.send("notification-events", userId, NotificationEvent)
         │
         ▼
Kafka Topic: notification-events (keyed by userId → same partition for ordering)
         │
         ▼
NotificationKafkaConsumer.handleNotificationEvent(event)
         │
         ├─ Step 1: preferencesService.isNotificationTypeEnabled(userId, DOCUMENT_UPLOAD_SUCCESS)
         │               └─ checks pushEnabled + documentNotifications flag
         │               └─ false → return (skip delivery)
         │
         ├─ Step 2: config.respectQuietHours && preferencesService.isInQuietHours(userId)
         │               └─ true → return (skip during quiet window)
         │
         ├─ Step 3: config.persistNotifications → save Notification entity
         │
         ├─ Step 4: providers.get(FCM) → FirebaseNotificationProvider
         │
         ├─ Step 5: deviceTokenService.getActiveTokenEntities(userId)
         │               └─ returns List<UserDeviceToken>
         │
         └─ Step 6: firebaseProvider.sendBatch(tokenStrings, title, message, data)
                         │
                         └─ Split into 500-token chunks → FCM MulticastMessage
                               └─ firebaseMessaging.sendEachForMulticast()
```

---

### Device Registration Flow

```
User → POST /api/v1/notifications/device
  { deviceToken: "fcm-xxx", deviceType: "ANDROID", deviceName: "Pixel 9" }
  │
  ▼
DeviceTokenServiceImpl.registerDevice(userId, request)
  │
  ├─ userRepository.findById(userId)     [throws if not found]
  │
  ├─ deviceTokenRepository.findByDeviceToken("fcm-xxx")
  │     │
  │     ├─ Found + different user → transfer ownership to current user
  │     ├─ Found + same user → reactivate + update name + updateLastUsed()
  │     └─ save + return DeviceTokenResponse
  │
  └─ Not found:
        ├─ countByUserIdAndIsActiveTrue(userId) >= maxDevicesPerUser (10)?
        │     └─ YES → throw IllegalStateException
        └─ Create new UserDeviceToken → save → return DeviceTokenResponse

Response: 201 Created, DeviceTokenResponse
```

---

### Storage Warning Job Flow

```
@Scheduled("0 0 10 * * *")  ← daily 10 AM
StorageWarningNotificationJob.checkStorageUsage()
  │
  └─ For each UserSubscription:
        │
        ├─ storageLimit == null || <= 0 || storageUsed == null → skip
        │
        ├─ usagePercent = (storageUsed / storageLimit) * 100
        │
        ├─ usagePercent >= 95?
        │     └─ existsByUserIdAndWarningLevel(userId, 95)?
        │           ├─ true → skip
        │           └─ false → sendToUser(STORAGE_WARNING_95) → save StorageWarningSent(95)
        │
        ├─ usagePercent >= 90?
        │     └─ (same deduplication pattern)
        │
        └─ usagePercent >= 80?
              └─ (same deduplication pattern)
```

---

### Quiet Hours Check

```
preferencesService.isInQuietHours(userId)
  │
  ├─ No preferences record → return false (default = not in quiet hours)
  │
  └─ preferences.isInQuietHours()
        │
        ├─ quietHoursEnabled = false → return false
        │
        ├─ quietHoursStart = null || quietHoursEnd = null → return false
        │
        └─ currentTime = LocalTime.now()
              │
              ├─ Normal window (start < end):
              │     return currentTime >= start && currentTime < end
              │
              └─ Overnight window (start > end, e.g., 22:00 to 07:00):
                    return currentTime >= start || currentTime < end
```

