# Admin API Documentation - User Management

This document details the API endpoints related to comprehensive user lifecycle management and statistics within the UnravelDocs admin dashboard.

## Phase 2A: Enhanced User List & Detail View

### 1. Get All Users (Advanced Filtering)
Fetches a paginated list of all users, supporting an extensive list of filters for deep administrative insight.

- **URL:** `/api/v1/admin/users`
- **Method:** `GET`
- **Authentication:** Required (Bearer Token)
- **Role:** `ADMIN`, `SUPER_ADMIN`, `MODERATOR`

#### Query Parameters
| Parameter             | Type     | Required | Description                                                                   |
|-----------------------|----------|----------|-------------------------------------------------------------------------------|
| `page`                | integer  | No       | Page number (0-indexed, default: 0)                                           |
| `size`                | integer  | No       | Page size (default: 10)                                                       |
| `sortBy`              | string   | No       | Field to sort by (`createdAt`, `lastLogin`, `email`, `firstName`, `lastName`) |
| `sortOrder`           | string   | No       | Sort direction (`asc` or `desc`)                                              |
| `search`              | string   | No       | Search query for name or email (min 3 chars)                                  |
| `firstName`           | string   | No       | Exact match first name                                                        |
| `lastName`            | string   | No       | Exact match last name                                                         |
| `email`               | string   | No       | Exact match email                                                             |
| `role`                | string   | No       | Filter by role (`USER`, `ADMIN`, etc.)                                        |
| `isActive`            | boolean  | No       | Filter by active status                                                       |
| `isVerified`          | boolean  | No       | Filter by verification status                                                 |
| `isPlatformAdmin`     | boolean  | No       | Filter platform admins                                                        |
| `isOrganizationAdmin` | boolean  | No       | Filter org admins                                                             |
| `country`             | string   | No       | Filter by country code/name                                                   |
| `profession`          | string   | No       | Filter by profession                                                          |
| `organization`        | string   | No       | Filter by organization                                                        |
| `subscriptionPlan`    | string   | No       | Filter by active plan (`FREE`, `PRO`, `TEAM`, `ENTERPRISE`)                   |
| `hasTeam`             | boolean  | No       | Filter users belonging to a team                                              |
| `isBlocked`           | boolean  | No       | Filter blocked users                                                          |
| `createdAfter`        | datetime | No       | Registration date range start (ISO 8601)                                      |
| `createdBefore`       | datetime | No       | Registration date range end (ISO 8601)                                        |
| `lastLoginAfter`      | datetime | No       | Last login range start (ISO 8601)                                             |
| `lastLoginBefore`     | datetime | No       | Last login range end (ISO 8601)                                               |

#### Response format (200 OK)
Returns `UnravelDocsResponse<UserListData>`.

```json
{
    "statusCode": 200,
    "status": "success",
    "message": "Successfully fetched all users.",
    "data": {
        "users": [
            {
                "id": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
                "profilePicture": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/profile_pictures/7790079e-ec77-4a6f-beee-f8f72df96517-unnamed.png",
                "firstName": "Admin",
                "lastName": "User",
                "email": "admin@unraveldocs.xyz",
                "role": "super_admin",
                "lastLogin": "2026-03-25T13:30:17.665713Z",
                "createdAt": "2026-01-04T16:56:46.785597Z",
                "updatedAt": "2026-03-25T13:30:17.672626Z",
                "active": true,
                "verified": true
            },
            {
                "id": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
                "profilePicture": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/profile_pictures/bf2672d7-172a-4dda-ac94-c2cd38dc6872-54312052.jpeg",
                "firstName": "Michael",
                "lastName": "Whyte",
                "email": "afiaaniebiet0@gmail.com",
                "role": "user",
                "lastLogin": "2026-03-18T11:51:20.456684Z",
                "createdAt": "2026-01-06T01:41:44.92849Z",
                "updatedAt": "2026-03-18T11:51:20.490708Z",
                "active": true,
                "verified": true
            },
            {
                "id": "37182e20-ed95-40aa-acdd-3afb1f8d0a5a",
                "profilePicture": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/profile_pictures/0474c2e9-1670-4043-98ab-c93b46061744-unnamed.png",
                "firstName": "William",
                "lastName": "French",
                "email": "goldenlee87@gmail.com",
                "role": "user",
                "lastLogin": "2026-03-18T11:53:49.262021Z",
                "createdAt": "2026-01-06T02:34:16.495644Z",
                "updatedAt": "2026-03-18T11:53:49.26353Z",
                "active": true,
                "verified": true
            },
            {
                "id": "f4f9c4b4-53e1-4816-bf09-057819d7a2b8",
                "profilePicture": null,
                "firstName": "Yetunde",
                "lastName": "Benneth",
                "email": "aniebietafia87@gmail.com",
                "role": "user",
                "lastLogin": "2026-03-25T13:29:49.534382Z",
                "createdAt": "2026-01-09T03:13:57.612842Z",
                "updatedAt": "2026-03-25T13:29:49.559786Z",
                "active": true,
                "verified": true
            },
            {
                "id": "7ba790fa-9c22-4eec-bf6f-9d2686eb0aaf",
                "profilePicture": null,
                "firstName": "Michael",
                "lastName": "Weiß",
                "email": "brintsgroup@gmail.com",
                "role": "user",
                "lastLogin": null,
                "createdAt": "2026-03-25T13:33:57.500585Z",
                "updatedAt": "2026-03-25T13:33:57.500585Z",
                "active": false,
                "verified": false
            }
        ],
        "totalUsers": 5,
        "totalPages": 1,
        "currentPage": 0,
        "pageSize": 10
    }
}
```

---

### 2. Get User Profile by Admin (Enriched View)
Fetches an exhaustive, nested record of a user's data including usage quotas, credit balance, subscription details, and security flags.

- **URL:** `/api/v1/admin/{userId}`
- **Method:** `GET`
- **Authentication:** Required (Bearer Token)
- **Role:** `ADMIN`, `SUPER_ADMIN`

#### Path Variables
| Variable | Type   | Description      |
|----------|--------|------------------|
| `userId` | string | UUID of the user |

