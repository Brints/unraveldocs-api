# UnravelDocs API Documentation

This document provides a comprehensive overview of all API endpoints available in the UnravelDocs application.

**Base URL:** `/api/v1`

---

## Table of Contents

1. [Root](#root)
2. [Plan Pricing](#plan-pricing)
3. [Authentication](#authentication)
4. [User Management](#user-management)
5. [Team Management](#team-management)
6. [Organization Management](#organization-management)
7. [Admin Management](#admin-management)
8. [Documents](#documents)
9. [OCR Processing](#ocr-processing)
10. [Word Export](#word-export)
11. [Payments - Stripe](#payments---stripe)
12. [Payments - Paystack](#payments---paystack)
13. [Receipts](#receipts)
14. [Subscription Management](#subscription-management)
15. [Search - Elasticsearch](#search---elasticsearch)
16. [Webhooks](#webhooks)

---

## Root

### Health Check

| **Method** | **Endpoint** | **Description**             |
|------------|--------------|-----------------------------|
| `GET`      | `/`          | Check if the API is running |

#### Response

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "UnravelDocs API is running",
  "data": "Current server time: 2024-01-01T12:00:00Z"
}
```

---

## Plan Pricing

Base path: `/api/v1/plans`

> **Note:** These endpoints require authentication. They are designed for use on pricing pages to display subscription plan prices in the user's preferred currency.

### Supported Currencies

The system supports 69 currencies for price conversion including:

| Region       | Currencies                                                 |
|--------------|------------------------------------------------------------|
| Americas     | USD, CAD, BRL, MXN, ARS, CLP, COP, PEN                     |
| Europe       | EUR, GBP, CHF, SEK, NOK, DKK, PLN, CZK, HUF, RON           |
| Asia Pacific | JPY, CNY, INR, KRW, SGD, HKD, TWD, THB, IDR, MYR, PHP, VND |
| Middle East  | AED, TRY, ILS, QAR, KWD, BHD, JOD, OMR                     |
| Africa       | NGN, ZAR, GHS, KES, EGP, MAD                               |

### Get All Plans with Pricing

| **Method** | **Endpoint** | **Auth Required** |
|------------|--------------|-------------------|
| `GET`      | `/plans`     | No                |

**Query Parameters:**

| Parameter  | Type   | Required | Default | Description                                              |
|------------|--------|----------|---------|----------------------------------------------------------|
| `currency` | String | No       | USD     | Currency code for price conversion (e.g., NGN, EUR, GBP) |

**Response:**

```json
{
  "individualPlans": [
    {
      "planId": "e543f7aa-0464-4347-b762-8b84e00d039a",
      "planName": "FREE",
      "displayName": "Free",
      "billingInterval": "MONTH",
      "price": {
        "originalAmountUsd": 0.00,
        "convertedAmount": 0.00,
        "currency": "NGN",
        "formattedPrice": "₦0.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "documentUploadLimit": 10,
      "ocrPageLimit": 50,
      "features": [
        "Basic document processing",
        "Limited OCR pages",
        "Email support"
      ],
      "active": true
    },
    {
      "planId": "d00889f8-136f-4da4-945c-4b58fadce44a",
      "planName": "STARTER_MONTHLY",
      "displayName": "Starter Monthly",
      "billingInterval": "MONTH",
      "price": {
        "originalAmountUsd": 9.00,
        "convertedAmount": 13950.00,
        "currency": "NGN",
        "formattedPrice": "₦13,950.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "documentUploadLimit": 30,
      "ocrPageLimit": 150,
      "features": [
        "Standard document processing",
        "Increased OCR pages",
        "Priority email support",
        "API access"
      ],
      "active": true
    },
    {
      "planId": "1e4bbf4b-6ad3-4b05-bc5f-b584e5ef16e0",
      "planName": "STARTER_YEARLY",
      "displayName": "Starter Yearly",
      "billingInterval": "YEAR",
      "price": {
        "originalAmountUsd": 90.00,
        "convertedAmount": 139500.00,
        "currency": "NGN",
        "formattedPrice": "₦139,500.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "documentUploadLimit": 360,
      "ocrPageLimit": 1800,
      "features": [
        "Standard document processing",
        "Increased OCR pages",
        "Priority email support",
        "API access"
      ],
      "active": true
    },
    {
      "planId": "31eccb32-62fa-4af6-89ff-64794e768342",
      "planName": "PRO_MONTHLY",
      "displayName": "Pro Monthly",
      "billingInterval": "MONTH",
      "price": {
        "originalAmountUsd": 19.00,
        "convertedAmount": 29450.00,
        "currency": "NGN",
        "formattedPrice": "₦29,450.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "documentUploadLimit": 100,
      "ocrPageLimit": 500,
      "features": [
        "Advanced document processing",
        "High OCR page limit",
        "Priority support",
        "Full API access",
        "Custom integrations"
      ],
      "active": true
    },
    {
      "planId": "f8f111c8-3aaa-4079-9158-f297493ecbfd",
      "planName": "PRO_YEARLY",
      "displayName": "Pro Yearly",
      "billingInterval": "YEAR",
      "price": {
        "originalAmountUsd": 190.00,
        "convertedAmount": 294500.00,
        "currency": "NGN",
        "formattedPrice": "₦294,500.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "documentUploadLimit": 1200,
      "ocrPageLimit": 6000,
      "features": [
        "Advanced document processing",
        "High OCR page limit",
        "Priority support",
        "Full API access",
        "Custom integrations"
      ],
      "active": true
    },
    {
      "planId": "650cac00-c5fc-4351-abfe-75dbe87f9a80",
      "planName": "BUSINESS_MONTHLY",
      "displayName": "Business Monthly",
      "billingInterval": "MONTH",
      "price": {
        "originalAmountUsd": 49.00,
        "convertedAmount": 75950.00,
        "currency": "NGN",
        "formattedPrice": "₦75,950.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "documentUploadLimit": 500,
      "ocrPageLimit": 2500,
      "features": [
        "Unlimited document processing",
        "Unlimited OCR pages",
        "24/7 premium support",
        "Full API access",
        "Custom integrations",
        "Dedicated account manager"
      ],
      "active": true
    },
    {
      "planId": "8a75a1e2-c2e7-402a-a765-642419747662",
      "planName": "BUSINESS_YEARLY",
      "displayName": "Business Yearly",
      "billingInterval": "YEAR",
      "price": {
        "originalAmountUsd": 490.00,
        "convertedAmount": 759500.00,
        "currency": "NGN",
        "formattedPrice": "₦759,500.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "documentUploadLimit": 6000,
      "ocrPageLimit": 30000,
      "features": [
        "Unlimited document processing",
        "Unlimited OCR pages",
        "24/7 premium support",
        "Full API access",
        "Custom integrations",
        "Dedicated account manager"
      ],
      "active": true
    }
  ],
  "teamPlans": [
    {
      "planId": "503abfde-4b6c-4b4e-969c-9a5bb7dcfe24",
      "planName": "TEAM_PREMIUM",
      "displayName": "Team Premium",
      "description": "Perfect for small teams. Includes 200 documents per month with up to 10 members.",
      "monthlyPrice": {
        "originalAmountUsd": 29.00,
        "convertedAmount": 44950.00,
        "currency": "NGN",
        "formattedPrice": "₦44,950.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "yearlyPrice": {
        "originalAmountUsd": 290.00,
        "convertedAmount": 449500.00,
        "currency": "NGN",
        "formattedPrice": "₦449,500.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "maxMembers": 10,
      "monthlyDocumentLimit": 200,
      "hasAdminPromotion": false,
      "hasEmailInvitations": false,
      "trialDays": 10,
      "features": [
        "Up to 10 team members",
        "200 documents per month",
        "10-day free trial",
        "Team collaboration",
        "Shared workspace"
      ],
      "active": true
    },
    {
      "planId": "f2103611-9259-413c-a218-b54cb7533d4b",
      "planName": "TEAM_ENTERPRISE",
      "displayName": "Team Enterprise",
      "description": "For larger teams that need unlimited documents, admin roles, and email invitations.",
      "monthlyPrice": {
        "originalAmountUsd": 79.00,
        "convertedAmount": 122450.00,
        "currency": "NGN",
        "formattedPrice": "₦122,450.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "yearlyPrice": {
        "originalAmountUsd": 790.00,
        "convertedAmount": 1224500.00,
        "currency": "NGN",
        "formattedPrice": "₦1,224,500.00",
        "exchangeRate": 1550.00,
        "rateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
      },
      "maxMembers": 15,
      "monthlyDocumentLimit": null,
      "hasAdminPromotion": true,
      "hasEmailInvitations": true,
      "trialDays": 10,
      "features": [
        "Up to 15 team members",
        "Unlimited documents",
        "Admin role promotion",
        "Email invitations",
        "10-day free trial",
        "Team collaboration",
        "Shared workspace"
      ],
      "active": true
    }
  ],
  "displayCurrency": "NGN",
  "exchangeRateTimestamp": "2026-01-01T12:17:58.8093467+01:00"
}
```

---

### Get Supported Currencies

| **Method** | **Endpoint**        | **Auth Required** |
|------------|---------------------|-------------------|
| `GET`      | `/plans/currencies` | No                |

Returns all supported currencies for the pricing dropdown.

**Response:**

```json
{
  "currencies": [
    {
      "code": "USD",
      "symbol": "$",
      "name": "United States Dollar"
    },
    {
      "code": "NGN",
      "symbol": "₦",
      "name": "Nigerian Naira"
    }
  ],
  "totalCount": 69
}
```

---

## Authentication

Base path: `/api/v1/auth`

### Generate Password

| **Method** | **Endpoint**              | **Auth Required** |
|------------|---------------------------|-------------------|
| `POST`     | `/auth/generate-password` | No                |

**Request Body:**

```json
{
  "length": 16,
  "excludedCharacters": [
    "0",
    "O",
    "l",
    "1"
  ]
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Password generated successfully",
  "data": {
    "password": "generated_password_here"
  }
}
```

---

### Register User

| **Method** | **Endpoint**   | **Auth Required** |
|------------|----------------|-------------------|
| `POST`     | `/auth/signup` | No                |

**Request Body:**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePassword123!",
  "confirmPassword": "SecurePassword123!",
  "otp": "123456",
  "phoneNumber": "+1234567890",
  "country": "United States"
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "User registered successfully",
  "data": {
    "id": "98c0ba52-69e0-4bfe-bb76-793431334e47",
    "profilePicture": null,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "user",
    "lastLogin": null,
    "isActive": false,
    "isVerified": false,
    "termsAccepted": true,
    "marketingOptIn": true,
    "isPlatformAdmin": false,
    "isOrganizationAdmin": false,
    "country": "NG",
    "profession": "Data Analyst",
    "organization": "Brints Tech",
    "createdAt": "2025-12-27T12:33:58.6206681+01:00",
    "updatedAt": "2025-12-27T12:33:58.6206681+01:00"
  }
}
```

---

### Login

| **Method** | **Endpoint**  | **Auth Required** |
|------------|---------------|-------------------|
| `POST`     | `/auth/login` | No                |

**Request Body:**

```json
{
  "email": "john.doe@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "User logged in successfully",
  "data": {
    "id": "98c0ba52-69e0-4bfe-bb76-793431334e47",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "user",
    "lastLogin": "2025-12-27T12:53:05.9651815+01:00",
    "isActive": true,
    "isVerified": true,
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "createdAt": "2025-12-27T11:33:58.862233Z",
    "updatedAt": "2025-12-27T12:53:05.972777+01:00"
  }
}
```

---

### Verify Email

| **Method** | **Endpoint**         | **Auth Required** |
|------------|----------------------|-------------------|
| `POST`     | `/auth/verify-email` | No                |

**Request Body:**

```json
{
  "email": "john.doe@example.com",
  "token": "verification_token"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Email verified successfully",
  "data": null
}
```

---

### Resend Verification Email

| **Method** | **Endpoint**                      | **Auth Required** |
|------------|-----------------------------------|-------------------|
| `POST`     | `/auth/resend-verification-email` | No                |

**Request Body:**

```json
{
  "email": "john.doe@example.com"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Verification email sent successfully"
}
```

---

### Refresh Token

| **Method** | **Endpoint**          | **Auth Required** |
|------------|-----------------------|-------------------|
| `POST`     | `/auth/refresh-token` | No                |

**Request Body:**

```json
{
  "refreshToken": "jwt_refresh_token"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Token refreshed successfully",
  "data": {
    "id": "98c0ba52-69e0-4bfe-bb76-793431334e47",
    "email": "aniebietafia87@gmail.com",
    "accessToken": "jwt-access-token",
    "refreshToken": "refresh-token",
    "tokenType": "Bearer",
    "accessExpirationInMs": 3600000
  }
}
```

---

### Logout

| **Method** | **Endpoint**   | **Auth Required**  |
|------------|----------------|--------------------|
| `POST`     | `/auth/logout` | Yes (Bearer Token) |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Logged out successfully"
}
```

---

## User Management

Base path: `/api/v1/user`

### Get Current User Profile

| **Method** | **Endpoint** | **Auth Required** |
|------------|--------------|-------------------|
| `GET`      | `/me`        | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "User profile retrieved successfully",
  "data": {
    "id": "30e08f7d-9821-49f8-b0b6-f0bbc98d0fa4",
    "profilePicture": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/profile_pictures/eb296c46-e1b9-44e3-99f4-7a67213290eb-unnamed.png",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "user",
    "lastLogin": "2026-01-01T17:08:22.98912Z",
    "country": "NG",
    "profession": null,
    "organization": null,
    "createdAt": "2025-12-27T11:32:27.697829Z",
    "updatedAt": "2026-01-01T17:08:23.003015Z",
    "verified": true
  }
}
```

---

### Forgot Password

| **Method** | **Endpoint**            | **Auth Required** | **Rate Limit**  |
|------------|-------------------------|-------------------|-----------------|
| `POST`     | `/user/forgot-password` | No                | 5 requests/hour |

**Request Body:**

```json
{
  "email": "john.doe@example.com"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Password reset email sent successfully"
}
```

---

### Reset Password

| **Method** | **Endpoint**           | **Auth Required** | **Rate Limit**   |
|------------|------------------------|-------------------|------------------|
| `POST`     | `/user/reset-password` | No                | 10 requests/hour |

**Request Body:**

```json
{
  "email": "john.doe@example.com",
  "token": "reset_token",
  "newPassword": "NewSecurePassword123!",
  "confirmPassword": "NewSecurePassword123!"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Password reset successfully"
}
```

---

### Change Password

| **Method** | **Endpoint**            | **Auth Required** | **Rate Limit**     |
|------------|-------------------------|-------------------|--------------------|
| `POST`     | `/user/change-password` | Yes               | 20 requests/minute |

**Request Body:**

```json
{
  "currentPassword": "CurrentPassword123!",
  "newPassword": "NewSecurePassword123!",
  "confirmPassword": "NewSecurePassword123!"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Password changed successfully"
}
```

---

### Update Profile

| **Method** | **Endpoint**             | **Auth Required** | **Rate Limit**     |
|------------|--------------------------|-------------------|--------------------|
| `PUT`      | `/user/profile/{userId}` | Yes               | 20 requests/minute |

**Content-Type:** `multipart/form-data` or `application/json`

**Request Body:**

```json
{
  "firstName": "John",
  "lastName": "Updated",
  "phoneNumber": "+1234567891",
  "country": "Canada"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Profile updated successfully",
  "data": {
    "id": "uuid",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Updated"
  }
}
```

---

### Delete User

| **Method** | **Endpoint**             | **Auth Required** | **Rate Limit**     |
|------------|--------------------------|-------------------|--------------------|
| `DELETE`   | `/user/profile/{userId}` | Yes               | 20 requests/minute |

**Response:**

```json
"User profile deleted successfully"
```

---

### Upload Profile Picture

| **Method** | **Endpoint**                    | **Auth Required** | **Rate Limit**     |
|------------|---------------------------------|-------------------|--------------------|
| `POST`     | `/user/profile/{userId}/upload` | Yes               | 20 requests/minute |

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field  | Type | Description                  |
|--------|------|------------------------------|
| `file` | File | Image file (JPEG, PNG, etc.) |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Profile picture uploaded successfully",
  "data": "https://s3.amazonaws.com/bucket/profile-pictures/uuid.jpg"
}
```

---

### Delete Profile Picture

| **Method** | **Endpoint**                    | **Auth Required** | **Rate Limit**     |
|------------|---------------------------------|-------------------|--------------------|
| `DELETE`   | `/user/profile/{userId}/delete` | Yes               | 20 requests/minute |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Profile picture deleted successfully"
}
```

---

## Team Management

Base path: `/api/v1/teams`

> **Note:** Team features require a Team Premium or Team Enterprise subscription. Email invitations and admin promotion are Enterprise-only features.

### Subscription Tiers

| Feature           | Team Premium | Team Enterprise |
|-------------------|--------------|-----------------|
| Price (Monthly)   | $29          | $79             |
| Price (Yearly)    | $290         | $790            |
| Max Members       | 10           | 15              |
| Document Limit    | 200/month    | Unlimited       |
| Admin Promotion   | ❌            | ✅               |
| Email Invitations | ❌            | ✅               |
| Free Trial        | 10 days      | 10 days         |

### Currency Conversion

Prices are stored in USD and can be displayed in the user's local currency. The system supports 60+ currencies with real-time exchange rates.

**Supported Currencies:** USD, EUR, GBP, NGN, INR, JPY, AUD, CAD, CNY, ZAR, GHS, KES, BRL, MXN, AED, SGD, CHF, and more.

### Initiate Team Creation

| **Method** | **Endpoint**      | **Auth Required** |
|------------|-------------------|-------------------|
| `POST`     | `/teams/initiate` | Yes               |

Sends OTP to user email to verify team creation.

**Request Body:**

```json
{
  "name": "Acme Corporation",
  "description": "Our company team",
  "subscriptionType": "PREMIUM",
  "billingCycle": "MONTHLY",
  "paymentGateway": "stripe",
  "paymentToken": "tok_visa"
}
```

| Field              | Type   | Required | Description                                      |
|--------------------|--------|----------|--------------------------------------------------|
| `name`             | String | Yes      | Team name (2-100 characters)                     |
| `description`      | String | No       | Team description (max 500 characters)            |
| `subscriptionType` | Enum   | Yes      | `PREMIUM` or `ENTERPRISE`                        |
| `billingCycle`     | Enum   | Yes      | `MONTHLY` or `YEARLY`                            |
| `paymentGateway`   | String | Yes      | `stripe` or `paystack`                           |
| `paymentToken`     | String | No       | Payment token from gateway (for immediate charge)|

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "OTP has been sent to your email. Please verify to complete team creation.",
  "data": null
}
```

---

### Verify OTP and Create Team

| **Method** | **Endpoint**    | **Auth Required** |
|------------|-----------------|-------------------|
| `POST`     | `/teams/verify` | Yes               |

**Request Body:**

```json
{
  "otp": "123456"
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Team created successfully. You have a 10-day free trial.",
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "description": "Our company team",
    "teamCode": "ACM12345",
    "subscriptionType": "PREMIUM",
    "billingCycle": "MONTHLY",
    "subscriptionStatus": "TRIALING",
    "subscriptionPrice": 29.00,
    "currency": "USD",
    "isActive": true,
    "isVerified": true,
    "isClosed": false,
    "autoRenew": true,
    "trialEndsAt": "2025-01-08T12:00:00Z",
    "nextBillingDate": "2025-01-08T12:00:00Z",
    "subscriptionEndsAt": null,
    "cancellationRequestedAt": null,
    "createdAt": "2024-12-29T12:00:00Z",
    "currentMemberCount": 1,
    "maxMembers": 10,
    "monthlyDocumentLimit": 200,
    "isOwner": true
  }
}
```

---

### Get Team Details

| **Method** | **Endpoint**      | **Auth Required** |
|------------|-------------------|-------------------|
| `GET`      | `/teams/{teamId}` | Yes (Member)      |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Team details retrieved successfully",
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "description": "Our company team",
    "teamCode": "ACM12345",
    "subscriptionType": "PREMIUM",
    "billingCycle": "MONTHLY",
    "subscriptionStatus": "ACTIVE",
    "subscriptionPrice": 29.00,
    "currency": "USD",
    "isActive": true,
    "isVerified": true,
    "isClosed": false,
    "autoRenew": true,
    "trialEndsAt": null,
    "nextBillingDate": "2025-02-08T12:00:00Z",
    "subscriptionEndsAt": null,
    "cancellationRequestedAt": null,
    "createdAt": "2024-12-29T12:00:00Z",
    "currentMemberCount": 5,
    "maxMembers": 10,
    "monthlyDocumentLimit": 200,
    "isOwner": true
  }
}
```

---

### Get My Teams

| **Method** | **Endpoint** | **Auth Required** |
|------------|--------------|-------------------|
| `GET`      | `/teams/my`  | Yes               |

Returns all teams the user belongs to.

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Teams retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "name": "Acme Corporation",
      "teamCode": "ACM12345",
      "subscriptionType": "PREMIUM",
      "subscriptionStatus": "ACTIVE",
      "currentMemberCount": 5,
      "maxMembers": 10,
      "isOwner": true
    }
  ]
}
```

---

### Get Team Members

| **Method** | **Endpoint**             | **Auth Required** |
|------------|--------------------------|-------------------|
| `GET`      | `/teams/{teamId}/members`| Yes (Member)      |

> **Note:** Email addresses are masked for non-owners (except own email).

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Team members retrieved successfully",
  "data": [
    {
      "id": "member-uuid",
      "userId": "user-uuid",
      "firstName": "John",
      "lastName": "Doe",
      "email": "j***e@e***e.com",
      "role": "MEMBER",
      "joinedAt": "2024-12-27T12:00:00Z"
    }
  ]
}
```

---

### Add Member

| **Method** | **Endpoint**              | **Auth Required** | **Role**     |
|------------|---------------------------|-------------------|--------------|
| `POST`     | `/teams/{teamId}/members` | Yes               | ADMIN, OWNER |

**Request Body:**

```json
{
  "email": "newmember@example.com"
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Member added successfully",
  "data": {
    "id": "member-uuid",
    "userId": "user-uuid",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "newmember@example.com",
    "role": "MEMBER",
    "joinedAt": "2024-12-29T12:00:00Z"
  }
}
```

---

### Remove Member

| **Method** | **Endpoint**                           | **Auth Required** | **Role**     |
|------------|----------------------------------------|-------------------|--------------|
| `DELETE`   | `/teams/{teamId}/members/{memberId}`   | Yes               | ADMIN, OWNER |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Member removed successfully",
  "data": null
}
```

---

### Batch Remove Members

| **Method** | **Endpoint**                  | **Auth Required** | **Role**     |
|------------|-------------------------------|-------------------|--------------|
| `DELETE`   | `/teams/{teamId}/members/batch`| Yes               | ADMIN, OWNER |

**Request Body:**

```json
{
  "memberIds": [
    "member-uuid-1",
    "member-uuid-2",
    "member-uuid-3"
  ]
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Members removed successfully",
  "data": null
}
```

---

### Promote to Admin (Enterprise Only)

| **Method** | **Endpoint**                                   | **Auth Required** | **Role** |
|------------|------------------------------------------------|-------------------|----------|
| `POST`     | `/teams/{teamId}/members/{memberId}/promote`   | Yes               | OWNER    |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Member promoted to admin successfully",
  "data": {
    "id": "member-uuid",
    "userId": "user-uuid",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com",
    "role": "ADMIN",
    "joinedAt": "2024-12-27T12:00:00Z"
  }
}
```

---

### Send Invitation (Enterprise Only)

| **Method** | **Endpoint**                    | **Auth Required** | **Role**     |
|------------|---------------------------------|-------------------|--------------|
| `POST`     | `/teams/{teamId}/invitations`   | Yes               | ADMIN, OWNER |

**Request Body:**

```json
{
  "email": "newmember@example.com"
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Invitation sent successfully",
  "data": "https://api.unraveldocs.com/api/v1/teams/invitations/{token}/accept"
}
```

---

### Accept Invitation

| **Method** | **Endpoint**                          | **Auth Required** |
|------------|---------------------------------------|-------------------|
| `POST`     | `/teams/invitations/{token}/accept`   | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Invitation accepted successfully",
  "data": {
    "id": "member-uuid",
    "userId": "user-uuid",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com",
    "role": "MEMBER",
    "joinedAt": "2024-12-29T12:00:00Z"
  }
}
```

---

### Cancel Subscription

| **Method** | **Endpoint**            | **Auth Required** | **Role** |
|------------|-------------------------|-------------------|----------|
| `POST`     | `/teams/{teamId}/cancel`| Yes               | OWNER    |

Cancels the subscription but service continues until the current billing period ends.

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Subscription cancelled. Service will continue until the end of the billing period.",
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "subscriptionStatus": "CANCELLED",
    "autoRenew": false,
    "subscriptionEndsAt": "2025-02-08T12:00:00Z",
    "cancellationRequestedAt": "2024-12-29T12:00:00Z"
  }
}
```

---

### Close Team

| **Method** | **Endpoint**       | **Auth Required** | **Role** |
|------------|--------------------|-------------------|----------|
| `DELETE`   | `/teams/{teamId}`  | Yes               | OWNER    |

Closes the team. Members remain but lose access until reactivated.

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Team closed successfully",
  "data": null
}
```

---

### Reactivate Team

| **Method** | **Endpoint**                  | **Auth Required** | **Role** |
|------------|-------------------------------|-------------------|----------|
| `POST`     | `/teams/{teamId}/reactivate`  | Yes               | OWNER    |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Team reactivated successfully",
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "subscriptionStatus": "ACTIVE",
    "isActive": true,
    "isClosed": false
  }
}
```

---

## Organization Management

Base path: `/api/v1/organizations`

> **Note:** Organizations are a legacy feature. For new implementations, use the Team Management endpoints above.

### Initiate Organization Creation

| **Method** | **Endpoint**              | **Auth Required** |
|------------|---------------------------|-------------------|
| `POST`     | `/organizations/initiate` | Yes               |

Sends OTP to user email to verify organization creation.

**Request Body:**

```json
{
  "name": "Acme Corporation",
  "description": "Our company organization",
  "subscriptionType": "PREMIUM",
  "paymentGateway": "stripe",
  "paymentToken": "tok_visa"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "OTP has been sent to your email. Please verify to complete organization creation.",
  "data": null
}
```

---

### Verify OTP and Create Organization

| **Method** | **Endpoint**            | **Auth Required** |
|------------|-------------------------|-------------------|
| `POST`     | `/organizations/verify` | Yes               |

**Request Body:**

```json
{
  "otp": "123456"
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Organization created successfully. You have a 10-day free trial.",
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "orgCode": "ABC12345",
    "subscriptionType": "PREMIUM",
    "inTrial": true,
    "trialEndsAt": "2025-01-06T12:00:00Z",
    "maxMembers": 10,
    "monthlyDocumentLimit": 200
  }
}
```

---

### Get Organization Details

| **Method** | **Endpoint**             | **Auth Required** |
|------------|--------------------------|-------------------|
| `GET`      | `/organizations/{orgId}` | Yes (Member)      |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Organization details retrieved successfully",
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "orgCode": "ABC12345",
    "subscriptionType": "PREMIUM",
    "active": true,
    "verified": true,
    "closed": false,
    "currentMemberCount": 5,
    "maxMembers": 10,
    "isOwner": true
  }
}
```

---

### Get My Organizations

| **Method** | **Endpoint**        | **Auth Required** |
|------------|---------------------|-------------------|
| `GET`      | `/organizations/my` | Yes               |

Returns all organizations the user belongs to.

---

### Get Organization Members

| **Method** | **Endpoint**                     | **Auth Required** |
|------------|----------------------------------|-------------------|
| `GET`      | `/organizations/{orgId}/members` | Yes (Member)      |

> **Note:** Email addresses are masked for non-owners (except own email).

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "data": [
    {
      "userId": "uuid",
      "firstName": "John",
      "lastName": "Doe",
      "email": "j***e@e***e.com",
      "role": "MEMBER",
      "joinedAt": "2024-12-27T12:00:00Z"
    }
  ]
}
```

---

### Add Member

| **Method** | **Endpoint**                     | **Auth Required** | **Role**     |
|------------|----------------------------------|-------------------|--------------|
| `POST`     | `/organizations/{orgId}/members` | Yes               | ADMIN, OWNER |

**Request Body:**

```json
{
  "userId": "uuid"
}
```

---

### Remove Member

| **Method** | **Endpoint**                                | **Auth Required** | **Role**     |
|------------|---------------------------------------------|-------------------|--------------|
| `DELETE`   | `/organizations/{orgId}/members/{memberId}` | Yes               | ADMIN, OWNER |

---

### Batch Remove Members

| **Method** | **Endpoint**                           | **Auth Required** | **Role**     |
|------------|----------------------------------------|-------------------|--------------|
| `DELETE`   | `/organizations/{orgId}/members/batch` | Yes               | ADMIN, OWNER |

**Request Body:**

```json
{
  "userIds": [
    "uuid1",
    "uuid2",
    "uuid3"
  ]
}
```

---

### Promote to Admin (Enterprise Only)

| **Method** | **Endpoint**                                        | **Auth Required** | **Role** |
|------------|-----------------------------------------------------|-------------------|----------|
| `POST`     | `/organizations/{orgId}/members/{memberId}/promote` | Yes               | OWNER    |

---

### Send Invitation (Enterprise Only)

| **Method** | **Endpoint**                         | **Auth Required** | **Role**     |
|------------|--------------------------------------|-------------------|--------------|
| `POST`     | `/organizations/{orgId}/invitations` | Yes               | ADMIN, OWNER |

**Request Body:**

```json
{
  "email": "newmember@example.com"
}
```

**Response:**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Invitation sent successfully",
  "data": "https://api.unraveldocs.com/api/v1/organizations/invitations/{token}/accept"
}
```

---

### Accept Invitation

| **Method** | **Endpoint**                                | **Auth Required** |
|------------|---------------------------------------------|-------------------|
| `POST`     | `/organizations/invitations/{token}/accept` | Yes               |

---

### Close Organization

| **Method** | **Endpoint**             | **Auth Required** | **Role** |
|------------|--------------------------|-------------------|----------|
| `DELETE`   | `/organizations/{orgId}` | Yes               | OWNER    |

Members remain but lose access until reactivated.

---

### Reactivate Organization

| **Method** | **Endpoint**                        | **Auth Required** | **Role** |
|------------|-------------------------------------|-------------------|----------|
| `POST`     | `/organizations/{orgId}/reactivate` | Yes               | OWNER    |

---

## Admin Management

Base path: `/api/v1/admin`

### Create Admin

| **Method** | **Endpoint**    | **Auth Required** |
|------------|-----------------|-------------------|
| `POST`     | `/admin/signup` | No                |

**Request Body:**

```json
{
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@example.com",
  "password": "AdminPassword123!",
  "confirmPassword": "AdminPassword123!",
  "phoneNumber": "+1234567890",
  "country": "United States"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Admin created successfully",
  "data": {
    "id": "uuid",
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "role": "ADMIN"
  }
}
```

---

### Change User Role

| **Method** | **Endpoint**         | **Auth Required** | **Roles**          |
|------------|----------------------|-------------------|--------------------|
| `PUT`      | `/admin/change-role` | Yes               | ADMIN, SUPER_ADMIN |

**Request Body:**

```json
{
  "userId": "uuid",
  "newRole": "MODERATOR"
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "User role changed successfully",
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "role": "MODERATOR"
  }
}
```

---

### Get All Users

| **Method** | **Endpoint**   | **Auth Required** | **Roles**                     |
|------------|----------------|-------------------|-------------------------------|
| `GET`      | `/admin/users` | Yes               | ADMIN, MODERATOR, SUPER_ADMIN |

**Query Parameters:**

| Parameter       | Type    | Default     | Description                   |
|-----------------|---------|-------------|-------------------------------|
| `page`          | int     | 0           | Page number                   |
| `size`          | int     | 10          | Page size                     |
| `sortBy`        | string  | `createdAt` | Field to sort by              |
| `sortDirection` | string  | `desc`      | Sort direction                |
| `role`          | string  | -           | Filter by role                |
| `isVerified`    | boolean | -           | Filter by verification status |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Users retrieved successfully",
  "data": {
    "users": [
      "..."
    ],
    "totalElements": 100,
    "totalPages": 10,
    "currentPage": 0
  }
}
```

---

### Get User Profile by Admin

| **Method** | **Endpoint**      | **Auth Required** | **Roles**          |
|------------|-------------------|-------------------|--------------------|
| `GET`      | `/admin/{userId}` | Yes               | ADMIN, SUPER_ADMIN |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "User profile retrieved successfully",
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER"
  }
}
```

---

### Generate OTP

| **Method** | **Endpoint**          | **Auth Required** | **Roles**   |
|------------|-----------------------|-------------------|-------------|
| `POST`     | `/admin/generate-otp` | Yes               | SUPER_ADMIN |

**Request Body:**

```json
{
  "length": 6,
  "count": 10
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "OTPs generated successfully",
  "data": [
    "123456",
    "234567",
    "345678"
  ]
}
```

---

### Fetch Active OTPs

| **Method** | **Endpoint**         | **Auth Required** | **Roles**          |
|------------|----------------------|-------------------|--------------------|
| `GET`      | `/admin/active-otps` | Yes               | SUPER_ADMIN, ADMIN |

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page`    | int  | 0       | Page number |
| `size`    | int  | 5       | Page size   |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Active OTPs fetched successfully",
  "data": {
    "otps": [
      "123456", "654321", "112233"
    ],
    "totalElements": 50,
    "totalPages": 10
  }
}
```

---

## Documents

Base path: `/api/v1/documents`

### Upload Documents

| **Method** | **Endpoint**        | **Auth Required** | **Rate Limit**    |
|------------|---------------------|-------------------|-------------------|
| `POST`     | `/documents/upload` | Yes               | 10 uploads/minute |

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field   | Type   | Description                       |
|---------|--------|-----------------------------------|
| `files` | File[] | Array of document files to upload |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Documents uploaded successfully",
  "data": {
    "collectionId": "uuid",
    "files": [
      {
        "documentId": "uuid",
        "fileName": "document.pdf",
        "fileUrl": "https://...",
        "status": "UPLOADED"
      }
    ]
  }
}
```

---

### Get Document Collection by ID

| **Method** | **Endpoint**                           | **Auth Required** |
|------------|----------------------------------------|-------------------|
| `GET`      | `/documents/collection/{collectionId}` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document collection retrieved successfully",
  "data": {
    "collectionId": "uuid",
    "createdAt": "2024-01-01T12:00:00Z",
    "files": [
      {
        "documentId": "uuid",
        "fileName": "document.pdf",
        "fileUrl": "https://...",
        "ocrProcessed": true
      }
    ]
  }
}
```

---

### Get All User Collections

| **Method** | **Endpoint**                | **Auth Required** |
|------------|-----------------------------|-------------------|
| `GET`      | `/documents/my-collections` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document collections retrieved successfully",
  "data": [
    {
      "collectionId": "uuid",
      "fileCount": 5,
      "createdAt": "2024-01-01T12:00:00Z"
    }
  ]
}
```

---

### Get File from Collection

| **Method** | **Endpoint**                                                 | **Auth Required** |
|------------|--------------------------------------------------------------|-------------------|
| `GET`      | `/documents/collection/{collectionId}/document/{documentId}` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "File retrieved successfully",
  "data": {
    "documentId": "uuid",
    "fileName": "document.pdf",
    "fileUrl": "https://...",
    "ocrProcessed": true,
    "extractedText": "..."
  }
}
```

---

### Delete Document Collection

| **Method** | **Endpoint**                           | **Auth Required** |
|------------|----------------------------------------|-------------------|
| `DELETE`   | `/documents/collection/{collectionId}` | Yes               |

**Response:** `204 No Content`

---

### Delete File from Collection

| **Method** | **Endpoint**                                                 | **Auth Required** |
|------------|--------------------------------------------------------------|-------------------|
| `DELETE`   | `/documents/collection/{collectionId}/document/{documentId}` | Yes               |

**Response:** `204 No Content`

---

### Clear All Collections

| **Method** | **Endpoint**           | **Auth Required** |
|------------|------------------------|-------------------|
| `DELETE`   | `/documents/clear-all` | Yes               |

**Response:** `204 No Content`

---

## OCR Processing

Base path: `/api/v1/collections`

### Extract Text from File

| **Method** | **Endpoint**                                                | **Auth Required** |
|------------|-------------------------------------------------------------|-------------------|
| `POST`     | `/collections/{collectionId}/document/{documentId}/extract` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Text extraction completed successfully",
  "data": {
    "documentId": "uuid",
    "extractedText": "The extracted text content...",
    "confidence": 0.95,
    "language": "en"
  }
}
```

---

### Upload and Extract Text from All Files

| **Method** | **Endpoint**                      | **Auth Required** |
|------------|-----------------------------------|-------------------|
| `POST`     | `/collections/upload/extract-all` | Yes               |

**Content-Type:** `multipart/form-data`

**Request Body:**

| Field   | Type   | Description             |
|---------|--------|-------------------------|
| `files` | File[] | Array of document files |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Documents uploaded and OCR extraction started",
  "data": {
    "collectionId": "uuid",
    "files": [
      "..."
    ]
  }
}
```

---

### Get OCR Results for Collection

| **Method** | **Endpoint**                                   | **Auth Required** |
|------------|------------------------------------------------|-------------------|
| `GET`      | `/collections/{collectionId}/document/results` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "OCR results retrieved successfully",
  "data": {
    "collectionId": "uuid",
    "results": [
      {
        "documentId": "uuid",
        "fileName": "document.pdf",
        "extractedText": "...",
        "status": "COMPLETED"
      }
    ]
  }
}
```

---

### Get OCR Data for Specific Document

| **Method** | **Endpoint**                                                 | **Auth Required** |
|------------|--------------------------------------------------------------|-------------------|
| `GET`      | `/collections/{collectionId}/document/{documentId}/ocr-data` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "OCR data retrieved successfully",
  "data": {
    "documentId": "uuid",
    "fileName": "document.pdf",
    "extractedText": "The extracted text...",
    "confidence": 0.95
  }
}
```

---

## Word Export

Base path: `/api/v1/collections/{collectionId}/documents/{documentId}/download`

### Download OCR Result as DOCX

| **Method** | **Endpoint**                                                       | **Auth Required** |
|------------|--------------------------------------------------------------------|-------------------|
| `GET`      | `/collections/{collectionId}/documents/{documentId}/download/docx` | Yes               |

**Response:** Binary DOCX file download

**Content-Type:** `application/vnd.openxmlformats-officedocument.wordprocessingml.document`

---

## Payments - Stripe

### Customer Management

Base path: `/api/v1/stripe/customer`

#### Get Customer Details

| **Method** | **Endpoint**               | **Auth Required** |
|------------|----------------------------|-------------------|
| `GET`      | `/stripe/customer/details` | Yes               |

**Response:**

```json
{
  "id": "cus_xxx",
  "email": "user@example.com",
  "name": "John Doe",
  "defaultPaymentMethod": "pm_xxx",
  "paymentMethods": [
    "..."
  ]
}
```

---

#### Attach Payment Method

| **Method** | **Endpoint**                             | **Auth Required** |
|------------|------------------------------------------|-------------------|
| `POST`     | `/stripe/customer/payment-method/attach` | Yes               |

**Query Parameters:**

| Parameter         | Type   | Required | Description              |
|-------------------|--------|----------|--------------------------|
| `paymentMethodId` | string | Yes      | Stripe payment method ID |

**Response:** `200 OK`

---

#### Set Default Payment Method

| **Method** | **Endpoint**                                  | **Auth Required** |
|------------|-----------------------------------------------|-------------------|
| `POST`     | `/stripe/customer/payment-method/set-default` | Yes               |

**Query Parameters:**

| Parameter         | Type   | Required | Description              |
|-------------------|--------|----------|--------------------------|
| `paymentMethodId` | string | Yes      | Stripe payment method ID |

**Response:** `200 OK`

---

### One-Time Payments

Base path: `/api/v1/stripe/payment`

#### Create Payment Intent

| **Method** | **Endpoint**                            | **Auth Required** |
|------------|-----------------------------------------|-------------------|
| `POST`     | `/stripe/payment/create-payment-intent` | Yes               |

**Request Body:**

```json
{
  "amount": 2000,
  "currency": "usd",
  "description": "One-time payment",
  "metadata": {}
}
```

**Response:**

```json
{
  "paymentIntentId": "pi_xxx",
  "clientSecret": "pi_xxx_secret_xxx",
  "amount": 2000,
  "currency": "usd",
  "status": "requires_payment_method"
}
```

---

#### Get Payment Intent

| **Method** | **Endpoint**                               | **Auth Required** |
|------------|--------------------------------------------|-------------------|
| `GET`      | `/stripe/payment/intent/{paymentIntentId}` | Yes               |

**Response:**

```json
{
  "paymentIntentId": "pi_xxx",
  "amount": 2000,
  "currency": "usd",
  "status": "succeeded"
}
```

---

#### Process Refund

| **Method** | **Endpoint**             | **Auth Required** |
|------------|--------------------------|-------------------|
| `POST`     | `/stripe/payment/refund` | Yes               |

**Request Body:**

```json
{
  "paymentIntentId": "pi_xxx",
  "amount": 1000,
  "reason": "requested_by_customer"
}
```

**Response:**

```json
{
  "refundId": "re_xxx",
  "amount": 1000,
  "status": "succeeded"
}
```

---

#### Get Payment History

| **Method** | **Endpoint**              | **Auth Required** |
|------------|---------------------------|-------------------|
| `GET`      | `/stripe/payment/history` | Yes               |

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page`    | int  | 0       | Page number |
| `size`    | int  | 20      | Page size   |

**Response:** Paginated list of `StripePayment` objects

---

### Subscriptions

Base path: `/api/v1/stripe/subscription`

#### Create Checkout Session

| **Method** | **Endpoint**                                   | **Auth Required** |
|------------|------------------------------------------------|-------------------|
| `POST`     | `/stripe/subscription/create-checkout-session` | Yes               |

**Request Body:**

```json
{
  "priceId": "price_xxx",
  "mode": "subscription",
  "successUrl": "https://example.com/success",
  "cancelUrl": "https://example.com/cancel",
  "quantity": 1,
  "trialPeriodDays": 14,
  "promoCode": "DISCOUNT10",
  "metadata": {}
}
```

**Response:**

```json
{
  "sessionId": "cs_xxx",
  "url": "https://checkout.stripe.com/...",
  "customerId": "cus_xxx",
  "expiresAt": 1234567890
}
```

---

#### Create Subscription Directly

| **Method** | **Endpoint**                  | **Auth Required** |
|------------|-------------------------------|-------------------|
| `POST`     | `/stripe/subscription/create` | Yes               |

**Request Body:**

```json
{
  "priceId": "price_xxx",
  "paymentMethodId": "pm_xxx",
  "trialDays": 14
}
```

**Response:**

```json
{
  "subscriptionId": "sub_xxx",
  "status": "active",
  "currentPeriodEnd": "2024-02-01T12:00:00Z"
}
```

---

#### Cancel Subscription

| **Method** | **Endpoint**                                   | **Auth Required** |
|------------|------------------------------------------------|-------------------|
| `POST`     | `/stripe/subscription/{subscriptionId}/cancel` | Yes               |

**Query Parameters:**

| Parameter     | Type    | Default | Description                         |
|---------------|---------|---------|-------------------------------------|
| `immediately` | boolean | false   | Cancel immediately or at period end |

**Response:**

```json
{
  "subscriptionId": "sub_xxx",
  "status": "canceled",
  "canceledAt": "2024-01-15T12:00:00Z"
}
```

---

#### Pause Subscription

| **Method** | **Endpoint**                                  | **Auth Required** |
|------------|-----------------------------------------------|-------------------|
| `POST`     | `/stripe/subscription/{subscriptionId}/pause` | Yes               |

**Response:**

```json
{
  "subscriptionId": "sub_xxx",
  "status": "paused"
}
```

---

#### Resume Subscription

| **Method** | **Endpoint**                                   | **Auth Required** |
|------------|------------------------------------------------|-------------------|
| `POST`     | `/stripe/subscription/{subscriptionId}/resume` | Yes               |

**Response:**

```json
{
  "subscriptionId": "sub_xxx",
  "status": "active"
}
```

---

#### Get Subscription Details

| **Method** | **Endpoint**                            | **Auth Required** |
|------------|-----------------------------------------|-------------------|
| `GET`      | `/stripe/subscription/{subscriptionId}` | Yes               |

**Response:**

```json
{
  "subscriptionId": "sub_xxx",
  "status": "active",
  "currentPeriodStart": "2024-01-01T12:00:00Z",
  "currentPeriodEnd": "2024-02-01T12:00:00Z"
}
```

---

## Payments - Paystack

Base path: `/api/v1/paystack`

### Transactions

#### Initialize Transaction

| **Method** | **Endpoint**                       | **Auth Required** |
|------------|------------------------------------|-------------------|
| `POST`     | `/paystack/transaction/initialize` | Yes               |

**Request Body:**

```json
{
  "amount": 500000,
  "email": "user@example.com",
  "currency": "NGN",
  "callbackUrl": "https://example.com/callback",
  "metadata": {}
}
```

**Response:**

```json
{
  "status": true,
  "message": "Transaction initialized successfully",
  "data": {
    "authorizationUrl": "https://checkout.paystack.com/xxx",
    "accessCode": "xxx",
    "reference": "ref_xxx"
  }
}
```

---

#### Verify Transaction

| **Method** | **Endpoint**                               | **Auth Required** |
|------------|--------------------------------------------|-------------------|
| `GET`      | `/paystack/transaction/verify/{reference}` | Yes               |

**Response:**

```json
{
  "status": true,
  "message": "Transaction verified successfully",
  "data": {
    "reference": "ref_xxx",
    "amount": 500000,
    "status": "success",
    "currency": "NGN"
  }
}
```

---

#### Charge Authorization

| **Method** | **Endpoint**                                 | **Auth Required** |
|------------|----------------------------------------------|-------------------|
| `POST`     | `/paystack/transaction/charge-authorization` | Yes               |

**Query Parameters:**

| Parameter           | Type   | Required | Description                     |
|---------------------|--------|----------|---------------------------------|
| `authorizationCode` | string | Yes      | Previously authorized card code |
| `amount`            | long   | Yes      | Amount in kobo                  |
| `currency`          | string | No       | Currency code                   |

**Response:**

```json
{
  "status": true,
  "message": "Authorization charged successfully",
  "data": {
    "reference": "ref_xxx",
    "amount": 500000,
    "status": "success"
  }
}
```

---

#### Get Payment History

| **Method** | **Endpoint**                    | **Auth Required** |
|------------|---------------------------------|-------------------|
| `GET`      | `/paystack/transaction/history` | Yes               |

**Response:** Paginated list of `PaystackPayment` objects

---

#### Get Payment by Reference

| **Method** | **Endpoint**                        | **Auth Required** |
|------------|-------------------------------------|-------------------|
| `GET`      | `/paystack/transaction/{reference}` | Yes               |

**Response:** `PaystackPayment` object or `404 Not Found`

---

### Paystack Subscriptions

#### Create Subscription

| **Method** | **Endpoint**             | **Auth Required** |
|------------|--------------------------|-------------------|
| `POST`     | `/paystack/subscription` | Yes               |

**Request Body:**

```json
{
  "planCode": "PLN_xxx",
  "email": "user@example.com"
}
```

**Response:**

```json
{
  "status": true,
  "message": "Subscription created successfully",
  "data": {
    "subscriptionCode": "SUB_xxx",
    "emailToken": "xxx",
    "status": "active"
  }
}
```

---

#### Get Subscription by Code

| **Method** | **Endpoint**                                | **Auth Required** |
|------------|---------------------------------------------|-------------------|
| `GET`      | `/paystack/subscription/{subscriptionCode}` | Yes               |

**Response:** `PaystackSubscription` object or `404 Not Found`

---

#### Get Active Subscription

| **Method** | **Endpoint**                    | **Auth Required** |
|------------|---------------------------------|-------------------|
| `GET`      | `/paystack/subscription/active` | Yes               |

**Response:** `PaystackSubscription` object or `404 Not Found`

---

#### Get Subscription History

| **Method** | **Endpoint**              | **Auth Required** |
|------------|---------------------------|-------------------|
| `GET`      | `/paystack/subscriptions` | Yes               |

**Response:** Paginated list of `PaystackSubscription` objects

---

#### Enable Subscription

| **Method** | **Endpoint**                                       | **Auth Required** |
|------------|----------------------------------------------------|-------------------|
| `POST`     | `/paystack/subscription/{subscriptionCode}/enable` | Yes               |

**Query Parameters:**

| Parameter    | Type   | Required | Description               |
|--------------|--------|----------|---------------------------|
| `emailToken` | string | Yes      | Email token from Paystack |

**Response:**

```json
{
  "status": true,
  "message": "Subscription enabled successfully",
  "data": {
    "..."
  }
}
```

---

#### Disable Subscription

| **Method** | **Endpoint**                                        | **Auth Required** |
|------------|-----------------------------------------------------|-------------------|
| `POST`     | `/paystack/subscription/{subscriptionCode}/disable` | Yes               |

**Query Parameters:**

| Parameter    | Type   | Required | Description               |
|--------------|--------|----------|---------------------------|
| `emailToken` | string | Yes      | Email token from Paystack |

**Response:**

```json
{
  "status": true,
  "message": "Subscription disabled successfully",
  "data": {
    "..."
  }
}
```

---

#### Payment Callback

| **Method** | **Endpoint**         | **Auth Required** |
|------------|----------------------|-------------------|
| `GET`      | `/paystack/callback` | No                |

**Query Parameters:**

| Parameter   | Type   | Required | Description                       |
|-------------|--------|----------|-----------------------------------|
| `reference` | string | Yes      | Transaction reference             |
| `trxref`    | string | No       | Alternative transaction reference |

**Response:**

```json
{
  "status": true,
  "message": "Payment success",
  "data": {
    "..."
  }
}
```

---

## Receipts

Base path: `/api/v1/receipts`

### Get User's Receipts

| **Method** | **Endpoint** | **Auth Required** |
|------------|--------------|-------------------|
| `GET`      | `/receipts`  | Yes               |

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page`    | int  | 0       | Page number |
| `size`    | int  | 10      | Page size   |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Receipts retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "receiptNumber": "RCP-xxx",
      "paymentProvider": "STRIPE",
      "amount": 2000,
      "currency": "USD",
      "paymentMethod": "card",
      "description": "Subscription payment",
      "receiptUrl": "https://...",
      "paidAt": "2024-01-15T12:00:00Z",
      "createdAt": "2024-01-15T12:00:00Z"
    }
  ]
}
```

---

### Get Receipt by Number

| **Method** | **Endpoint**                | **Auth Required** |
|------------|-----------------------------|-------------------|
| `GET`      | `/receipts/{receiptNumber}` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Receipt retrieved successfully",
  "data": {
    "id": "uuid",
    "receiptNumber": "RCP-xxx",
    "paymentProvider": "STRIPE",
    "amount": 2000,
    "currency": "USD"
  }
}
```

---

### Download Receipt

| **Method** | **Endpoint**                         | **Auth Required** |
|------------|--------------------------------------|-------------------|
| `GET`      | `/receipts/{receiptNumber}/download` | Yes               |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Receipt download URL retrieved",
  "data": "https://s3.amazonaws.com/bucket/receipts/xxx.pdf"
}
```

---

## Subscription Management

Base path: `/api/v1/admin/subscriptions`

**Note:** All endpoints require ADMIN or SUPER_ADMIN role.

### Create Subscription Plan

| **Method** | **Endpoint**                 | **Auth Required** | **Roles**          |
|------------|------------------------------|-------------------|--------------------|
| `POST`     | `/admin/subscriptions/plans` | Yes               | ADMIN, SUPER_ADMIN |

**Request Body:**

```json
{
  "name": "Pro Plan",
  "description": "Pro features",
  "price": 1999,
  "currency": "USD",
  "interval": "monthly",
  "features": [
    "feature1",
    "feature2"
  ]
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Subscription plan created successfully",
  "data": {
    "id": "uuid",
    "name": "Pro Plan",
    "price": 1999,
    "currency": "USD"
  }
}
```

---

### Update Subscription Plan

| **Method** | **Endpoint**                          | **Auth Required** | **Roles**          |
|------------|---------------------------------------|-------------------|--------------------|
| `PUT`      | `/admin/subscriptions/plans/{planId}` | Yes               | ADMIN, SUPER_ADMIN |

**Request Body:**

```json
{
  "name": "Pro Plan Updated",
  "price": 2499
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Subscription plan updated successfully",
  "data": {
    "..."
  }
}
```

---

### Assign Subscriptions to Existing Users

| **Method** | **Endpoint**                                                  | **Auth Required** | **Roles**          |
|------------|---------------------------------------------------------------|-------------------|--------------------|
| `POST`     | `/admin/subscriptions/assign-subscriptions-to-existing-users` | Yes               | ADMIN, SUPER_ADMIN |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Subscriptions assigned successfully",
  "data": {
    "..."
  }
}
```

---

## Search - Elasticsearch

**Note:** These endpoints require Elasticsearch to be configured (`spring.elasticsearch.uris`).

### Document Search

Base path: `/api/v1/search/documents`

#### Search Documents (POST)

| **Method** | **Endpoint**        | **Auth Required** |
|------------|---------------------|-------------------|
| `POST`     | `/search/documents` | Yes               |

**Request Body:**

```json
{
  "query": "search term",
  "page": 0,
  "size": 10,
  "sortBy": "createdAt",
  "sortDirection": "desc",
  "filters": {}
}
```

**Response:**

```json
{
  "content": [
    "..."
  ],
  "totalElements": 100,
  "totalPages": 10,
  "page": 0,
  "size": 10
}
```

---

#### Search Document Content

| **Method** | **Endpoint**                | **Auth Required** |
|------------|-----------------------------|-------------------|
| `GET`      | `/search/documents/content` | Yes               |

**Query Parameters:**

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| `query`   | string | Yes      | Search query             |
| `page`    | int    | No       | Page number (default: 0) |
| `size`    | int    | No       | Page size (default: 10)  |

---

#### Quick Document Search

| **Method** | **Endpoint**        | **Auth Required** |
|------------|---------------------|-------------------|
| `GET`      | `/search/documents` | Yes               |

**Query Parameters:**

| Parameter       | Type   | Default     | Description    |
|-----------------|--------|-------------|----------------|
| `query`         | string | -           | Search query   |
| `page`          | int    | 0           | Page number    |
| `size`          | int    | 10          | Page size      |
| `sortBy`        | string | `createdAt` | Sort field     |
| `sortDirection` | string | `desc`      | Sort direction |

---

### Admin Search

Base path: `/api/v1/admin/search`

**Note:** All endpoints require ADMIN role.

#### Search Users (POST)

| **Method** | **Endpoint**          | **Auth Required** | **Roles** |
|------------|-----------------------|-------------------|-----------|
| `POST`     | `/admin/search/users` | Yes               | ADMIN     |

**Request Body:**

```json
{
  "query": "john",
  "page": 0,
  "size": 10,
  "filters": {
    "role": "USER",
    "isActive": true
  }
}
```

---

#### Quick User Search

| **Method** | **Endpoint**          | **Auth Required** | **Roles** |
|------------|-----------------------|-------------------|-----------|
| `GET`      | `/admin/search/users` | Yes               | ADMIN     |

**Query Parameters:**

| Parameter       | Type    | Default     | Description             |
|-----------------|---------|-------------|-------------------------|
| `query`         | string  | -           | Search query            |
| `role`          | string  | -           | Filter by role          |
| `isActive`      | boolean | -           | Filter by active status |
| `country`       | string  | -           | Filter by country       |
| `page`          | int     | 0           | Page number             |
| `size`          | int     | 10          | Page size               |
| `sortBy`        | string  | `createdAt` | Sort field              |
| `sortDirection` | string  | `desc`      | Sort direction          |

---

#### Search Payments (POST)

| **Method** | **Endpoint**             | **Auth Required** | **Roles** |
|------------|--------------------------|-------------------|-----------|
| `POST`     | `/admin/search/payments` | Yes               | ADMIN     |

**Request Body:**

```json
{
  "query": "receipt_number",
  "page": 0,
  "size": 10,
  "filters": {
    "paymentProvider": "STRIPE",
    "status": "success"
  }
}
```

---

#### Quick Payment Search

| **Method** | **Endpoint**             | **Auth Required** | **Roles** |
|------------|--------------------------|-------------------|-----------|
| `GET`      | `/admin/search/payments` | Yes               | ADMIN     |

**Query Parameters:**

| Parameter         | Type   | Default     | Description                                |
|-------------------|--------|-------------|--------------------------------------------|
| `query`           | string | -           | Search query (receipt number, email, name) |
| `paymentProvider` | string | -           | Filter by provider                         |
| `status`          | string | -           | Filter by status                           |
| `currency`        | string | -           | Filter by currency                         |
| `page`            | int    | 0           | Page number                                |
| `size`            | int    | 10          | Page size                                  |
| `sortBy`          | string | `createdAt` | Sort field                                 |
| `sortDirection`   | string | `desc`      | Sort direction                             |

---

#### Find Payment by Receipt Number

| **Method** | **Endpoint**                                     | **Auth Required** | **Roles** |
|------------|--------------------------------------------------|-------------------|-----------|
| `GET`      | `/admin/search/payments/receipt/{receiptNumber}` | Yes               | ADMIN     |

---

### Elasticsearch Sync (Admin)

Base path: `/api/v1/admin/elasticsearch`

**Note:** All endpoints require ADMIN role.

#### Full Sync

| **Method** | **Endpoint**                | **Auth Required** | **Roles** |
|------------|-----------------------------|-------------------|-----------|
| `POST`     | `/admin/elasticsearch/sync` | Yes               | ADMIN     |

**Response:**

```json
{
  "message": "Full synchronization started in background",
  "status": "STARTED"
}
```

---

#### Sync Users

| **Method** | **Endpoint**                      | **Auth Required** | **Roles** |
|------------|-----------------------------------|-------------------|-----------|
| `POST`     | `/admin/elasticsearch/sync/users` | Yes               | ADMIN     |

**Response:**

```json
{
  "message": "User synchronization completed",
  "usersIndexed": 150
}
```

---

#### Sync Documents

| **Method** | **Endpoint**                          | **Auth Required** | **Roles** |
|------------|---------------------------------------|-------------------|-----------|
| `POST`     | `/admin/elasticsearch/sync/documents` | Yes               | ADMIN     |

**Response:**

```json
{
  "message": "Document synchronization completed",
  "documentsIndexed": 500
}
```

---

#### Sync Payments

| **Method** | **Endpoint**                         | **Auth Required** | **Roles** |
|------------|--------------------------------------|-------------------|-----------|
| `POST`     | `/admin/elasticsearch/sync/payments` | Yes               | ADMIN     |

**Response:**

```json
{
  "message": "Payment synchronization completed",
  "paymentsIndexed": 200
}
```

---

## Webhooks

### Stripe Webhook

| **Method** | **Endpoint**             | **Auth Required** | **Description**       |
|------------|--------------------------|-------------------|-----------------------|
| `POST`     | `/api/v1/stripe/webhook` | No                | Handles Stripe events |

**Headers:**

| Header             | Description              |
|--------------------|--------------------------|
| `Stripe-Signature` | Stripe webhook signature |

**Supported Events:**

- `checkout.session.completed`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.payment_succeeded`
- `invoice.payment_failed`

