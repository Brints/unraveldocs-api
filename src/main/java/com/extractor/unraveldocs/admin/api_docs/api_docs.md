# Admin API Documentation - User Management (Phase 2)

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

#### 16. Toggle Plan Status
Activates or deactivates a subscription plan.

- **URL:** `PATCH /api/v1/admin/subscriptions/plans/{planId}/status`
- **Method:** `PATCH`
- **Role:** `SUPER_ADMIN`
- **Body:** `ActionReasonDto`

### Phase 3C: Plan Subscribers

#### 17. List Users on Plan
Returns a paginated list of users subscribed to a specific plan.

- **URL:** `GET /api/v1/admin/subscriptions/plans/{planId}/subscribers`
- **Method:** `GET`
- **Role:** `ADMIN`, `SUPER_ADMIN`, `MODERATOR`
- **Query Params:** `page` (default 0), `size` (default 10)