#### Response schema (200 OK)
Returns `UnravelDocsResponse<AdminUserDetailDto>`.

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Enriched user profile retrieved successfully",
  "data": {
    "profile": {
      "id": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "name": "Michael Whyte",
      "email": "afiaaniebiet0@gmail.com",
      "profilePicture": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/profile_pictures/bf2672d7-172a-4dda-ac94-c2cd38dc6872-54312052.jpeg",
      "country": "Canada",
      "profession": "Software engineer",
      "organization": "Times",
      "role": "user",
      "createdAt": "2026-01-06T01:41:44.92849Z",
      "lastLogin": "2026-03-18T11:51:20.456684Z"
    },
    "statusFlags": {
      "termsAccepted": true,
      "marketingOptIn": false,
      "active": true,
      "organizationAdmin": false,
      "platformAdmin": false,
      "verified": true
    },
    "subscription": {
      "planName": "STARTER_MONTHLY",
      "status": "active",
      "periodStart": "2026-02-04T12:42:39.872523Z",
      "periodEnd": "2026-03-04T12:42:39.872523Z",
      "autoRenew": true,
      "trialEndsAt": null,
      "source": "INDIVIDUAL",
      "gatewaySubscriptionId": null
    },
    "usageQuotas": {
      "storageUsed": 6967386,
      "storageLimit": 2791728742,
      "ocrUsed": 4,
      "ocrLimit": 150,
      "aiUsed": 0,
      "aiLimit": 0,
      "docsUploaded": 7,
      "docsLimit": 30,
      "quotaResetDate": "2026-04-01T00:00:00Z"
    },
    "creditBalance": {
      "balance": 229,
      "totalPurchased": 231,
      "totalUsed": 2
    },
    "loginSecurity": {
      "attempts": 0,
      "blockedUntil": null,
      "blocked": false
    }
  }
}
```

#### Errors
- `404 Not Found`: "User not found"
```json
{
    "statusCode": 404,
    "error": "Not Found",
    "message": "User not found",
    "errorCode": null
}
```
- `403 Forbidden`: Role is insufficient.
```json
{
  "statusCode": 403,
  "status": "FORBIDDEN",
  "message": "Access denied. Only admin or super admin can access this resource.",
  "data": null
}
```

---

## Phase 2B: Admin Action Endpoints

### 3. Activate User
- **URL:** `PUT /api/v1/admin/users/{userId}/activate`
- **Role:** `ADMIN`, `SUPER_ADMIN`

- **Body:** 
```json
{ 
    "reason": "Difficulty getting activation email after 3 days." 
}
```

- **Response:** 
```json
{
    "statusCode": 200,
    "status": "success",
    "message": "User successfully activated",
    "data": null
}
```

### 4. Deactivate User
- **URL:** `PUT /api/v1/admin/users/{userId}/deactivate`
- **Role:** `ADMIN`, `SUPER_ADMIN`
- **Body:** `{ "reason": "string" }`
- **Response:** `200 OK` — `"User successfully deactivated"`

### 5. Force Verify Email
- **URL:** `PUT /api/v1/admin/users/{userId}/force-verify`
- **Role:** `ADMIN`, `SUPER_ADMIN`
- **Body:** None
- **Response:** `200 OK` 
```json
{
    "statusCode": 200,
    "status": "success",
    "message": "User email successfully verified",
    "data": null
}
```

### 6. Unlock User
Resets `loginAttempts` to 0 and clears the `blockedUntil` timestamp.
- **URL:** `PUT /api/v1/admin/users/{userId}/unlock`
- **Role:** `ADMIN`, `SUPER_ADMIN`
- **Body:** None
- **Response:** `200 OK` — `"User account unlocked"`

### 7. Admin Reset Password
Triggers a password-reset email to the user (same flow as `POST /forgot-password`).
- **URL:** `POST /api/v1/admin/users/{userId}/reset-password`
- **Role:** `ADMIN`, `SUPER_ADMIN`
- **Body:** None
- **Response:** `200 OK` — `"Password reset email triggered successfully"`

### 8. Soft Delete User
Sets `deletedAt` and deactivates the user.
- **URL:** `DELETE /api/v1/admin/users/{userId}/soft-delete`
- **Role:** `SUPER_ADMIN`
- **Body:** `{ "reason": "string" }`
- **Response:** `200 OK` — `"User successfully soft-deleted"`

### 9. Impersonate User
Generates a JWT access token for the target user (audit-logged at `WARN` level).
- **URL:** `POST /api/v1/admin/users/{userId}/impersonate`
- **Role:** `SUPER_ADMIN`
- **Body:** `{ "reason": "string" }`
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Impersonation token generated successfully",
  "data": {
    "userId": "7ba790fa-9c22-4eec-bf6f-9d2686eb0aaf",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9****",
    "tokenType": "Bearer",
    "accessExpiresIn": 3600000
  }
}
```

### 10. Adjust User Subscription
Overrides the user's subscription plan, billing interval, and auto-renew flag.
- **URL:** `PUT /api/v1/admin/users/{userId}/subscription`
- **Role:** `SUPER_ADMIN`
- **Body:**
```json
{
  "plan": "PRO_MONTHLY",
  "billingIntervalUnit": "MONTH",
  "billingIntervalValue": 1,
  "autoRenew": true,
  "source": "ADMIN_OVERRIDE"
}
```
- **Response:** `200 OK` — `"User subscription successfully adjusted to PRO_MONTHLY"`

### 11. Reset User Quotas
Resets `ocrPagesUsed`, `monthlyDocumentsUploaded`, and `aiOperationsUsed` to 0 and pushes `quotaResetDate` forward 30 days.
- **URL:** `POST /api/v1/admin/users/{userId}/quotas/reset`
- **Role:** `ADMIN`, `SUPER_ADMIN`
- **Body:** None
- **Response:** `200 OK` — `"User usage quotas have been reset for the current billing cycle"`

### Common Error Responses (All Action Endpoints)
| Code  | Condition                                      |
|-------|------------------------------------------------|
| `400` | Validation errors (blank reason, missing plan) |
| `403` | Caller lacks the required role                 |
| `404` | User not found                                 |

---

## Phase 2C: User Statistics Endpoint

### 12. Get User Statistics
Returns aggregated user statistics for admin dashboard charts and KPIs.

- **URL:** `GET /api/v1/admin/users/stats`
- **Method:** `GET`
- **Authentication:** Required (Bearer Token)
- **Role:** `ADMIN`, `SUPER_ADMIN`, `MODERATOR`