---

### Paystack Webhook

| **Method** | **Endpoint**               | **Auth Required** | **Description**         |
|------------|----------------------------|-------------------|-------------------------|
| `POST`     | `/api/v1/paystack/webhook` | No                | Handles Paystack events |

**Headers:**

| Header                 | Description                           |
|------------------------|---------------------------------------|
| `x-paystack-signature` | Paystack webhook signature (optional) |

---

#### Paystack Webhook Health Check

| **Method** | **Endpoint**                      | **Auth Required** |
|------------|-----------------------------------|-------------------|
| `GET`      | `/api/v1/paystack/webhook/health` | No                |

**Response:**

```text
Webhook endpoint is healthy
```

---

### Mailgun Webhook

| **Method** | **Endpoint**           | **Auth Required** | **Description**              |
|------------|------------------------|-------------------|------------------------------|
| `POST`     | `/api/webhook/mailgun` | No                | Handles Mailgun email events |

**Supported Events:**

- `delivered`
- `opened`
- `clicked`
- `failed`

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request

```json
{
  "statusCode": 400,
  "status": "error",
  "message": "Validation error message",
  "errors": {
    "field": "Error description"
  }
}
```

### 401 Unauthorized

```json
{
  "statusCode": 401,
  "status": "error",
  "message": "Authentication required"
}
```

