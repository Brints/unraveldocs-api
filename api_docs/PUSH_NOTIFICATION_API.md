# Push Notification API Documentation

This document describes the REST API endpoints for the Push Notification system in UnravelDocs.

## Base URL

```
/api/v1/notifications
```

## Authentication

All endpoints require authentication via JWT Bearer token.

```
Authorization: Bearer <your_jwt_token>
```

---

## Table of Contents

1. [Device Management](#device-management)
   - [Register Device](#register-device)
   - [Unregister Device](#unregister-device)
   - [Get Registered Devices](#get-registered-devices)
2. [Notifications](#notifications)
   - [Get Notifications](#get-notifications)
   - [Get Unread Notifications](#get-unread-notifications)
   - [Get Notifications by Type](#get-notifications-by-type)
   - [Get Unread Count](#get-unread-count)
   - [Mark Notification as Read](#mark-notification-as-read)
   - [Mark All as Read](#mark-all-as-read)
   - [Delete Notification](#delete-notification)
3. [Preferences](#preferences)
   - [Get Notification Preferences](#get-notification-preferences)
   - [Update Notification Preferences](#update-notification-preferences)
4. [Data Types](#data-types)
   - [Device Types](#device-types)
   - [Notification Types](#notification-types)

---

## Device Management

### Register Device

Register a device to receive push notifications.

**Endpoint:** `POST /api/v1/notifications/device`

**Request Body:**

```json
{
  "deviceToken": "string (required, max 512 chars)",
  "deviceType": "ANDROID | IOS | WEB (required)",
  "deviceName": "string (optional, max 100 chars)"
}
```

**Example Request:**

```json
{
  "deviceToken": "fMxB7tK9RwC:APA91bHun4MxP...",
  "deviceType": "WEB",
  "deviceName": "Chrome on Windows"
}
```

**Response:** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "deviceToken": "fMxB****4MxP",
  "deviceType": "WEB",
  "deviceName": "Chrome on Windows",
  "isActive": true,
  "createdAt": "2026-01-08T12:00:00Z",
  "lastUsedAt": null
}
```

**Error Responses:**

| Status | Description                                    |
|--------|------------------------------------------------|
| 400    | Invalid request body or validation error       |
| 401    | Unauthorized - Missing or invalid token        |
| 409    | Device limit reached (max 10 devices per user) |

---

### Unregister Device

Remove a device from push notifications.

**Endpoint:** `DELETE /api/v1/notifications/device/{tokenId}`

**Path Parameters:**

| Parameter | Type   | Description                |
|-----------|--------|----------------------------|
| tokenId   | string | The device token ID (UUID) |

**Response:** `204 No Content`

**Error Responses:**

| Status | Description      |
|--------|------------------|
| 401    | Unauthorized     |
| 404    | Device not found |

---

### Get Registered Devices

Get all registered devices for the authenticated user.

**Endpoint:** `GET /api/v1/notifications/devices`

**Response:** `200 OK`

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "deviceToken": "fMxB****4MxP",
    "deviceType": "WEB",
    "deviceName": "Chrome on Windows",
    "isActive": true,
    "createdAt": "2026-01-08T12:00:00Z",
    "lastUsedAt": "2026-01-08T14:30:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "deviceToken": "dGhi****9RkM",
    "deviceType": "ANDROID",
    "deviceName": "Pixel 8",
    "isActive": true,
    "createdAt": "2026-01-07T10:00:00Z",
    "lastUsedAt": "2026-01-08T09:15:00Z"
  }
]
```

---

## Notifications

### Get Notifications

Get paginated list of all notifications for the authenticated user.

**Endpoint:** `GET /api/v1/notifications`

**Query Parameters:**

| Parameter | Type    | Default | Description              |
|-----------|---------|---------|--------------------------|
| page      | integer | 0       | Page number (0-indexed)  |
| size      | integer | 20      | Number of items per page |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440000",
      "type": "DOCUMENT_UPLOAD_SUCCESS",
      "typeDisplayName": "Document Upload Success",
      "category": "document",
      "title": "Document Uploaded",
      "message": "Your document 'Report.pdf' has been uploaded successfully.",
      "data": {
        "documentId": "abc123",
        "fileName": "Report.pdf"
      },
      "isRead": false,
      "createdAt": "2026-01-08T12:00:00Z",
      "readAt": null
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "type": "OCR_PROCESSING_COMPLETED",
      "typeDisplayName": "OCR Processing Completed",
      "category": "ocr",
      "title": "OCR Complete",
      "message": "OCR processing completed for 'Invoice.pdf'.",
      "data": {
        "documentId": "def456",
        "pageCount": "5"
      },
      "isRead": true,
      "createdAt": "2026-01-08T11:30:00Z",
      "readAt": "2026-01-08T11:45:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "direction": "DESC"
    }
  },
  "totalElements": 45,
  "totalPages": 3,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

---

### Get Unread Notifications

Get paginated list of unread notifications.

**Endpoint:** `GET /api/v1/notifications/unread`

**Query Parameters:**

| Parameter | Type    | Default | Description    |
|-----------|---------|---------|----------------|
| page      | integer | 0       | Page number    |
| size      | integer | 20      | Items per page |

**Response:** `200 OK`

Same structure as [Get Notifications](#get-notifications), filtered to unread only.

---

### Get Notifications by Type

Get notifications filtered by notification type.

**Endpoint:** `GET /api/v1/notifications/by-type/{type}`

**Path Parameters:**

| Parameter | Type             | Description                        |
|-----------|------------------|------------------------------------|
| type      | NotificationType | The notification type to filter by |

**Query Parameters:**

| Parameter | Type    | Default | Description    |
|-----------|---------|---------|----------------|
| page      | integer | 0       | Page number    |
| size      | integer | 20      | Items per page |

**Example:** `GET /api/v1/notifications/by-type/PAYMENT_SUCCESS?page=0&size=10`

**Response:** `200 OK`

Same structure as [Get Notifications](#get-notifications).

---

### Get Unread Count

Get the count of unread notifications.

**Endpoint:** `GET /api/v1/notifications/unread-count`

**Response:** `200 OK`

```json
{
  "count": 12
}
```

---

### Mark Notification as Read

Mark a specific notification as read.

**Endpoint:** `PATCH /api/v1/notifications/{id}/read`

**Path Parameters:**

| Parameter | Type   | Description                |
|-----------|--------|----------------------------|
| id        | string | The notification ID (UUID) |

**Response:** `204 No Content`

**Error Responses:**

| Status | Description            |
|--------|------------------------|
| 404    | Notification not found |

---

### Mark All as Read

Mark all notifications as read for the authenticated user.

**Endpoint:** `PATCH /api/v1/notifications/read-all`

**Response:** `204 No Content`

---

### Delete Notification

Delete a specific notification.

**Endpoint:** `DELETE /api/v1/notifications/{id}`

**Path Parameters:**

| Parameter | Type   | Description                |
|-----------|--------|----------------------------|
| id        | string | The notification ID (UUID) |

**Response:** `204 No Content`

**Error Responses:**

| Status | Description            |
|--------|------------------------|
| 404    | Notification not found |

---

## Preferences

### Get Notification Preferences

Get the notification preferences for the authenticated user.

**Endpoint:** `GET /api/v1/notifications/preferences`

**Response:** `200 OK`

```json
{
  "id": "770e8400-e29b-41d4-a716-446655440000",
  "pushEnabled": true,
  "emailEnabled": true,
  "documentNotifications": true,
  "ocrNotifications": true,
  "paymentNotifications": true,
  "storageNotifications": true,
  "subscriptionNotifications": true,
  "teamNotifications": true,
  "quietHoursEnabled": false,
  "quietHoursStart": null,
  "quietHoursEnd": null,
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-08T12:00:00Z"
}
```

---

### Update Notification Preferences

Update notification preferences for the authenticated user.

**Endpoint:** `PUT /api/v1/notifications/preferences`

**Request Body:**

```json
{
  "pushEnabled": true,
  "emailEnabled": true,
  "documentNotifications": true,
  "ocrNotifications": true,
  "paymentNotifications": true,
  "storageNotifications": true,
  "subscriptionNotifications": true,
  "teamNotifications": true,
  "quietHoursEnabled": false,
  "quietHoursStart": "22:00:00",
  "quietHoursEnd": "07:00:00"
}
```

**Request Body Fields:**

| Field                     | Type    | Required | Description                                |
|---------------------------|---------|----------|--------------------------------------------|
| pushEnabled               | boolean | Yes      | Enable/disable push notifications globally |
| emailEnabled              | boolean | Yes      | Enable/disable email notifications         |
| documentNotifications     | boolean | Yes      | Document upload/delete notifications       |
| ocrNotifications          | boolean | Yes      | OCR processing notifications               |
| paymentNotifications      | boolean | Yes      | Payment status notifications               |
| storageNotifications      | boolean | Yes      | Storage warning notifications              |
| subscriptionNotifications | boolean | Yes      | Subscription expiry notifications          |
| teamNotifications         | boolean | Yes      | Team activity notifications                |
| quietHoursEnabled         | boolean | No       | Enable quiet hours                         |
| quietHoursStart           | time    | No       | Start time for quiet hours (HH:mm:ss)      |
| quietHoursEnd             | time    | No       | End time for quiet hours (HH:mm:ss)        |

**Response:** `200 OK`

```json
{
  "id": "770e8400-e29b-41d4-a716-446655440000",
  "pushEnabled": true,
  "emailEnabled": true,
  "documentNotifications": true,
  "ocrNotifications": false,
  "paymentNotifications": true,
  "storageNotifications": true,
  "subscriptionNotifications": true,
  "teamNotifications": true,
  "quietHoursEnabled": true,
  "quietHoursStart": "22:00:00",
  "quietHoursEnd": "07:00:00",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-08T12:30:00Z"
}
```

---

## Data Types

### Device Types

| Value     | Description                      |
|-----------|----------------------------------|
| `ANDROID` | Android device (FCM token)       |
| `IOS`     | iOS device (APNs/FCM token)      |
| `WEB`     | Web browser (FCM Web Push token) |

---

### Notification Types

#### Document Events

| Type                      | Display Name            | Description                    |
|---------------------------|-------------------------|--------------------------------|
| `DOCUMENT_UPLOAD_SUCCESS` | Document Upload Success | Document uploaded successfully |
| `DOCUMENT_UPLOAD_FAILED`  | Document Upload Failed  | Document upload failed         |
| `DOCUMENT_DELETED`        | Document Deleted        | Document was deleted           |

#### OCR Events

| Type                       | Display Name             | Description                |
|----------------------------|--------------------------|----------------------------|
| `OCR_PROCESSING_STARTED`   | OCR Processing Started   | OCR processing has begun   |
| `OCR_PROCESSING_COMPLETED` | OCR Processing Completed | OCR completed successfully |
| `OCR_PROCESSING_FAILED`    | OCR Processing Failed    | OCR processing failed      |

#### Storage Events

| Type                    | Display Name          | Description           |
|-------------------------|-----------------------|-----------------------|
| `STORAGE_WARNING_80`    | Storage Warning 80%   | 80% of storage used   |
| `STORAGE_WARNING_90`    | Storage Warning 90%   | 90% of storage used   |
| `STORAGE_WARNING_95`    | Storage Warning 95%   | 95% of storage used   |
| `STORAGE_LIMIT_REACHED` | Storage Limit Reached | Storage limit reached |

#### Payment Events

| Type               | Display Name     | Description                    |
|--------------------|------------------|--------------------------------|
| `PAYMENT_SUCCESS`  | Payment Success  | Payment processed successfully |
| `PAYMENT_FAILED`   | Payment Failed   | Payment processing failed      |
| `PAYMENT_REFUNDED` | Payment Refunded | Payment was refunded           |

#### Subscription Events

| Type                           | Display Name                    | Description                    |
|--------------------------------|---------------------------------|--------------------------------|
| `SUBSCRIPTION_EXPIRING_7_DAYS` | Subscription Expiring in 7 Days | Subscription expires in 7 days |
| `SUBSCRIPTION_EXPIRING_3_DAYS` | Subscription Expiring in 3 Days | Subscription expires in 3 days |
| `SUBSCRIPTION_EXPIRING_1_DAY`  | Subscription Expiring Tomorrow  | Subscription expires tomorrow  |
| `SUBSCRIPTION_EXPIRED`         | Subscription Expired            | Subscription has expired       |
| `SUBSCRIPTION_RENEWED`         | Subscription Renewed            | Subscription was renewed       |
| `SUBSCRIPTION_UPGRADED`        | Subscription Upgraded           | Subscription was upgraded      |
| `SUBSCRIPTION_DOWNGRADED`      | Subscription Downgraded         | Subscription was downgraded    |
| `TRIAL_EXPIRING_SOON`          | Trial Expiring Soon             | Trial period ending soon       |
| `TRIAL_EXPIRED`                | Trial Expired                   | Trial period has ended         |

#### Team Events

| Type                       | Display Name             | Description              |
|----------------------------|--------------------------|--------------------------|
| `TEAM_INVITATION_RECEIVED` | Team Invitation Received | Received team invitation |
| `TEAM_MEMBER_ADDED`        | Team Member Added        | New member added to team |
| `TEAM_MEMBER_REMOVED`      | Team Member Removed      | Member removed from team |
| `TEAM_ROLE_CHANGED`        | Team Role Changed        | Team role was changed    |

#### System Events

| Type                  | Display Name        | Description                        |
|-----------------------|---------------------|------------------------------------|
| `SYSTEM_ANNOUNCEMENT` | System Announcement | System-wide announcement           |
| `WELCOME`             | Welcome             | Welcome notification for new users |

---

## Error Response Format

All error responses follow this format:

```json
{
  "timestamp": "2026-01-08T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: deviceToken is required",
  "path": "/api/v1/notifications/device"
}
```

---

## Rate Limiting

The notification endpoints are subject to rate limiting:
- Device registration: 10 requests per hour
- Get notifications: 60 requests per minute
- Preference updates: 10 requests per minute

Exceeding rate limits returns `429 Too Many Requests`.