#### Response (200 OK)
Returns `UnravelDocsResponse<UserStatsDto>`.

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "User statistics retrieved successfully",
  "data": {
    "totalUsers": 100,
    "activeUsers": 80,
    "inactiveUsers": 20,
    "verifiedUsers": 75,
    "unverifiedUsers": 25,
    "blockedUsers": 3,
    "softDeletedUsers": 5,
    "newUsersToday": 2,
    "newUsersThisWeek": 15,
    "newUsersThisMonth": 45,
    "dailyActiveUsers": 30,
    "weeklyActiveUsers": 60,
    "monthlyActiveUsers": 85,
    "verificationRate": 75.0,
    "marketingOptInRate": 40.0,
    "usersByRole": { "user": 90, "admin": 10 },
    "usersByCountry": { "US": 40, "NG": 30, "UK": 20 },
    "usersByProfession": { "Developer": 50, "Designer": 30 },
    "usersByOrganization": { "Acme Corp": 25 },
    "userGrowthSeries": {
      "2024-02-23": 2,
      "2024-02-24": 0,
      "2026-03-23": 0,
      "2026-03-24": 0,
      "2026-03-25": 1
    }
  }
}
```

---

## Phase 3: Subscription & Billing Module

### Phase 3A: Subscription Statistics

#### 13. Get Subscription Statistics
Returns aggregated subscription and revenue statistics for the admin dashboard.

- **URL:** `GET /api/v1/admin/subscriptions/stats`
- **Method:** `GET`
- **Authentication:** Required (Bearer Token)
- **Role:** `ADMIN`, `SUPER_ADMIN`, `MODERATOR`

##### Response (200 OK)
Returns `UnravelDocsResponse<SubscriptionStatsDto>`.

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Subscription statistics retrieved successfully",
  "data": {
    "totalSubscriptions": 1500,
    "byPlan": {
      "FREE": 800,
      "STARTER_MONTHLY": 200,
      "PRO_MONTHLY": 150
    },
    "byStatus": {
      "ACTIVE": 700,
      "TRIAL": 200,
      "CANCELLED": 100,
      "EXPIRED": 500
    },
    "bySource": {
      "INDIVIDUAL": 1200,
      "TEAM": 300
    },
    "trialConversionRate": 45.2,
    "churnRate": 5.3,
    "mrr": 12500.00,
    "averageRevenuePerUser": 8.33,
    "usersNearingQuotaLimits": {
      "storage": 45,
      "ocrPages": 23,
      "aiOperations": 12,
      "documentUploads": 67
    }
  }
}
```

### Phase 3B: Subscription Plan Management

#### 14. List Subscription Plans
Returns all subscription plans with their current limits, pricing, and active status.

- **URL:** `GET /api/v1/admin/subscriptions/plans`
- **Method:** `GET`
- **Role:** `ADMIN`, `SUPER_ADMIN`, `MODERATOR`