### 403 Forbidden

```json
{
  "statusCode": 403,
  "status": "error",
  "message": "You don't have permission to access this resource"
}
```

### 404 Not Found

```json
{
  "statusCode": 404,
  "status": "error",
  "message": "Resource not found"
}
```

### 429 Too Many Requests

```json
{
  "statusCode": 429,
  "status": "error",
  "message": "Rate limit exceeded. Please try again later."
}
```

### 500 Internal Server Error

```json
{
  "statusCode": 500,
  "status": "error",
  "message": "An unexpected error occurred"
}
```

---

## Authentication

All authenticated endpoints require a Bearer token in the `Authorization` header:

```text
Authorization: Bearer <access_token>
```

Access tokens are obtained through the `/api/v1/auth/login` endpoint and can be refreshed using
`/api/v1/auth/refresh-token`.

---

## Rate Limiting

Some endpoints have rate limiting applied:

| Endpoint                | Limit                  |
|-------------------------|------------------------|
| `/user/forgot-password` | 5 requests per hour    |
| `/user/reset-password`  | 10 requests per hour   |
| `/user/change-password` | 20 requests per minute |
| `/user/profile/*`       | 20 requests per minute |
| `/documents/upload`     | 10 requests per minute |

---

## Pagination

Paginated endpoints accept the following query parameters:

| Parameter | Type   | Default | Description                                       |
|-----------|--------|---------|---------------------------------------------------|
| `page`    | int    | 0       | Page number (0-indexed)                           |
| `size`    | int    | 10-20   | Number of items per page                          |
| `sort`    | string | varies  | Sort field and direction (e.g., `createdAt,desc`) |

Paginated responses include:

```json
{
  "content": [
    "..."
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0,
  "numberOfElements": 10,
  "first": true,
  "last": false,
  "empty": false
}
```
