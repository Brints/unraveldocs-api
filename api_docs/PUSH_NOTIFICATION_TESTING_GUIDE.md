# Push Notification Testing Guide with Postman

This guide explains how to test the Push Notification system using Postman, including setup, authentication, and step-by-step testing of all endpoints.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Firebase Setup](#firebase-setup)
3. [Getting a Test FCM Token](#getting-a-test-fcm-token)
4. [Postman Setup](#postman-setup)
5. [Testing Flow](#testing-flow)
6. [API Endpoints Testing](#api-endpoints-testing)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before testing, ensure you have:

1. **UnravelDocs API running locally** (default: `http://localhost:8080`)
2. **PostgreSQL database** with migrations applied
3. **Kafka running** (for async processing)
4. **Postman** installed
5. **Firebase project** configured (for actual push notifications)

---

## Firebase Setup

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or select an existing project
3. Follow the setup wizard

### Step 2: Get Service Account Credentials

1. In Firebase Console, go to **Project Settings** (gear icon)
2. Navigate to **Service accounts** tab
3. Click **"Generate new private key"**
4. Save the JSON file as `firebase-service-account.json`
5. Place it in `src/main/resources/` folder

### Step 3: Configure Environment

Update your `.env` file:

```properties
FIREBASE_ENABLED=true
FIREBASE_CREDENTIALS_PATH=classpath:firebase-service-account.json
NOTIFICATION_ACTIVE_PROVIDER=FCM
```

---

## Getting a Test FCM Token

To receive actual push notifications, you need a valid FCM device token. Here are several ways to get one:

### Option A: Using Firebase Console (Easiest for Testing)

1. Go to Firebase Console → **Cloud Messaging** → **Send your first message**
2. For testing purposes, you can use a test token from Firebase's testing tools

### Option B: Create a Simple HTML Test Page

Create `test-push.html` in your project:

```html
<!DOCTYPE html>
<html>
<head>
    <title>FCM Token Generator</title>
    <script src="https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js"></script>
</head>
<body>
    <h1>FCM Token Generator</h1>
    <button onclick="requestPermission()">Get FCM Token</button>
    <p id="token"></p>
    
    <script>
        // Replace with your Firebase config from Firebase Console
        const firebaseConfig = {
            apiKey: "YOUR_API_KEY",
            authDomain: "YOUR_PROJECT.firebaseapp.com",
            projectId: "YOUR_PROJECT_ID",
            storageBucket: "YOUR_PROJECT.appspot.com",
            messagingSenderId: "YOUR_SENDER_ID",
            appId: "YOUR_APP_ID"
        };

        firebase.initializeApp(firebaseConfig);
        const messaging = firebase.messaging();

        async function requestPermission() {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                const token = await messaging.getToken({
                    vapidKey: 'YOUR_VAPID_KEY'  // From Firebase Console → Cloud Messaging → Web Push certificates
                });
                document.getElementById('token').innerText = token;
                console.log('FCM Token:', token);
            }
        }
    </script>
</body>
</html>
```

### Option C: Use a Mock Token (API Testing Only)

For testing the API endpoints without actual push delivery, use a fake token:

```
mock-device-token-for-testing-12345
```

> ⚠️ **Note:** Mock tokens will register in the database but won't receive actual push notifications. Firebase will return an error when trying to send to invalid tokens.

---

## Postman Setup

### Step 1: Create Environment

Create a new Postman environment with these variables:

| Variable | Initial Value | Description |
|----------|---------------|-------------|
| `base_url` | `http://localhost:8080` | API base URL |
| `access_token` | (empty) | JWT token after login |
| `device_token` | (your FCM token) | FCM device token |
| `notification_id` | (empty) | For testing read/delete |

### Step 2: Create Collection

Create a new collection called **"UnravelDocs - Push Notifications"**

### Step 3: Add Authorization

In Collection settings → **Authorization**:
- Type: `Bearer Token`
- Token: `{{access_token}}`

---

## Testing Flow

### Complete Testing Sequence

```
1. Login → Get JWT Token
        ↓
2. Register Device Token
        ↓
3. Set Notification Preferences
        ↓
4. Trigger a Notification (e.g., upload document)
        ↓
5. Check Notifications List
        ↓
6. Mark as Read
        ↓
7. Clean Up (Delete/Unregister)
```

---

## API Endpoints Testing

### 1. Authentication - Login

First, get a JWT token by logging in.

**Request:**
```
POST {{base_url}}/api/v1/auth/login
Content-Type: application/json

{
    "email": "testuser@example.com",
    "password": "your_password"
}
```

**Expected Response:** `200 OK`
```json
{
    "statusCode": 200,
    "status": "success",
    "message": "Login successful",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "...",
        "tokenType": "Bearer"
    }
}
```

**Post-request Script (to save token):**
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("access_token", jsonData.data.accessToken);
}
```

---

### 2. Register Device Token

Register your device to receive push notifications.

**Request:**
```
POST {{base_url}}/api/v1/notifications/device
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
    "deviceToken": "{{device_token}}",
    "deviceType": "WEB",
    "deviceName": "Postman Test Device"
}
```

**Expected Response:** `201 Created`
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "deviceToken": "mock****2345",
    "deviceType": "WEB",
    "deviceName": "Postman Test Device",
    "isActive": true,
    "createdAt": "2026-01-08T10:00:00Z",
    "lastUsedAt": null
}
```

**Post-request Script:**
```javascript
if (pm.response.code === 201) {
    var jsonData = pm.response.json();
    pm.environment.set("device_id", jsonData.id);
}
```

---

### 3. Get Registered Devices

Verify the device was registered.

**Request:**
```
GET {{base_url}}/api/v1/notifications/devices
Authorization: Bearer {{access_token}}
```

**Expected Response:** `200 OK`
```json
[
    {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "deviceToken": "mock****2345",
        "deviceType": "WEB",
        "deviceName": "Postman Test Device",
        "isActive": true,
        "createdAt": "2026-01-08T10:00:00Z",
        "lastUsedAt": null
    }
]
```

---

### 4. Get Notification Preferences

Check current notification settings.

**Request:**
```
GET {{base_url}}/api/v1/notifications/preferences
Authorization: Bearer {{access_token}}
```

**Expected Response:** `200 OK`
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
    "createdAt": "2026-01-08T10:00:00Z",
    "updatedAt": "2026-01-08T10:00:00Z"
}
```

---

### 5. Update Notification Preferences

Customize notification settings.

**Request:**
```
PUT {{base_url}}/api/v1/notifications/preferences
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
    "pushEnabled": true,
    "emailEnabled": false,
    "documentNotifications": true,
    "ocrNotifications": true,
    "paymentNotifications": true,
    "storageNotifications": true,
    "subscriptionNotifications": true,
    "teamNotifications": false,
    "quietHoursEnabled": true,
    "quietHoursStart": "22:00:00",
    "quietHoursEnd": "07:00:00"
}
```

**Expected Response:** `200 OK`

---

### 6. Trigger a Notification

To see notifications in action, trigger an event that creates a notification.

**Option A: Upload a Document**
```
POST {{base_url}}/api/v1/documents/upload
Authorization: Bearer {{access_token}}
Content-Type: multipart/form-data

file: [select a PDF file]
```

This should trigger a `DOCUMENT_UPLOAD_SUCCESS` notification.

**Option B: Directly Query Notifications (Simulated)**

If you've inserted test data into the `notifications` table:

```sql
INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at)
VALUES (
    gen_random_uuid(),
    'your-user-id',
    'DOCUMENT_UPLOAD_SUCCESS',
    'Test Notification',
    'This is a test notification from Postman',
    false,
    NOW()
);
```

---

### 7. Get All Notifications

Retrieve your notifications.

**Request:**
```
GET {{base_url}}/api/v1/notifications?page=0&size=20
Authorization: Bearer {{access_token}}
```

**Expected Response:** `200 OK`
```json
{
    "content": [
        {
            "id": "660e8400-e29b-41d4-a716-446655440000",
            "type": "DOCUMENT_UPLOAD_SUCCESS",
            "typeDisplayName": "Document Upload Success",
            "category": "document",
            "title": "Document Uploaded",
            "message": "Your document has been uploaded successfully.",
            "data": {},
            "isRead": false,
            "createdAt": "2026-01-08T10:15:00Z",
            "readAt": null
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
}
```

**Post-request Script:**
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.content && jsonData.content.length > 0) {
        pm.environment.set("notification_id", jsonData.content[0].id);
    }
}
```

---

### 8. Get Unread Notifications

**Request:**
```
GET {{base_url}}/api/v1/notifications/unread?page=0&size=20
Authorization: Bearer {{access_token}}
```

---

### 9. Get Unread Count

**Request:**
```
GET {{base_url}}/api/v1/notifications/unread-count
Authorization: Bearer {{access_token}}
```

**Expected Response:** `200 OK`
```json
{
    "count": 5
}
```

---

### 10. Mark Notification as Read

**Request:**
```
PATCH {{base_url}}/api/v1/notifications/{{notification_id}}/read
Authorization: Bearer {{access_token}}
```

**Expected Response:** `204 No Content`

---

### 11. Mark All as Read

**Request:**
```
PATCH {{base_url}}/api/v1/notifications/read-all
Authorization: Bearer {{access_token}}
```

**Expected Response:** `204 No Content`

---

### 12. Delete Notification

**Request:**
```
DELETE {{base_url}}/api/v1/notifications/{{notification_id}}
Authorization: Bearer {{access_token}}
```

**Expected Response:** `204 No Content`

---

### 13. Unregister Device (Cleanup)

**Request:**
```
DELETE {{base_url}}/api/v1/notifications/device/{{device_id}}
Authorization: Bearer {{access_token}}
```

**Expected Response:** `204 No Content`

---

## How Push Notifications Flow Works

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Your App      │────▶│   API Server    │────▶│     Kafka       │
│ (Trigger Event) │     │ (Publishes msg) │     │   (Queue)       │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                        ┌─────────────────┐              │
                        │  Kafka Consumer │◀─────────────┘
                        │ (Processes msg) │
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼
     ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
     │    Firebase    │ │   OneSignal    │ │    AWS SNS     │
     │     (FCM)      │ │  (Alternative) │ │ (Alternative)  │
     └────────┬───────┘ └────────────────┘ └────────────────┘
              │
              ▼
     ┌────────────────┐
     │  User Device   │
     │  (Push Notif)  │
     └────────────────┘
```

### Notification Event Flow:

1. **Event Occurs** (e.g., document upload, payment success)
2. **API Server** publishes `NotificationEvent` to Kafka topic
3. **Kafka Consumer** picks up the event
4. **Consumer checks** user preferences (type enabled? quiet hours?)
5. **If allowed**, sends via active provider (FCM/OneSignal/SNS)
6. **Notification** stored in database for history
7. **Push delivered** to user's registered devices

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Check JWT token is valid and not expired |
| 404 Not Found | Verify endpoint URL and resource IDs |
| No push received | Verify FCM token is valid and Firebase is configured |
| Notification not created | Check Kafka is running and consumer is processing |

### Checking Kafka Messages

```bash
# List topics
kafka-topics --list --bootstrap-server localhost:9092

# Watch notification events
kafka-console-consumer --topic notification-events --from-beginning --bootstrap-server localhost:9092
```

### Checking Database

```sql
-- View registered devices
SELECT * FROM user_device_tokens WHERE user_id = 'your-user-id';

-- View notifications
SELECT * FROM notifications WHERE user_id = 'your-user-id' ORDER BY created_at DESC;

-- View preferences
SELECT * FROM notification_preferences WHERE user_id = 'your-user-id';
```

### Logs to Check

```bash
# Application logs for notification events
grep -i "notification" logs/application.log

# Firebase-specific logs
grep -i "firebase\|fcm" logs/application.log
```

---

## Postman Collection Export

You can import this collection directly into Postman. Save the following as `push-notifications.postman_collection.json`:

```json
{
    "info": {
        "name": "UnravelDocs - Push Notifications",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "auth": {
        "type": "bearer",
        "bearer": [{"key": "token", "value": "{{access_token}}"}]
    },
    "item": [
        {
            "name": "1. Login",
            "request": {
                "method": "POST",
                "url": "{{base_url}}/api/v1/auth/login",
                "body": {
                    "mode": "raw",
                    "raw": "{\"email\": \"test@example.com\", \"password\": \"password123\"}"
                }
            }
        },
        {
            "name": "2. Register Device",
            "request": {
                "method": "POST",
                "url": "{{base_url}}/api/v1/notifications/device",
                "body": {
                    "mode": "raw",
                    "raw": "{\"deviceToken\": \"{{device_token}}\", \"deviceType\": \"WEB\", \"deviceName\": \"Postman\"}"
                }
            }
        },
        {
            "name": "3. Get Devices",
            "request": {"method": "GET", "url": "{{base_url}}/api/v1/notifications/devices"}
        },
        {
            "name": "4. Get Preferences",
            "request": {"method": "GET", "url": "{{base_url}}/api/v1/notifications/preferences"}
        },
        {
            "name": "5. Get Notifications",
            "request": {"method": "GET", "url": "{{base_url}}/api/v1/notifications?page=0&size=20"}
        },
        {
            "name": "6. Get Unread Count",
            "request": {"method": "GET", "url": "{{base_url}}/api/v1/notifications/unread-count"}
        },
        {
            "name": "7. Mark as Read",
            "request": {"method": "PATCH", "url": "{{base_url}}/api/v1/notifications/{{notification_id}}/read"}
        },
        {
            "name": "8. Delete Device",
            "request": {"method": "DELETE", "url": "{{base_url}}/api/v1/notifications/device/{{device_id}}"}
        }
    ]
}
```

---

## Next Steps

1. **Setup Firebase** with real credentials
2. **Run the API** with Kafka enabled
3. **Get a valid FCM token** from a web browser or mobile app
4. **Register the device** via Postman
5. **Trigger events** (document upload, etc.)
6. **Verify push notifications** are received on device