#### Response (200 OK)
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Plans retrieved successfully",
  "data": [
    {
      "id": "2db26e3f-8311-49c3-a05a-4e3f2e690175",
      "name": "FREE",
      "price": 0.00,
      "currency": "USD",
      "billingIntervalUnit": "MONTH",
      "billingIntervalValue": 1,
      "documentUploadLimit": 5,
      "ocrPageLimit": 25,
      "paystackPlanCode": null,
      "stripePriceId": null,
      "paypalPlanCode": null,
      "discounts": [],
      "createdAt": "2026-01-04T16:56:46.343568Z",
      "updatedAt": "2026-01-04T16:56:46.343568Z",
      "storageLimit": 125829120,
      "trialDays": 10,
      "aiOperationsLimit": 0,
      "active": true
    },
    {
      "id": "53ee631a-429a-47f1-85e7-4958728132db",
      "name": "STARTER_MONTHLY",
      "price": 9.00,
      "currency": "USD",
      "billingIntervalUnit": "MONTH",
      "billingIntervalValue": 1,
      "documentUploadLimit": 30,
      "ocrPageLimit": 150,
      "paystackPlanCode": null,
      "stripePriceId": null,
      "paypalPlanCode": "P-1FH51661F6861794KNFVMAFQ",
      "discounts": [],
      "createdAt": "2026-01-04T16:56:46.386418Z",
      "updatedAt": "2026-01-16T22:47:55.944326Z",
      "storageLimit": 2791728742,
      "trialDays": 10,
      "aiOperationsLimit": 0,
      "active": true
    },
    {
      "id": "cb3ac114-16cf-4879-ae92-499470f5089e",
      "name": "STARTER_YEARLY",
      "price": 90.00,
      "currency": "USD",
      "billingIntervalUnit": "YEAR",
      "billingIntervalValue": 1,
      "documentUploadLimit": 360,
      "ocrPageLimit": 1800,
      "paystackPlanCode": null,
      "stripePriceId": null,
      "paypalPlanCode": "P-9PY30079LV554063PNFVMAFY",
      "discounts": [],
      "createdAt": "2026-01-04T16:56:46.393598Z",
      "updatedAt": "2026-01-16T22:47:55.955251Z",
      "storageLimit": 2791728742,
      "trialDays": 10,
      "aiOperationsLimit": 0,
      "active": true
    },
    {
      "id": "674c742b-b8bf-46ac-96bf-f969473b2af1",
      "name": "PRO_MONTHLY",
      "price": 19.00,
      "currency": "USD",
      "billingIntervalUnit": "MONTH",
      "billingIntervalValue": 1,
      "documentUploadLimit": 100,
      "ocrPageLimit": 500,
      "paystackPlanCode": null,
      "stripePriceId": null,
      "paypalPlanCode": "P-4S319464E4469603WNFVMAGA",
      "discounts": [],
      "createdAt": "2026-01-04T16:56:46.400931Z",
      "updatedAt": "2026-01-16T22:47:55.957992Z",
      "storageLimit": 13207604838,
      "trialDays": 10,
      "aiOperationsLimit": 0,
      "active": true
    },
    {
      "id": "5b40f60d-0436-4dd9-9d44-6005566f2519",
      "name": "PRO_YEARLY",
      "price": 190.00,
      "currency": "USD",
      "billingIntervalUnit": "YEAR",
      "billingIntervalValue": 1,
      "documentUploadLimit": 1200,
      "ocrPageLimit": 6000,
      "paystackPlanCode": null,
      "stripePriceId": null,
      "paypalPlanCode": "P-25L51081515626437NFVMAGI",
      "discounts": [],
      "createdAt": "2026-01-04T16:56:46.407335Z",
      "updatedAt": "2026-01-16T22:47:55.959143Z",
      "storageLimit": 13207604838,
      "trialDays": 10,
      "aiOperationsLimit": 0,
      "active": true
    },
    {
      "id": "1356641e-72fc-4254-9266-10816c522195",
      "name": "BUSINESS_MONTHLY",
      "price": 49.00,
      "currency": "USD",
      "billingIntervalUnit": "MONTH",
      "billingIntervalValue": 1,
      "documentUploadLimit": 500,
      "ocrPageLimit": 2500,
      "paystackPlanCode": null,
      "stripePriceId": null,
      "paypalPlanCode": "P-31D633984A491034NNFVMAGY",
      "discounts": [],
      "createdAt": "2026-01-04T16:56:46.415879Z",
      "updatedAt": "2026-01-16T22:47:55.962012Z",
      "storageLimit": 32212254720,
      "trialDays": 10,
      "aiOperationsLimit": 0,
      "active": true
    },
    {
      "id": "02ca4bbd-01a4-4ba0-9f92-00b4c8ebc6c9",
      "name": "BUSINESS_YEARLY",
      "price": 490.00,
      "currency": "USD",
      "billingIntervalUnit": "YEAR",
      "billingIntervalValue": 1,
      "documentUploadLimit": 6000,
      "ocrPageLimit": 30000,
      "paystackPlanCode": null,
      "stripePriceId": null,
      "paypalPlanCode": "P-1DE79127MA208243ENFVMAHA",
      "discounts": [],
      "createdAt": "2026-01-04T16:56:46.783559Z",
      "updatedAt": "2026-01-16T22:47:55.967103Z",
      "storageLimit": 32212254720,
      "trialDays": 10,
      "aiOperationsLimit": 0,
      "active": true
    }
  ]
}
```

#### 15. Update Plan Limits
Updates the limits, pricing, or trial period for a specific subscription plan.

- **URL:** `PUT /api/v1/admin/subscriptions/plans/{planId}`
- **Method:** `PUT`
- **Role:** `SUPER_ADMIN`
- **Body:** `UpdatePlanLimitsDto`
```json
{
  "documentUploadLimit": 1000,
  "ocrPageLimit": 500,
  "storageLimit": 10737418240,
  "aiOperationsLimit": 100,
  "price": 19.99,
  "trialDays": 14
}
```

#### Response (200 OK)
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Subscription plan updated successfully.",
  "data": {
    "id": "02ca4bbd-01a4-4ba0-9f92-00b4c8ebc6c9",
    "planName": "BUSINESS_YEARLY",
    "planPrice": 490.00,
    "planCurrency": "USD",
    "billingIntervalUnit": "YEAR",
    "billingIntervalValue": 1,
    "documentUploadLimit": 10000,
    "ocrPageLimit": 5000,
    "createdAt": "2026-01-04T16:56:46.783559Z",
    "updatedAt": "2026-03-30T13:52:01.636274Z",
    "active": true
  }
}
```

#### Toggle Plan Status
Activates or deactivates a subscription plan.

- **URL:** `PATCH /api/v1/admin/subscriptions/plans/{planId}/status`
- **Method:** `PATCH`
- **Role:** `SUPER_ADMIN`
- **Body:** `ActionReasonDto`

#### Request Parameters
| Parameter  | Type    | Required |
|------------|---------|----------|
| `isActive` | boolean | Yes      |

#### Request Body
```json
{
  "reason": "string"
}
```

#### Response (200 OK)
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Subscription plan successfully deactivated",
  "data": null
}
```

### Phase 3C: Plan Subscribers

#### 17. List Users on Plan
Returns a paginated list of users subscribed to a specific plan.

- **URL:** `GET /api/v1/admin/subscriptions/plans/{planId}/subscribers`
- **Method:** `GET`
- **Role:** `ADMIN`, `SUPER_ADMIN`, `MODERATOR`
- **Query Params:** `page` (default 0), `size` (default 10)

#### Response (200 OK)
```json
{
    "statusCode": 200,
    "status": "success",
    "message": "Plan subscribers retrieved successfully",
    "data": {
        "users": [
            {
                "id": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
                "profilePicture": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/profile_pictures/7790079e-ec77-4a6f-beee-f8f72df96517-unnamed.png",
                "firstName": "Admin",
                "lastName": "User",
                "email": "admin@unraveldocs.xyz",
                "role": "super_admin",
                "lastLogin": "2026-03-30T13:38:59.94528Z",
                "createdAt": "2026-01-04T16:56:46.785597Z",
                "updatedAt": "2026-03-30T13:38:59.955628Z",
                "active": true,
                "verified": true
            }
        ],
        "totalUsers": 1,
        "totalPages": 1,
        "currentPage": 0,
        "pageSize": 10
    }
}
```
---

## Document Admin API

### Get Document Statistics
**Endpoint**: `GET /api/v1/admin/documents/stats`  
**Description**: Fetches aggregated document, file and storage metrics.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document stats retrieved successfully",
  "data": {
    "totalCollections": 100,
    "totalFiles": 500,
    "totalStorageBytes": 1024000,
    "filesByType": {
      "application/pdf": 400,
      "image/jpeg": 100
    },
    "filesByStatus": {
      "COMPLETED": 450,
      "FAILED": 50
    },
    "encryptedDocuments": 50,
    "averageFilesPerCollection": 5.0,
    "uploadSuccessRate": 90.0
  }
}
```

---

## Notification Admin API

