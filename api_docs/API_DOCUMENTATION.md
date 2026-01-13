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
13. [Payments - PayPal](#payments---paypal)
14. [Receipts](#receipts)
15. [Subscription Management](#subscription-management)
16. [Storage](#storage)
17. [Search - Elasticsearch](#search---elasticsearch)
18. [Webhooks](#webhooks)

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
  "token": "648c4568833e7de2664a8cdc9d8272a053523f5b6b68325c3b7f5f1d1d563258dfe101b7c8abb797"
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

**Content-Type:** `application/json`

> **Note:** For profile picture uploads, use the separate `POST /user/profile/{userId}/upload` endpoint.

**Request Body:**

| Field          | Type   | Required | Description                  |
|----------------|--------|----------|------------------------------|
| `firstName`    | String | No       | First name (2-80 characters) |
| `lastName`     | String | No       | Last name (2-80 characters)  |
| `country`      | String | No       | Country code or name         |
| `profession`   | String | No       | User's profession            |
| `organization` | String | No       | User's organization/company  |

**Example Request (JSON):**

```json
{
  "firstName": "John",
  "lastName": "Updated",
  "country": "Canada",
  "profession": "Software Engineer",
  "organization": "Acme Inc."
}
```

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Profile updated successfully",
  "data": {
    "id": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
    "profilePicture": null,
    "firstName": "Aniebiet",
    "lastName": "Whyte",
    "email": "afiaaniebiet0@gmail.com",
    "role": "user",
    "lastLogin": "2026-01-06T01:48:44.930213Z",
    "country": "US",
    "profession": "Software engineer",
    "organization": "Times",
    "createdAt": "2026-01-06T01:41:44.92849Z",
    "updatedAt": "2026-01-06T01:48:44.944396Z",
    "verified": true
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
| Price (Monthly)   | $29.99       | $79.99          |
| Price (Yearly)    | $289.99      | $789.99         |
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
  "subscriptionType": "TEAM_PREMIUM",
  "billingCycle": "MONTHLY",
  "paymentGateway": "paystack"
}
```

| Field              | Type   | Required | Description                                       |
|--------------------|--------|----------|---------------------------------------------------|
| `name`             | String | Yes      | Team name (2-100 characters)                      |
| `description`      | String | No       | Team description (max 500 characters)             |
| `subscriptionType` | Enum   | Yes      | `PREMIUM` or `ENTERPRISE`                         |
| `billingCycle`     | Enum   | Yes      | `MONTHLY` or `YEARLY`                             |
| `paymentGateway`   | String | Yes      | `stripe` or `paystack`                            |
| `paymentToken`     | String | No       | Payment token from gateway (for immediate charge) |

**Response:**

```json
{
  "statusCode": 200,
  "status": "Success",
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
  "message": "Team created successfully",
  "data": {
    "active": true,
    "autoRenew": true,
    "billingCycle": "Monthly",
    "cancellationRequestedAt": null,
    "closed": false,
    "createdAt": null,
    "currency": "USD",
    "currentMemberCount": 1,
    "description": "Our company team",
    "id": "55e54bc9-4319-41db-a5cc-686a31752ad2",
    "maxMembers": 10,
    "monthlyDocumentLimit": 200,
    "name": "Acme Corporation",
    "nextBillingDate": null,
    "owner": true,
    "subscriptionEndsAt": null,
    "subscriptionPrice": 29.00,
    "subscriptionStatus": "Trial",
    "subscriptionType": "Team Premium",
    "teamCode": "C97640E3",
    "trialEndsAt": "2026-01-16T03:24:58.2561064+01:00",
    "verified": true
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
  "message": "1 team(s) found",
  "data": [
    {
      "active": true,
      "autoRenew": true,
      "billingCycle": "Monthly",
      "cancellationRequestedAt": null,
      "closed": false,
      "createdAt": "2026-01-06T02:25:01.042135Z",
      "currency": "USD",
      "currentMemberCount": 1,
      "description": "Our company team",
      "id": "55e54bc9-4319-41db-a5cc-686a31752ad2",
      "maxMembers": 10,
      "monthlyDocumentLimit": 200,
      "name": "Acme Corporation",
      "nextBillingDate": null,
      "owner": true,
      "subscriptionEndsAt": null,
      "subscriptionPrice": 29.00,
      "subscriptionStatus": "Trial",
      "subscriptionType": "Team Premium",
      "teamCode": "C97640E3",
      "trialEndsAt": "2026-01-16T02:24:58.256106Z",
      "verified": true
    }
  ]
}
```

---

### Get Team Members

| **Method** | **Endpoint**              | **Auth Required** |
|------------|---------------------------|-------------------|
| `GET`      | `/teams/{teamId}/members` | Yes (Member)      |

> **Note:** Email addresses are masked for non-owners (except own email).

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "2 member(s) found",
  "data": [
    {
      "email": "af***@gmail.com",
      "firstName": "Aniebiet",
      "id": "a368785f-800b-4e54-b9bb-7192422d1fa7",
      "joinedAt": "2026-01-06T02:25:01.096031Z",
      "lastName": "Whyte",
      "role": "OWNER",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8"
    },
    {
      "email": "goldenlee87@gmail.com",
      "firstName": "William",
      "id": "51b0ec2b-b811-4b74-9e38-90c00d53bb96",
      "joinedAt": "2026-01-06T02:34:38.065886Z",
      "lastName": "French",
      "role": "MEMBER",
      "userId": "37182e20-ed95-40aa-acdd-3afb1f8d0a5a"
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
    "email": "goldenlee87@gmail.com",
    "firstName": "William",
    "id": "51b0ec2b-b811-4b74-9e38-90c00d53bb96",
    "joinedAt": null,
    "lastName": "French",
    "role": "MEMBER",
    "userId": "37182e20-ed95-40aa-acdd-3afb1f8d0a5a"
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

| **Method** | **Endpoint**                    | **Auth Required** | **Role**     |
|------------|---------------------------------|-------------------|--------------|
| `DELETE`   | `/teams/{teamId}/members/batch` | Yes               | ADMIN, OWNER |

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

| **Method** | **Endpoint**             | **Auth Required** | **Role** |
|------------|--------------------------|-------------------|----------|
| `POST`     | `/teams/{teamId}/cancel` | Yes               | OWNER    |

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
  "data": {
    "collectionId": "77cde05e-7178-4930-8ed2-4a3888b8144c",
    "files": [
      {
        "documentId": "1e50bf3b-ad75-4b1e-b888-5309567e9487",
        "originalFileName": "docs2.jpeg",
        "fileSize": 180086,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/98f8c7e6-acaf-4cfd-a73b-9d5d6da095e8-docs2.jpeg",
        "status": "success"
      }
    ],
    "overallStatus": "completed"
  },
  "message": "All 1 document(s) uploaded successfully.",
  "status": "success",
  "statusCode": 200
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
  "data": {
    "collectionStatus": "processed",
    "createdAt": "2026-01-03T00:01:22.734172Z",
    "files": [
      {
        "documentId": "427b799e-26e3-469f-bf0e-fddb8454722b",
        "originalFileName": "docs1.jpeg",
        "fileSize": 87367,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/727592e3-e884-4573-9845-226f11d1a4b7-docs1.jpeg",
        "status": "success"
      },
      {
        "documentId": "c57d2a24-79ba-47f8-9145-45d84ffafb9d",
        "originalFileName": "docs2.jpeg",
        "fileSize": 180086,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/a8b2d631-9e42-454a-b617-195ae61d0fc5-docs2.jpeg",
        "status": "success"
      }
    ],
    "id": "1d1c127c-47e1-4e59-bff7-3ea810d5ad89",
    "updatedAt": "2026-01-03T00:01:37.549325Z",
    "uploadTimestamp": "2026-01-03T00:01:22.520824Z",
    "userId": "30e08f7d-9821-49f8-b0b6-f0bbc98d0fa4"
  },
  "message": "Document collection retrieved successfully.",
  "status": "success",
  "statusCode": 200
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
  "data": [
    {
      "collectionStatus": "processed",
      "createdAt": "2026-01-03T00:01:22.734172Z",
      "fileCount": 2,
      "id": "1d1c127c-47e1-4e59-bff7-3ea810d5ad89",
      "updatedAt": "2026-01-03T00:01:37.549325Z",
      "uploadTimestamp": "2026-01-03T00:01:22.520824Z"
    },
    {
      "collectionStatus": "failed_ocr",
      "createdAt": "2026-01-02T23:45:29.603412Z",
      "fileCount": 2,
      "id": "b5b9aef6-2537-42ee-b9e5-f6b38432f2c2",
      "updatedAt": "2026-01-02T23:45:31.580341Z",
      "uploadTimestamp": "2026-01-02T23:45:29.553583Z"
    },
    {
      "collectionStatus": "completed",
      "createdAt": "2026-01-02T22:19:28.534082Z",
      "fileCount": 1,
      "id": "77cde05e-7178-4930-8ed2-4a3888b8144c",
      "updatedAt": "2026-01-02T22:19:28.534082Z",
      "uploadTimestamp": "2026-01-02T22:19:28.491113Z"
    }
  ],
  "message": "Document collections retrieved successfully.",
  "status": "success",
  "statusCode": 200
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
  "data": {
    "documentId": "427b799e-26e3-469f-bf0e-fddb8454722b",
    "originalFileName": "docs1.jpeg",
    "fileSize": 87367,
    "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/727592e3-e884-4573-9845-226f11d1a4b7-docs1.jpeg",
    "status": "success"
  },
  "message": "File retrieved successfully.",
  "status": "success",
  "statusCode": 200
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
  "data": {
    "id": "abc783c9-7aae-4ae7-b100-40d5bedb693a",
    "documentId": "1e50bf3b-ad75-4b1e-b888-5309567e9487",
    "status": "COMPLETED",
    "extractedText": "& Samia Kousar @ - 2nd + Follow\n\nHR Ops Coordinator @ Rubik's Technologies,\n“-\" 1d-Edited-@\n\nHiring on behalf of colleague:\n& Position : Senior Backend Engineer (Java)\nBB Job Duties:\n+ Work as part of an agile software team to improve digital\nsales platforms and contribute new ideas.\n+ Take responsibility for delivery processes, perform code\nreviews, refactoring, and deployments using CI/CD.\n+ Oversee the full software development lifecycle, providing\nsolutions for risks, code quality, and process optimization.\n* Collaborate with Product Owners and advise stakeholders\non technical topics.\n+ Contribute to IT security, DevOps, and service optimization\nand stabilization.\n+ Help define test strategies and implement IT security\nmeasures.\nBB Your Profile:\n+ 3+ years of experience in agile backend software\ndevelopment (Scrum/Kanban).\n+ Proficient in Java, Spring Boot, JPA (Hibernate), REST &\nOpenAPI, and AWS.\n+ Experienced with CI/CD, Git, GitLab Cl, Docker, Kubernetes,\nand PostgreSQL.\nFamiliar with architecture principles, microservices, and test\nautomation in microservice environments.\n+ Knowledge of implementing security measures such as\noverload protection and OWASP Top 10.\n+ Fluent in German and proficient in English, both spoken and\nwritten.\nBB What's Important to Us:\nWe provide equal opportunities to all candidates and promote\ndiversity and inclusion within our teams. We value every\napplication regardless of gender, nationality, ethnic or social\norigin, religion, belief, disability, age, or sexual orientation.\n",
    "errorMessage": null,
    "createdAt": "2026-01-02T23:26:30.564011+01:00",
    "updatedAt": "2026-01-02T23:26:30.575559+01:00"
  },
  "message": "Text extraction completed successfully.",
  "status": "success",
  "statusCode": 200
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
  "data": {
    "collectionId": "9a3a763b-2fd9-4f70-b01e-eb1582a8f927",
    "files": [
      {
        "documentId": "8cfb0ef4-e1c6-46b4-b24b-7d4ce1100cb6",
        "originalFileName": "docs1.jpeg",
        "fileSize": 87367,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/8d85b26f-4cfe-4544-8922-748b57830e34-docs1.jpeg",
        "status": "success"
      },
      {
        "documentId": "d43d3ee3-1b56-48c3-bbfd-7090e17d05cc",
        "originalFileName": "docs2.jpeg",
        "fileSize": 180086,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/6e6d2211-9443-4373-b447-bbc633bfe4b2-docs2.jpeg",
        "status": "success"
      }
    ],
    "overallStatus": "processing"
  },
  "message": "2 document(s) uploaded successfully and queued for processing. 0 failed.",
  "status": "processing",
  "statusCode": 202
}
```

---

### Get OCR Results for Collection

| **Method** | **Endpoint**                                   | **Auth Required** |
|------------|------------------------------------------------|-------------------|
| `GET`      | `/collections/{collectionId}/document/results` | Yes               |

**Response:**
### Example of Successful OCR Response
```json
{
  "data": {
    "collectionId": "1d1c127c-47e1-4e59-bff7-3ea810d5ad89",
    "files": [
      {
        "createdAt": "2026-01-03T00:01:22.918841Z",
        "documentId": "427b799e-26e3-469f-bf0e-fddb8454722b",
        "errorMessage": null,
        "extractedText": "Siva @ @sivalabs - 5h 12)\nMy favorite @Tech Stack:\n\nLanguage: Java/Kotlin\n\nFrameworks: Spring Boot, Quarkus\n\nDesign & Architecture: Modular Monolith,\nPackage-by-Feature\n\nDatabase: PostgreSQL, Flyway Migrations\nTesting: Integration Tests with Testcontainers,\nUnit Tests with Mockito, API Stubbing with\nWireMock\n\nArchitecture Tests: Spring Modulith Tests, Arch\nUnit Tests with Taikai\n\nLibrary Upgrades: Renovate, Dependabot,\nOpenRewrite\n\nIDE: IntelliJ IDEA\n\nBuild Tool: Maven\n\nCode Formatting: Spotless Plugin with Palantir\nJava Format\n\nHava #Architecture #KISS\n",
        "originalFileName": "docs1.jpeg",
        "status": "completed"
      },
      {
        "createdAt": "2026-01-03T00:01:22.928132Z",
        "documentId": "c57d2a24-79ba-47f8-9145-45d84ffafb9d",
        "errorMessage": null,
        "extractedText": "Samia Kousar © - 2nd + Follow\nHR Ops Coordinator @ Rubik's Technologies.\n\n« 1d+ Edited + @\n\nHiring on behalf of colleague:\n& Position : Senior Backend Engineer (Java)\n\nBB Job Duties:\n\n+ Work as part of an agile software team to improve digital\nsales platforms and contribute new ideas.\n\n+ Take responsibility for delivery processes, perform code\nreviews, refactoring, and deployments using CI/CD.\n\n+ Oversee the full software development lifecycle, providing\nsolutions for risks, code quality, and process optimization.\n* Collaborate with Product Owners and advise stakeholders\non technical topics.\n\n+ Contribute to IT security, DevOps, and service optimization\nand stabilization.\n\n+ Help define test strategies and implement IT security\nmeasures.\n\nBB Your Profile:\n\n+ 3+ years of experience in agile backend software\ndevelopment (Scrum/Kanban).\n\n+ Proficient in Java, Spring Boot, JPA (Hibernate), REST &\nOpenAPI, and AWS.\n\n+ Experienced with CI/CD, Git, GitLab Cl, Docker, Kubernetes,\nand PostgreSQL.\n\nFamiliar with architecture principles, microservices, and test\nautomation in microservice environments.\n\n+ Knowledge of implementing security measures such as\noverload protection and OWASP Top 10.\n\n+ Fluent in German and proficient in English, both spoken and\nwritten.\n\nBB What's Important to Us:\n\nWe provide equal opportunities to all candidates and promote\ndiversity and inclusion within our teams. We value every\napplication regardless of gender, nationality, ethnic or social\norigin, religion, belief, disability, age, or sexual orientation.\n",
        "originalFileName": "docs2.jpeg",
        "status": "completed"
      }
    ],
    "overallStatus": "processed"
  },
  "message": "OCR results retrieved successfully.",
  "status": "success",
  "statusCode": 200
}
```
### Example of Failed OCR Response
```json
{
    "data": {
        "collectionId": "b5b9aef6-2537-42ee-b9e5-f6b38432f2c2",
        "files": [
            {
                "createdAt": "2026-01-02T23:45:29.672077Z",
                "documentId": "0a6cc6ff-8218-4719-9cc7-174f7e7c5bdb",
                "errorMessage": "OCR provider is not available: Local Tesseract OCR",
                "extractedText": null,
                "originalFileName": "docs1.jpeg",
                "status": "failed"
            },
            {
                "createdAt": "2026-01-02T23:45:29.683818Z",
                "documentId": "a3eb06f7-a90d-4515-868e-ba0dc1e67fce",
                "errorMessage": "OCR provider is not available: Local Tesseract OCR",
                "extractedText": null,
                "originalFileName": "docs2.jpeg",
                "status": "failed"
            }
        ],
        "overallStatus": "failed_ocr"
    },
    "message": "OCR results retrieved successfully.",
    "status": "success",
    "statusCode": 200
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
    
  }
}
```

---

## Payments - PayPal

Base path: `/api/v1/paypal`

> [!IMPORTANT]
> **PayPal Account Required for Subscriptions**
> 
> - **Subscriptions (recurring payments):** Users **must have a PayPal account** because PayPal manages the recurring billing agreement.
> - **One-time payments:** Users can use PayPal Guest Checkout (pay with card without a PayPal account) in supported countries.
> 
> **Recommendation:** For users who prefer not to create a PayPal account:
> - Use **Stripe** for international card payments
> - Use **Paystack** for Nigerian/African users

### One-Time Payments

#### Create PayPal Order

| **Method** | **Endpoint**      | **Auth Required** |
|------------|-------------------|-------------------|
| `POST`     | `/paypal/orders`  | Yes               |

**Request Body:**

```json
{
  "amount": 29.99,
  "currency": "USD",
  "description": "One-time payment",
  "metadata": {},
  "intent": "CAPTURE"
}
```

**Response:**

```json
{
  "status": true,
  "message": "Order created successfully",
  "data": {
    "orderId": "ORDER-XXX",
    "status": "CREATED",
    "approvalUrl": "https://www.sandbox.paypal.com/checkoutnow?token=XXX",
    "links": ["..."]
  }
}
```

---

#### Capture PayPal Order

| **Method** | **Endpoint**                       | **Auth Required** |
|------------|------------------------------------|-------------------|
| `POST`     | `/paypal/orders/{orderId}/capture` | Yes               |

**Response:**

```json
{
  "status": true,
  "message": "Payment captured successfully",
  "data": {
    "captureId": "CAPTURE-XXX",
    "orderId": "ORDER-XXX",
    "status": "COMPLETED",
    "amount": 29.99,
    "currency": "USD"
  }
}
```

---

#### Get Order Details

| **Method** | **Endpoint**               | **Auth Required** |
|------------|----------------------------|-------------------|
| `GET`      | `/paypal/orders/{orderId}` | Yes               |

**Response:** PayPal order details with status and links.

---

#### Process Refund

| **Method** | **Endpoint**     | **Auth Required** |
|------------|------------------|-------------------|
| `POST`     | `/paypal/refund` | Yes               |

**Request Body:**

```json
{
  "captureId": "CAPTURE-XXX",
  "amount": 10.00,
  "currency": "USD",
  "reason": "Customer request"
}
```

**Response:**

```json
{
  "status": true,
  "message": "Refund processed successfully",
  "data": {
    "refundId": "REFUND-XXX",
    "captureId": "CAPTURE-XXX",
    "status": "COMPLETED",
    "amount": 10.00,
    "currency": "USD"
  }
}
```

---

#### Get Payment History

| **Method** | **Endpoint**               | **Auth Required** |
|------------|----------------------------|-------------------|
| `GET`      | `/paypal/payments/history` | Yes               |

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page`    | int  | 0       | Page number |
| `size`    | int  | 20      | Page size   |

**Response:** Paginated list of `PayPalPayment` objects.

---

### Subscriptions

#### Create Subscription

| **Method** | **Endpoint**            | **Auth Required** |
|------------|-------------------------|-------------------|
| `POST`     | `/paypal/subscriptions` | Yes               |

**Request Body:**

```json
{
  "planId": "P-XXXXXXXX",
  "returnUrl": "https://example.com/success",
  "cancelUrl": "https://example.com/cancel"
}
```

**Response:**

```json
{
  "status": true,
  "message": "Subscription created successfully",
  "data": {
    "subscriptionId": "I-XXXXXXXX",
    "status": "APPROVAL_PENDING",
    "approvalUrl": "https://www.sandbox.paypal.com/..."
  }
}
```

---

#### Get Subscription Details

| **Method** | **Endpoint**                             | **Auth Required** |
|------------|------------------------------------------|-------------------|
| `GET`      | `/paypal/subscriptions/{subscriptionId}` | Yes               |

**Response:** PayPal subscription details with billing info.

---

#### Get Active Subscription

| **Method** | **Endpoint**                   | **Auth Required** |
|------------|--------------------------------|-------------------|
| `GET`      | `/paypal/subscriptions/active` | Yes               |

**Response:** Active subscription for authenticated user or `404 Not Found`.

---

#### Cancel Subscription

| **Method** | **Endpoint**                                     | **Auth Required** |
|------------|--------------------------------------------------|-------------------|
| `POST`     | `/paypal/subscriptions/{subscriptionId}/cancel`  | Yes               |

**Query Parameters:**

| Parameter | Type   | Required | Description             |
|-----------|--------|----------|-------------------------|
| `reason`  | string | No       | Reason for cancellation |

**Response:**

```json
{
  "status": true,
  "message": "Subscription cancelled successfully",
  "data": {
    "subscriptionId": "I-XXXXXXXX",
    "status": "CANCELLED"
  }
}
```

---

#### Suspend Subscription

| **Method** | **Endpoint**                                     | **Auth Required** |
|------------|--------------------------------------------------|-------------------|
| `POST`     | `/paypal/subscriptions/{subscriptionId}/suspend` | Yes               |

**Response:** Subscription suspended successfully.

---

#### Activate Subscription

| **Method** | **Endpoint**                                      | **Auth Required** |
|------------|---------------------------------------------------|-------------------|
| `POST`     | `/paypal/subscriptions/{subscriptionId}/activate` | Yes               |

**Response:** Subscription activated successfully.

---

### Webhooks

| **Method** | **Endpoint**       | **Auth Required** |
|------------|--------------------|-------------------|
| `POST`     | `/paypal/webhook`  | No (PayPal IPN)   |

**Supported Events:**

- `PAYMENT.CAPTURE.COMPLETED` - Payment captured successfully
- `PAYMENT.CAPTURE.DENIED` - Payment capture failed
- `BILLING.SUBSCRIPTION.ACTIVATED` - Subscription activated
- `BILLING.SUBSCRIPTION.CANCELLED` - Subscription cancelled
- `BILLING.SUBSCRIPTION.SUSPENDED` - Subscription suspended
- `BILLING.SUBSCRIPTION.PAYMENT.FAILED` - Subscription payment failed

---

### PayPal Admin (SUPER_ADMIN Only)

Base path: `/api/v1/admin/paypal`

> These endpoints are for initial setup and plan management. They require SUPER_ADMIN role.

#### Set Up All PayPal Plans

| **Method** | **Endpoint**     | **Auth Required**      |
|------------|------------------|------------------------|
| `POST`     | `/plans/setup`   | Yes (SUPER_ADMIN role) |

Creates a PayPal Product and billing plans for all subscription tiers. Run once during initial setup.

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "PayPal plans setup completed successfully",
  "data": {
    "productId": "PROD-XXXXXXXXXXXXXXXX",
    "createdPlans": [
      {
        "planName": "STARTER_MONTHLY",
        "paypalPlanId": "P-XXXXXXXXXXXXXXXX",
        "price": "9.00",
        "interval": "MONTH"
      },
      {
        "planName": "PRO_MONTHLY",
        "paypalPlanId": "P-XXXXXXXXXXXXXXXX",
        "price": "19.00",
        "interval": "MONTH"
      },
      {
        "planName": "BUSINESS_MONTHLY",
        "paypalPlanId": "P-XXXXXXXXXXXXXXXX",
        "price": "49.00",
        "interval": "MONTH"
      }
    ],
    "planCount": 6,
    "errors": [],
    "success": true
  }
}
```

---

#### List Existing PayPal Plans

| **Method** | **Endpoint** | **Auth Required**      |
|------------|--------------|------------------------|
| `GET`      | `/plans`     | Yes (SUPER_ADMIN role) |

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "PayPal plans retrieved successfully",
  "data": {
    "plans": [
      {
        "id": "P-XXXXXXXXXXXXXXXX",
        "name": "Starter Monthly",
        "status": "ACTIVE",
        "productId": "PROD-XXXXXXXXXXXXXXXX"
      }
    ],
    "count": 6
  }
}
```

---

#### Deactivate a PayPal Plan

| **Method** | **Endpoint**                 | **Auth Required**      |
|------------|------------------------------|------------------------|
| `POST`     | `/plans/{planId}/deactivate` | Yes (SUPER_ADMIN role) |

Deactivates a PayPal billing plan. Existing subscriptions will continue until cancelled.

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "PayPal plan deactivated successfully",
  "data": {
    "planId": "P-XXXXXXXXXXXXXXXX"
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
  "results": [
    {
      "id": "4c88ccae-07ba-4862-9cc7-833c8c10a4b7",
      "collectionId": "82da1cd8-05f9-4101-9b12-f529bc1f5971",
      "fileName": "docs1.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 87367,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Siva @ @sivalabs - 5h 12)\nMy favorite @Tech Stack:\n\nLanguage: Java/Kotlin\n\nFrameworks: Spring Boot, Quarkus\n\nDesign & Architecture: Modular Monolith,\nPackage-by-Feature\n\nDatabase: PostgreSQL, Flyway M...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/a71de614-0e57-4911-9261-3af48dad9f2d-docs1.jpeg",
      "uploadTimestamp": "2026-01-03T06:58:11.403Z",
      "createdAt": "2026-01-03T06:58:11.403Z",
      "score": null
    },
    {
      "id": "192edc1d-291d-481b-9533-139f3aa87106",
      "collectionId": "ab3d5e4e-4dd2-44ec-a61a-279c7e1fbc9c",
      "fileName": "docs2.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 180086,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Samia Kousar © - 2nd + Follow\nHR Ops Coordinator @ Rubik's Technologies.\n\n« 1d+ Edited + @\n\nHiring on behalf of colleague:\n& Position : Senior Backend Engineer (Java)\n\nBB Job Duties:\n\n+ Work as part o...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/2988c6e2-6f88-4dcc-b3d6-aeb1b6bb26c5-docs2.jpeg",
      "uploadTimestamp": "2026-01-03T01:05:19.199Z",
      "createdAt": "2026-01-03T01:05:19.199Z",
      "score": null
    },
    {
      "id": "f757c584-7a1b-4433-9dc6-720d1a4b6cb9",
      "collectionId": "ab3d5e4e-4dd2-44ec-a61a-279c7e1fbc9c",
      "fileName": "docs1.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 87367,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Siva @ @sivalabs - 5h 12)\nMy favorite @Tech Stack:\n\nLanguage: Java/Kotlin\n\nFrameworks: Spring Boot, Quarkus\n\nDesign & Architecture: Modular Monolith,\nPackage-by-Feature\n\nDatabase: PostgreSQL, Flyway M...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/f584f3dd-2a04-4d46-9afb-c01dcc12c062-docs1.jpeg",
      "uploadTimestamp": "2026-01-03T01:05:19.199Z",
      "createdAt": "2026-01-03T01:05:18.615Z",
      "score": null
    },
    {
      "id": "c57d2a24-79ba-47f8-9145-45d84ffafb9d",
      "collectionId": "1d1c127c-47e1-4e59-bff7-3ea810d5ad89",
      "fileName": "docs2.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 180086,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Samia Kousar © - 2nd + Follow\nHR Ops Coordinator @ Rubik's Technologies.\n\n« 1d+ Edited + @\n\nHiring on behalf of colleague:\n& Position : Senior Backend Engineer (Java)\n\nBB Job Duties:\n\n+ Work as part o...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/a8b2d631-9e42-454a-b617-195ae61d0fc5-docs2.jpeg",
      "uploadTimestamp": "2026-01-03T00:01:22.52Z",
      "createdAt": "2026-01-03T00:01:22.52Z",
      "score": null
    },
    {
      "id": "427b799e-26e3-469f-bf0e-fddb8454722b",
      "collectionId": "1d1c127c-47e1-4e59-bff7-3ea810d5ad89",
      "fileName": "docs1.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 87367,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Siva @ @sivalabs - 5h 12)\nMy favorite @Tech Stack:\n\nLanguage: Java/Kotlin\n\nFrameworks: Spring Boot, Quarkus\n\nDesign & Architecture: Modular Monolith,\nPackage-by-Feature\n\nDatabase: PostgreSQL, Flyway M...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/727592e3-e884-4573-9845-226f11d1a4b7-docs1.jpeg",
      "uploadTimestamp": "2026-01-03T00:01:22.52Z",
      "createdAt": "2026-01-03T00:01:21.629Z",
      "score": null
    },
    {
      "id": "a3eb06f7-a90d-4515-868e-ba0dc1e67fce",
      "collectionId": "b5b9aef6-2537-42ee-b9e5-f6b38432f2c2",
      "fileName": "docs2.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 180086,
      "status": "FAILED_OCR",
      "ocrStatus": "FAILED",
      "textPreview": null,
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/33112830-09f6-41fb-89d0-a2e7c74c8329-docs2.jpeg",
      "uploadTimestamp": "2026-01-02T23:45:29.553Z",
      "createdAt": "2026-01-02T23:45:29.553Z",
      "score": null
    },
    {
      "id": "0a6cc6ff-8218-4719-9cc7-174f7e7c5bdb",
      "collectionId": "b5b9aef6-2537-42ee-b9e5-f6b38432f2c2",
      "fileName": "docs1.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 87367,
      "status": "FAILED_OCR",
      "ocrStatus": "FAILED",
      "textPreview": null,
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/d164ab9d-cfc7-4865-893e-e673119fbcb1-docs1.jpeg",
      "uploadTimestamp": "2026-01-02T23:45:29.553Z",
      "createdAt": "2026-01-02T23:45:28.98Z",
      "score": null
    },
    {
      "id": "1e50bf3b-ad75-4b1e-b888-5309567e9487",
      "collectionId": "77cde05e-7178-4930-8ed2-4a3888b8144c",
      "fileName": "docs2.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 180086,
      "status": "COMPLETED",
      "ocrStatus": "COMPLETED",
      "textPreview": "& Samia Kousar @ - 2nd + Follow\n\nHR Ops Coordinator @ Rubik's Technologies,\n“-\" 1d-Edited-@\n\nHiring on behalf of colleague:\n& Position : Senior Backend Engineer (Java)\nBB Job Duties:\n+ Work as part of...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/98f8c7e6-acaf-4cfd-a73b-9d5d6da095e8-docs2.jpeg",
      "uploadTimestamp": "2026-01-02T22:19:28.491Z",
      "createdAt": "2026-01-02T22:19:28.491Z",
      "score": null
    }
  ],
  "totalHits": 8,
  "page": 0,
  "size": 10,
  "totalPages": 1,
  "took": null,
  "highlights": {},
  "facets": {}
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

### Sample Request:

```GET /api/v1/search/documents/content?query=invoice&page=0&size=5```

**Response:**

```json
{
  "results": [
    {
      "id": "427b799e-26e3-469f-bf0e-fddb8454722b",
      "collectionId": "1d1c127c-47e1-4e59-bff7-3ea810d5ad89",
      "fileName": "docs1.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 87367,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Siva @ @sivalabs - 5h 12)\nMy favorite @Tech Stack:\n\nLanguage: Java/Kotlin\n\nFrameworks: Spring Boot, Quarkus\n\nDesign & Architecture: Modular Monolith,\nPackage-by-Feature\n\nDatabase: PostgreSQL, Flyway M...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/727592e3-e884-4573-9845-226f11d1a4b7-docs1.jpeg",
      "uploadTimestamp": "2026-01-03T00:01:22.52Z",
      "createdAt": "2026-01-03T00:01:21.629Z",
      "score": null
    },
    {
      "id": "f757c584-7a1b-4433-9dc6-720d1a4b6cb9",
      "collectionId": "ab3d5e4e-4dd2-44ec-a61a-279c7e1fbc9c",
      "fileName": "docs1.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 87367,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Siva @ @sivalabs - 5h 12)\nMy favorite @Tech Stack:\n\nLanguage: Java/Kotlin\n\nFrameworks: Spring Boot, Quarkus\n\nDesign & Architecture: Modular Monolith,\nPackage-by-Feature\n\nDatabase: PostgreSQL, Flyway M...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/f584f3dd-2a04-4d46-9afb-c01dcc12c062-docs1.jpeg",
      "uploadTimestamp": "2026-01-03T01:05:19.199Z",
      "createdAt": "2026-01-03T01:05:18.615Z",
      "score": null
    },
    {
      "id": "4c88ccae-07ba-4862-9cc7-833c8c10a4b7",
      "collectionId": "82da1cd8-05f9-4101-9b12-f529bc1f5971",
      "fileName": "docs1.jpeg",
      "fileType": "image/jpeg",
      "fileSize": 87367,
      "status": "PROCESSED",
      "ocrStatus": "COMPLETED",
      "textPreview": "Siva @ @sivalabs - 5h 12)\nMy favorite @Tech Stack:\n\nLanguage: Java/Kotlin\n\nFrameworks: Spring Boot, Quarkus\n\nDesign & Architecture: Modular Monolith,\nPackage-by-Feature\n\nDatabase: PostgreSQL, Flyway M...",
      "highlights": [],
      "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/a71de614-0e57-4911-9261-3af48dad9f2d-docs1.jpeg",
      "uploadTimestamp": "2026-01-03T06:58:11.403Z",
      "createdAt": "2026-01-03T06:58:11.403Z",
      "score": null
    }
  ],
  "totalHits": 3,
  "page": 0,
  "size": 5,
  "totalPages": 1,
  "took": null,
  "highlights": {},
  "facets": {}
}
```
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

**Response:**

```json
{
    "results": [
        {
            "id": "5ee41e38-56ca-4912-ae5b-b7427e966189",
            "firstName": "Michael",
            "lastName": "Wyte",
            "email": "afiaaniebiet0@gmail.com",
            "role": "USER",
            "isActive": true,
            "isVerified": true,
            "isPlatformAdmin": false,
            "isOrganizationAdmin": false,
            "country": "US",
            "profession": "Software engineer",
            "organization": "Globe Tech",
            "profilePicture": null,
            "subscriptionPlan": "FREE",
            "subscriptionStatus": "Active",
            "lastLogin": "2026-01-01T15:30:41.028Z",
            "createdAt": "2025-12-28T13:01:36.752Z",
            "updatedAt": "2026-01-01T15:30:41.033Z",
            "documentCount": 0
        },
        {
            "id": "98c0ba52-69e0-4bfe-bb76-793431334e47",
            "firstName": "Aniebiet",
            "lastName": "Afia",
            "email": "aniebietafia87@gmail.com",
            "role": "USER",
            "isActive": true,
            "isVerified": true,
            "isPlatformAdmin": false,
            "isOrganizationAdmin": false,
            "country": "NG",
            "profession": "Data Analyst",
            "organization": "Brints Tech",
            "profilePicture": null,
            "subscriptionPlan": "FREE",
            "subscriptionStatus": "Active",
            "lastLogin": "2025-12-27T11:53:05.965Z",
            "createdAt": "2025-12-27T11:33:58.862Z",
            "updatedAt": "2025-12-27T11:53:05.972Z",
            "documentCount": 0
        }
    ],
    "totalHits": 2,
    "page": 0,
    "size": 10,
    "totalPages": 1,
    "took": null,
    "highlights": {},
    "facets": {}
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

**Response:**

```json
{
  "results": [
    {
      "id": "550e6312-250c-450c-b71e-64db0dd25533",
      "userId": "5ee41e38-56ca-4912-ae5b-b7427e966189",
      "userEmail": "afiaaniebiet0@gmail.com",
      "userName": "Michael Wyte",
      "receiptNumber": "RCP-20260101-336640",
      "paymentProvider": "PAYSTACK",
      "externalPaymentId": "PAY_34213CB8D9FB4DEE",
      "paymentType": null,
      "status": "COMPLETED",
      "amount": 1395000.0,
      "currency": "NGN",
      "paymentMethod": "card",
      "paymentMethodDetails": "**** 4081 (VISA )",
      "description": null,
      "subscriptionPlan": null,
      "receiptUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/receipts/RCP-20260101-336640.pdf",
      "emailSent": true,
      "paidAt": "2026-01-01T15:31:38.993Z",
      "createdAt": "2026-01-01T15:31:41.743Z",
      "updatedAt": "2026-01-01T15:31:41.75Z"
    }
  ],
  "totalHits": 1,
  "page": 0,
  "size": 10,
  "totalPages": 1,
  "took": null,
  "highlights": {},
  "facets": {}
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

---

## Storage

Base path: `/api/v1/storage`

Storage allocation allows tracking document storage usage and limits based on subscription plans.

### Storage Limits by Plan

| Plan            | Storage Limit |
|-----------------|---------------|
| Free            | 120 MB        |
| Starter         | 2.6 GB        |
| Pro             | 12.3 GB       |
| Business        | 30 GB         |
| Team Premium    | 200 GB        |
| Team Enterprise | Unlimited     |

### Get Storage Info

| **Method** | **Endpoint**      | **Auth Required** |
|------------|-------------------|-------------------|
| `GET`      | `/storage`        | Yes               |

Returns current storage usage and limits for the authenticated user.

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Storage information retrieved successfully",
  "data": {
    "storageUsed": 1157179,
    "storageLimit": 32212254720,
    "storageUsedFormatted": "1.10 MB",
    "storageLimitFormatted": "30.00 GB",
    "percentageUsed": 0.0,
    "ocrPageLimit": 30000,
    "ocrPagesUsed": 0,
    "ocrPagesRemaining": 30000,
    "ocrUnlimited": false,
    "documentUploadLimit": 6000,
    "documentsUploaded": 7,
    "documentsRemaining": 5993,
    "documentsUnlimited": false,
    "subscriptionPlan": "Business_Yearly",
    "billingInterval": "Yearly",
    "documentQuotaExceeded": false,
    "ocrQuotaExceeded": false,
    "quotaExceeded": false,
    "remainingStorage": 32211097541,
    "unlimited": false
  }
}
```

**Response Fields:**

| Field                   | Type    | Description                                           |
|-------------------------|---------|-------------------------------------------------------|
| `storageUsed`           | Long    | Current storage used in bytes                         |
| `storageLimit`          | Long    | Maximum storage allowed in bytes (null for unlimited) |
| `storageUsedFormatted`  | String  | Human-readable storage used                           |
| `storageLimitFormatted` | String  | Human-readable storage limit                          |
| `percentageUsed`        | Double  | Percentage of storage used                            |
| `isUnlimited`           | Boolean | True if plan has unlimited storage                    |

---

### Run Storage Migration (Admin)

| **Method** | **Endpoint**             | **Auth Required**      |
|------------|--------------------------|------------------------|
| `POST`     | `/admin/storage/migrate` | Yes (SUPER_ADMIN role) |

Calculates and updates storage usage for all users based on existing documents. Run once after deploying the storage feature to backfill historical data.

**Response:**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Migration completed: 1 users updated, 0 teams updated, 783.55 KB total storage calculated",
  "data": {
    "usersUpdated": 1,
    "teamsUpdated": 0,
    "totalBytesCalculated": 802359,
    "summary": "Migration completed: 1 users updated, 0 teams updated, 783.55 KB total storage calculated"
  }
}
```

---

### Storage Quota Exceeded Error

When uploading documents that would exceed the storage limit:

```json
{
  "statusCode": 400,
  "status": "error",
  "message": "Storage quota exceeded. Required: 50.00 MB, Available: 20.00 MB, Limit: 120.00 MB. Please upgrade your plan or delete some files."
}
```