### Get Notification Statistics
**Endpoint**: `GET /api/v1/admin/notifications/stats`  
**Description**: Fetches aggregated notification engagement and device metrics.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Notification stats retrieved successfully",
  "data": {
    "totalNotifications": 1000,
    "notificationsByType": {
      "OCR_COMPLETE": 700,
      "ACCOUNT_ALERT": 300
    },
    "readCount": 600,
    "unreadCount": 400,
    "readRate": 60.0,
    "registeredDevices": 250,
    "devicesByType": {
      "ios": 150,
      "android": 100
    }
  }
}
```

---

## Security Admin API

### Get Security Statistics
**Endpoint**: `GET /api/v1/admin/security/stats`  
**Description**: Fetches aggregated security metrics including bans, failing logins, and roles.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Security stats retrieved successfully",
  "data": {
    "activeBans": 15,
    "twoFactorEnabledUsers": 0,
    "recentFailedLogins": 45,
    "userRoles": {
      "USER": 1000,
      "ADMIN": 5,
      "SUPER_ADMIN": 2
    }
  }
}
```

---

## Credit Admin API

### Get Credit Statistics
**Endpoint**: `GET /api/v1/admin/credits/stats`  
**Description**: Fetches aggregated credit metrics including usage, circulation, and transactions.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credit stats retrieved successfully",
  "data": {
    "totalCreditsInCirculation": 50000,
    "totalCreditsPurchased": 120000,
    "totalCreditsUsed": 70000,
    "totalCreditTransactions": 1450,
    "transactionsByType": {
      "PURCHASE": 500,
      "DEDUCTION": 800,
      "BONUS": 100,
      "REFUND": 50
    },
    "activeCreditPacks": 3,
    "usersWithZeroBalance": 1250,
    "averageCreditBalance": 52.5
  }
}
```

### 1. Create Credit Pack
**`POST /api/v1/admin/credits/packs`**

```json
{
  "name": "STARTER_PACK",
  "displayName": "Starter Pack",
  "priceInCents": 500,
  "currency": "USD",
  "creditsIncluded": 20
}
```

### 2. Update Credit Pack
**`PUT /api/v1/admin/credits/packs/{id}`**

Partial update — only provided fields are changed.

```json
{
  "priceInCents": 35000,
  "isActive": false,
  "creditsIncluded": 250
}
```

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credit pack updated",
  "data": {
    "id": "8ac54c55-6bed-4d02-8924-4cc98aad1d4f",
    "name": "POWER_PACK",
    "displayName": "Power Pack",
    "priceInCents": 35000,
    "currency": "USD",
    "creditsIncluded": 250,
    "costPerCredit": 140.00
  }
}
```

### 3. Deactivate Credit Pack
**`DELETE /api/v1/admin/credits/packs/{id}`**

Soft-deletes the pack (sets `isActive = false`).

### 4. List All Packs (Including Inactive)
**`GET /api/v1/admin/credits/packs`**

### 5. Get Pack by ID
**`GET /api/v1/admin/credits/packs/{id}`**

### 6. Allocate Credits to a User
**`POST /api/v1/admin/credits/allocate`**

Allocates credits to any user without restrictions (no cap).

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "userId": "uuid",
  "amount": 100,
  "reason": "Support credit for service disruption"
}
```

| Field    | Type    | Required | Notes                        |
|----------|---------|----------|------------------------------|
| `userId` | string  | ✅        | Target user's ID             |
| `amount` | integer | ✅        | Credits to allocate (min: 1) |
| `reason` | string  | ❌        | Optional admin note          |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Successfully allocated 100 credits to user"
}
```

### 4. Transfer Credits to Another User
**`POST /api/v1/credits/transfer`**

Transfers credits to another user by email. Both sender and receiver receive push notifications and emails.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "recipientEmail": "jane@example.com",
  "amount": 10
}
```

| Field            | Type    | Required | Notes                        |
|------------------|---------|----------|------------------------------|
| `recipientEmail` | string  | ✅        | Email of the recipient user  |
| `amount`         | integer | ✅        | Credits to transfer (min: 1) |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credits transferred successfully",
  "data": {
    "transferId": "3127d38f-4a9c-46ab-aaee-e274ec91110e",
    "creditsTransferred": 80,
    "senderBalanceAfter": 2147483567,
    "recipientEmail": "afiaaniebiet0@gmail.com",
    "recipientName": "Michael Whyte"
  }
}
```
---

## Promo Code Admin API

### Get Promo Code Statistics
**Endpoint**: `GET /api/v1/admin/coupons/stats`  
**Description**: Fetches aggregated coupon metrics including total usage and discount figures.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon stats retrieved successfully",
  "data": {
    "totalCoupons": 150,
    "activeCoupons": 120,
    "expiredCoupons": 20,
    "totalUsages": 1850,
    "totalDiscountGiven": 8500.00,
    "uniqueUsersWhoUsedCoupons": 950,
    "averageDiscountPercentage": 15.5,
    "couponsNearExpiry": 5,
    "couponsAtUsageLimit": 2
  }
}
```

### Create Promo Code

Creates a new promo code.

```http
POST /admin/coupons
```

**Request Fields:**

| Field                | Type       | Required | Description                                                                                          |
|----------------------|------------|----------|------------------------------------------------------------------------------------------------------|
| `customCode`         | `string`   | Yes      | Unique code for the coupon (alphanumeric, 5-20 chars)                                                |
| `description`        | `string`   | Yes      | Description of the coupon                                                                            |
| `discountPercentage` | `decimal`  | Yes      | Discount percentage (e.g., 15.00 for 15%)                                                            |
| `minPurchaseAmount`  | `decimal`  | No       | Minimum purchase amount to apply coupon (default: 0.00)                                              |
| `validFrom`          | `datetime` | No       | Start date/time for coupon validity (ISO 8601 UTC format). Defaults to creation time if not provided |
| `validUntil`         | `datetime` | No*      | End date/time for coupon validity (ISO 8601 UTC format)                                              |
| `validDurationValue` | `long`     | No*      | Duration value (e.g. 5, 13) for dynamic expiry matching validDurationUnit                            |
| `validDurationUnit`  | `string`   | No*      | Unit for duration (e.g. Seconds, Minutes, Days)                                                      |
| `maxUsageCount`      | `int`      | No       | Maximum total uses for the coupon (default: unlimited)                                               |
| `maxUsagePerUser`    | `int`      | No       | Maximum uses per user (default: unlimited)                                                           |
| `recipientCategory`  | `string`   | Yes      | Target users: `ALL_USERS`, `NEW_USERS`, `ALL_PAID_USERS`, `SPECIFIC_USERS`                           |
| `specificUserIds`    | `array`    | No       | List of user IDs (UUIDs) if `recipientCategory` is `SPECIFIC_USERS`                                  |
| `templateId`         | `string`   | No       | ID of coupon template to base this coupon on (if any)                                                |

**Note:** You must provide either `validUntil` OR `validDurationValue` & `validDurationUnit` for the coupon to be successfully created. Dates must strictly conform to ISO 8601 UTC formats truncated to seconds (e.g. "2026-03-28T23:59:59Z").

**Request Body:**

```json
{
  "customCode": "WELCOME",
  "description": "This is a welcome bonus for all new signups.",
  "discountPercentage": 3.00,
  "minPurchaseAmount": 0.01,
  "validFrom": "",
  "validUntil": "",
  "maxUsageCount": 88,
  "maxUsagePerUser": 1,
  "recipientCategory": "ALL_USERS",
  "specificUserIds": [],
  "templateId": "",
  "validDurationValue": 30,
  "validDurationUnit": "Days"
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Coupon created successfully",
  "data": {
    "id": "511b4e8b-a95b-4d30-beaa-1b6b51e9a256",
    "code": "WELCOME-QJ93NZRS",
    "description": "This is a welcome bonus for all new signups.",
    "recipientCategory": "ALL_USERS",
    "discountPercentage": 3.00,
    "minPurchaseAmount": 0.01,
    "maxUsageCount": 88,
    "maxUsagePerUser": 1,
    "currentUsageCount": 0,
    "validFrom": "2026-03-30T14:15:44Z",
    "validUntil": "2026-04-29T14:15:44Z",
    "templateId": null,
    "templateName": null,
    "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
    "createdByName": "Admin User",
    "createdAt": null,
    "updatedAt": null,
    "active": true,
    "currentlyValid": true,
    "customCode": true,
    "expired": false
  }
}
```

**Error Responses:**

| Status | Code                 | Description                |
|--------|----------------------|----------------------------|
| 400    | `INVALID_REQUEST`    | Missing required fields    |
| 409    | `COUPON_CODE_EXISTS` | Custom code already exists |
| 403    | `FORBIDDEN`          | User not authorized        |

---

### Update Promo Code

Updates an existing coupon.

```http
PUT /admin/coupons/{couponId}
```

**Path Parameters:**

| Parameter  | Type     | Description        |
|------------|----------|--------------------|
| `couponId` | `string` | UUID of the coupon |

**Request Body:**

```json
{
  "minPurchaseAmount": 10.00,
  "maxUsageCount": 2000
}
```

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon updated successfully",
  "data": {
    "id": "511b4e8b-a95b-4d30-beaa-1b6b51e9a256",
    "code": "WELCOME-QJ93NZRS",
    "description": "This is a welcome bonus for all new signups.",
    "recipientCategory": "ALL_USERS",
    "discountPercentage": 3.00,
    "minPurchaseAmount": 10.00,
    "maxUsageCount": 2000,
    "maxUsagePerUser": 1,
    "currentUsageCount": 0,
    "validFrom": "2026-03-30T14:15:44Z",
    "validUntil": "2026-04-29T14:15:44Z",
    "templateId": null,
    "templateName": null,
    "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
    "createdByName": "Admin User",
    "createdAt": "2026-03-30T14:15:44.52021Z",
    "updatedAt": "2026-03-30T14:15:44.52021Z",
    "active": true,
    "currentlyValid": true,
    "customCode": true,
    "expired": false
  }
}
```

---

### Deactivate Promo Code

Deactivates a promo code (soft delete).

```http
DELETE /admin/coupons/{couponId}
```

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon deactivated successfully",
  "data": null
}
```

---

### Get Promo Code by ID

```http
GET /admin/coupons/{couponId}
```

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon retrieved successfully",
  "data": {
    "id": "511b4e8b-a95b-4d30-beaa-1b6b51e9a256",
    "code": "WELCOME-QJ93NZRS",
    "description": "This is a welcome bonus for all new signups.",
    "recipientCategory": "ALL_USERS",
    "discountPercentage": 3.00,
    "minPurchaseAmount": 10.00,
    "maxUsageCount": 2000,
    "maxUsagePerUser": 1,
    "currentUsageCount": 0,
    "validFrom": "2026-03-30T14:15:44Z",
    "validUntil": "2026-04-29T14:15:44Z",
    "templateId": null,
    "templateName": null,
    "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
    "createdByName": "Admin User",
    "createdAt": "2026-03-30T14:15:44.52021Z",
    "updatedAt": "2026-03-30T14:20:32.591352Z",
    "active": true,
    "currentlyValid": true,
    "customCode": true,
    "expired": false
  }
}
```

---

### List All Promo Codes

```http
GET /admin/coupons
```

**Query Parameters:**

| Parameter           | Type      | Default | Description             |
|---------------------|-----------|---------|-------------------------|
| `page`              | `int`     | `0`     | Page number (0-indexed) |
| `size`              | `int`     | `20`    | Page size               |
| `isActive`          | `boolean` | -       | Filter by active status |
| `recipientCategory` | `string`  | -       | Filter by category      |

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupons retrieved successfully",
  "data": {
    "coupons": [
      {
        "id": "511b4e8b-a95b-4d30-beaa-1b6b51e9a256",
        "code": "WELCOME-QJ93NZRS",
        "description": "This is a welcome bonus for all new signups.",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 3.00,
        "minPurchaseAmount": 10.00,
        "maxUsageCount": 2000,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-30T14:15:44Z",
        "validUntil": "2026-04-29T14:15:44Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-30T14:15:44.52021Z",
        "updatedAt": "2026-03-30T14:20:32.591352Z",
        "active": true,
        "currentlyValid": true,
        "customCode": true,
        "expired": false
      },
      {
        "id": "aa15c1b3-121a-41dd-97e4-9eeaf28f6c60",
        "code": "WELCOME-RWEWX70W",
        "description": "March edition of 97% off",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 0.01,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-10T07:45:31Z",
        "validUntil": "2026-03-20T07:45:31Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-10T07:45:31.312999Z",
        "updatedAt": "2026-03-10T07:45:31.312999Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "616b8b4f-accc-4167-9146-dcedd3268d02",
        "code": "SUMMER21-S4ZR2Y2Z",
        "description": "March edition of 97% off",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 0.01,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-07T23:36:36Z",
        "validUntil": "2026-03-17T23:36:36Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-07T23:36:36.684327Z",
        "updatedAt": "2026-03-07T23:36:36.684327Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "3619ca97-0536-4c1d-91c3-0aece4f35bda",
        "code": "SUMMER21-CZEFL7SC",
        "description": "March edition of 97% off",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 0.01,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-07T23:22:33.421861Z",
        "validUntil": "2026-03-28T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-07T23:22:33.516284Z",
        "updatedAt": "2026-03-07T23:22:33.516284Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "70ae1d32-7779-44d8-a15a-50b82d31283d",
        "code": "SUMMER17-UHIREQYZ",
        "description": "March edition of 97% off",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 0.01,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-07T15:34:00Z",
        "validUntil": "2026-03-28T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-07T15:33:46.773895Z",
        "updatedAt": "2026-03-07T15:33:46.773895Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "49ceb4ab-bddf-4d13-82ab-f4e170c038f6",
        "code": "SUMMER16-BDTKYCD4",
        "description": "March edition of 97% off",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 0.01,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-07T16:30:00Z",
        "validUntil": "2026-03-28T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-07T15:27:16.137431Z",
        "updatedAt": "2026-03-07T15:27:16.137431Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "35cc2a7c-03e8-422b-9f78-46ebb3267621",
        "code": "PROMO17",
        "description": "Trial for significant % off",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 0.01,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-03T03:05:00Z",
        "validUntil": "2026-03-04T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-03T03:04:42.591252Z",
        "updatedAt": "2026-03-03T03:04:42.591252Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "1592f7b2-0e73-4389-91e9-a696e59e0e87",
        "code": "PROMO16",
        "description": "Trial for significant % off",
        "recipientCategory": "ALL_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 5.00,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-03T02:25:00Z",
        "validUntil": "2026-03-04T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-03T02:24:16.661728Z",
        "updatedAt": "2026-03-03T02:24:16.661728Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "10bc82d8-2988-4916-9c04-d06a87a7c37e",
        "code": "PROMO15",
        "description": "Trial for significant % off",
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 5.00,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-03T02:15:00Z",
        "validUntil": "2026-03-04T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-03T02:14:45.99813Z",
        "updatedAt": "2026-03-03T02:14:45.99813Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "41447f0a-a7fe-4cc1-ab8e-8d20aa2fc6d4",
        "code": "SUMMER174",
        "description": "Trial for significant % off",
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 10.00,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-03T02:01:00Z",
        "validUntil": "2026-03-04T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-03T02:00:40.617765Z",
        "updatedAt": "2026-03-03T02:00:40.617765Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "91162abc-4bb5-4a13-abb4-4ed225799a92",
        "code": "SUMMER17",
        "description": "Trial for significant % off",
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 10.00,
        "maxUsageCount": 3,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-03T02:41:00Z",
        "validUntil": "2026-03-04T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-03T01:42:04.554799Z",
        "updatedAt": "2026-03-03T01:42:04.555811Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "d710554b-656d-4cfb-9c0e-a4acc4cf2a06",
        "code": "SUMMER",
        "description": "Trial for significant % off",
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 97.42,
        "minPurchaseAmount": 10.00,
        "maxUsageCount": 1,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-03-03T02:08:00Z",
        "validUntil": "2026-03-04T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-03-03T01:09:08.110415Z",
        "updatedAt": "2026-03-03T01:09:08.110415Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      },
      {
        "id": "27f09844-39ec-4354-9038-e9deeb6c81bc",
        "code": "AHXQ1PF8",
        "description": null,
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 20.00,
        "minPurchaseAmount": null,
        "maxUsageCount": null,
        "maxUsagePerUser": 1,
        "currentUsageCount": 2,
        "validFrom": "2026-01-26T14:33:00Z",
        "validUntil": "2026-02-21T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T14:32:46.30911Z",
        "updatedAt": "2026-02-21T09:01:44.819841Z",
        "active": true,
        "currentlyValid": false,
        "customCode": false,
        "expired": true
      },
      {
        "id": "4a57f33b-c9c9-4e33-b46f-c84cabbc3d8b",
        "code": "8HV7YMBE",
        "description": null,
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 20.00,
        "minPurchaseAmount": null,
        "maxUsageCount": null,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-01-26T15:31:00Z",
        "validUntil": "2026-02-21T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T14:30:22.768485Z",
        "updatedAt": "2026-02-21T09:01:44.817841Z",
        "active": true,
        "currentlyValid": false,
        "customCode": false,
        "expired": true
      },
      {
        "id": "cdaf6808-15e1-40f1-a7e6-e40350c9342a",
        "code": "S6W5X9WN",
        "description": null,
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 20.00,
        "minPurchaseAmount": null,
        "maxUsageCount": null,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-01-27T00:00:00Z",
        "validUntil": "2026-02-21T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T14:18:16.423788Z",
        "updatedAt": "2026-02-21T09:01:44.814839Z",
        "active": true,
        "currentlyValid": false,
        "customCode": false,
        "expired": true
      },
      {
        "id": "2e4c7079-0429-4a9e-9225-bd3a125f6552",
        "code": "SQ7TH31U",
        "description": null,
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 20.00,
        "minPurchaseAmount": null,
        "maxUsageCount": null,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-01-27T00:00:00Z",
        "validUntil": "2026-02-21T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T14:18:16.408775Z",
        "updatedAt": "2026-02-21T09:01:44.805796Z",
        "active": true,
        "currentlyValid": false,
        "customCode": false,
        "expired": true
      },
      {
        "id": "8f7f90d2-a66c-4762-9875-9cdaa2efe440",
        "code": "N6NM25T7",
        "description": null,
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 20.00,
        "minPurchaseAmount": null,
        "maxUsageCount": null,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-01-27T00:00:00Z",
        "validUntil": "2026-02-21T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T14:18:16.382611Z",
        "updatedAt": "2026-02-21T09:01:44.803796Z",
        "active": true,
        "currentlyValid": false,
        "customCode": false,
        "expired": true
      },
      {
        "id": "9ef71b9c-57a4-45f9-b82b-3e0c52e55a6b",
        "code": "3OIA444B",
        "description": null,
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 20.00,
        "minPurchaseAmount": null,
        "maxUsageCount": null,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-01-27T00:00:00Z",
        "validUntil": "2026-02-21T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T14:18:16.365971Z",
        "updatedAt": "2026-02-21T09:01:44.801783Z",
        "active": true,
        "currentlyValid": false,
        "customCode": false,
        "expired": true
      },
      {
        "id": "3c1bfc85-60d4-4619-9303-1972ca747341",
        "code": "OAR88I6C",
        "description": null,
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 20.00,
        "minPurchaseAmount": null,
        "maxUsageCount": null,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-01-27T00:00:00Z",
        "validUntil": "2026-02-21T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T14:18:16.31875Z",
        "updatedAt": "2026-02-21T09:01:44.791202Z",
        "active": true,
        "currentlyValid": false,
        "customCode": false,
        "expired": true
      },
      {
        "id": "501ba914-a427-44e6-a8af-387ba88659e7",
        "code": "REACTIVATE19",
        "description": "Expired Subscription 19% off",
        "recipientCategory": "EXPIRED_SUBSCRIPTION",
        "discountPercentage": 19.00,
        "minPurchaseAmount": 10.00,
        "maxUsageCount": 1000,
        "maxUsagePerUser": 1,
        "currentUsageCount": 0,
        "validFrom": "2026-01-26T20:00:00Z",
        "validUntil": "2026-01-30T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T13:49:31.725775Z",
        "updatedAt": "2026-01-26T13:51:04.386516Z",
        "active": false,
        "currentlyValid": false,
        "customCode": true,
        "expired": true
      }
    ],
    "totalElements": 21,
    "totalPages": 2,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### Get Promo Code Usage History

```http
GET /admin/coupons/{couponId}/usage
```

**Query Parameters:**

| Parameter | Type  | Default | Description |
|-----------|-------|---------|-------------|
| `page`    | `int` | `0`     | Page number |
| `size`    | `int` | `20`    | Page size   |

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon usage retrieved successfully",
  "data": {
    "usages": [
      {
        "id": "ff7f428d-eea2-468a-a237-c30879d26792",
        "user": {
          "id": "37182e20-ed95-40aa-acdd-3afb1f8d0a5a",
          "email": "user-email@email.com",
          "name": "William French"
        },
        "originalAmount": 1395000.00,
        "discountAmount": 279000.00,
        "finalAmount": 1116000.00,
        "subscriptionPlan": null,
        "paymentReference": "PAY_275C8A26221343FC",
        "usedAt": "2026-01-28T10:19:55.537682Z"
      },
      {
        "id": "c60f1a26-3de0-4e20-9af0-d0aa7482c6fa",
        "user": {
          "id": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
          "email": "user-email@email.com",
          "name": "Michael Whyte"
        },
        "originalAmount": 1395000.00,
        "discountAmount": 279000.00,
        "finalAmount": 1116000.00,
        "subscriptionPlan": null,
        "paymentReference": "PAY_209C92D0845C4888",
        "usedAt": "2026-01-28T10:14:39.234937Z"
      }
    ],
    "totalUsageCount": 2,
    "totalDiscountAmount": 558000.00
  }
}
```

---

### Get Promo Code Analytics

```http
GET /admin/coupons/{couponId}/analytics
```

**Query Parameters:**

| Parameter   | Type   | Description             |
|-------------|--------|-------------------------|
| `startDate` | `date` | Start date (YYYY-MM-DD) |
| `endDate`   | `date` | End date (YYYY-MM-DD)   |

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Analytics retrieved successfully",
  "data": {
    "couponId": "27f09844-39ec-4354-9038-e9deeb6c81bc",
    "couponCode": "AHXQ1PF8",
    "totalUsageCount": 2,
    "uniqueUsersCount": 2,
    "totalDiscountAmount": 558000.00,
    "totalOriginalAmount": 2790000.00,
    "totalFinalAmount": 2232000.00,
    "revenueImpact": 2232000.00,
    "averageDiscountPerTransaction": 279000.00,
    "redemptionRate": 0.0,
    "potentialRecipientsCount": 0,
    "usageBySubscriptionPlan": null,
    "usageByRecipientCategory": null,
    "dailyAnalytics": [],
    "startDate": "2026-01-20",
    "endDate": "2026-01-28"
  }
}
```

---

### Bulk Generate Promo Codes

Generates multiple coupons from a template.

```http
POST /admin/coupons/bulk-generate
```

**Request Fields:**

| Field                | Type       | Required | Description                                                                |
|----------------------|------------|----------|----------------------------------------------------------------------------|
| `templateId`         | `string`   | Yes      | ID of the coupon template to use                                           |
| `quantity`           | `int`      | Yes      | Number of coupons to generate                                              |
| `discountPercentage` | `decimal`  | Yes      | Override discount percentage                                               |
| `minPurchaseAmount`  | `decimal`  | No       | Override minimum purchase amount                                           |
| `codePrefix`         | `string`   | No       | Prefix for generated coupon codes                                          |
| `validFrom`          | `datetime` | No       | Override start date/time for validity (ISO 8601)                           |
| `validUntil`         | `datetime` | No       | Override end date/time for validity (ISO 8601)                             |
| `recipientCategory`  | `string`   | No       | Target users: `ALL_USERS`, `NEW_USERS`, `ALL_PAID_USERS`, `SPECIFIC_USERS` |
| `autoDistribute`     | `boolean`  | No       | Whether to auto-distribute coupons via email/notification                  |

**Request Body:**

```json
{
  "templateId": "template-id",
  "quantity": 5,
  "discountPercentage": 20.00,
  "codePrefix": "PROMO",
  "validFrom": "2026-01-27T00:00:00Z",
  "validUntil": "2026-02-21T23:59:59Z",
  "recipientCategory": "NEW_USERS",
  "autoDistribute": true
}
```

**Response (202 Accepted):**

```json
{
  "statusCode": 202,
  "status": "success",
  "message": "Bulk generation job submitted",
  "data": {
    "jobId": "a2762500-db91-4e68-8653-f8d5fac6c601",
    "status": "PENDING",
    "totalRequested": 5,
    "successfullyCreated": 0,
    "failed": 0,
    "createdCouponCodes": [],
    "errors": [],
    "startedAt": "2026-01-26T15:18:16.2057465+01:00",
    "completedAt": null,
    "progressPercentage": 0
  }
}
```
