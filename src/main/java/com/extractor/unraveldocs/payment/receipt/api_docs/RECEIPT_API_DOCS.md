# Receipt API Documentation

This document provides comprehensive documentation for the Receipt management endpoints in the UnravelDocs API.

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Endpoints](#endpoints)
4. [Data Models](#data-models)
5. [Error Handling](#error-handling)

---

## Overview

The Receipt API provides endpoints for users to view and download their payment receipts. Receipts are automatically generated when payments are processed through any supported payment provider.

**Supported Payment Providers:**
| Provider | Description |
|----------|-------------|
| STRIPE | Stripe payment gateway |
| PAYPAL | PayPal payment gateway |
| PAYSTACK | Paystack payment gateway |
| CHAPA | Chapa payment gateway |

---

## Authentication

All endpoints require Bearer token authentication.

```
Authorization: Bearer <access_token>
```

---

## Endpoints

Base URL: `/api/v1/receipts`

---

### Get User's Receipts

Retrieves a paginated list of receipts for the authenticated user.

**Endpoint:** `GET /api/v1/receipts`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | int | No | 0 | Page number (0-indexed) |
| size | int | No | 10 | Number of receipts per page |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Receipts retrieved successfully",
  "data": [
    {
      "id": "c5960b7d-58d4-4ab0-9db7-8015d929e54a",
      "receiptNumber": "RCP-20260116-108835",
      "paymentProvider": "PAYSTACK",
      "amount": 2945000.00,
      "currency": "NGN",
      "paymentMethod": "card",
      "paymentMethodDetails": "**** 4081 (VISA )",
      "description": null,
      "receiptUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/receipts/RCP-20260116-108835.pdf",
      "paidAt": "2026-01-16T21:47:40.859287Z",
      "createdAt": "2026-01-16T21:47:47.263663Z"
    }
  ]
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| id | string | Unique receipt identifier (UUID) |
| receiptNumber | string | Human-readable receipt number |
| paymentProvider | string | Payment provider: `STRIPE`, `PAYPAL`, `PAYSTACK`, `CHAPA` |
| amount | decimal | Payment amount |
| currency | string | 3-letter ISO currency code |
| paymentMethod | string | Payment method type (e.g., `card`, `bank`, `paypal`) |
| paymentMethodDetails | string | Additional details (e.g., card brand and last 4 digits) |
| description | string | Payment description |
| receiptUrl | string | URL to download receipt PDF |
| paidAt | datetime | When the payment was made (ISO 8601) |
| createdAt | datetime | When the receipt was created (ISO 8601) |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Receipts retrieved successfully |
| 401 | Unauthorized - Invalid or missing token |

---

### Get Receipt by Number

Retrieves a specific receipt by its receipt number.

**Endpoint:** `GET /api/v1/receipts/{receiptNumber}`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| receiptNumber | string | Yes | Unique receipt number (e.g., REC-2024-00001) |

**Response:**
```json
{
  "statusCode": 200,
  "status": true,
  "message": "Receipt retrieved successfully",
  "data": {
    "id": "uuid-abc123",
    "receiptNumber": "REC-2024-00001",
    "paymentProvider": "STRIPE",
    "amount": 49.99,
    "currency": "USD",
    "paymentMethod": "card",
    "paymentMethodDetails": "Visa ending in 4242",
    "description": "PRO Monthly Subscription",
    "receiptUrl": "https://storage.example.com/receipts/REC-2024-00001.pdf",
    "paidAt": "2024-01-01T12:00:00Z",
    "createdAt": "2024-01-01T12:00:05Z"
  },
  "timestamp": "2024-01-20T14:30:00Z"
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Receipt retrieved successfully |
| 401 | Unauthorized - Access denied to receipt |
| 404 | Receipt not found |

**Error Response (404):**
```json
{
  "statusCode": 404,
  "status": false,
  "message": "Receipt not found: REC-2024-00001",
  "timestamp": "2024-01-20T14:30:00Z"
}
```

**Error Response (401 - Access Denied):**
```json
{
  "statusCode": 401,
  "status": false,
  "message": "Access denied to receipt: REC-2024-00001",
  "timestamp": "2024-01-20T14:30:00Z"
}
```

---

### Download Receipt

Retrieves the download URL for a receipt PDF.

**Endpoint:** `GET /api/v1/receipts/{receiptNumber}/download`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| receiptNumber | string | Yes | Unique receipt number |

**Response:**
```json
{
  "statusCode": 200,
  "status": true,
  "message": "Receipt download URL retrieved",
  "data": "https://storage.example.com/receipts/REC-2024-00001.pdf",
  "timestamp": "2024-01-20T14:30:00Z"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| data | string | Direct URL to download the receipt PDF |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Download URL retrieved successfully |
| 401 | Unauthorized - Access denied to receipt |
| 404 | Receipt not found or PDF not available |

**Error Response (404 - PDF Not Available):**
```json
{
  "statusCode": 404,
  "status": false,
  "message": "Receipt PDF not available: REC-2024-00001",
  "timestamp": "2024-01-20T14:30:00Z"
}
```

---

## Data Models

### Receipt Entity

The Receipt entity stores payment receipt information in the database.

| Field                | Type            | Nullable | Description                   |
|----------------------|-----------------|----------|-------------------------------|
| id                   | string (UUID)   | No       | Primary key                   |
| user                 | User            | No       | Associated user (foreign key) |
| receiptNumber        | string          | No       | Unique receipt number         |
| paymentProvider      | PaymentProvider | No       | Payment gateway used          |
| externalPaymentId    | string          | No       | Payment ID from provider      |
| amount               | decimal(10,2)   | No       | Payment amount                |
| currency             | string(3)       | No       | ISO currency code             |
| paymentMethod        | string          | Yes      | Payment method type           |
| paymentMethodDetails | string          | Yes      | Additional payment details    |
| description          | text            | Yes      | Payment description           |
| receiptUrl           | text            | Yes      | URL to receipt PDF            |
| paidAt               | datetime        | Yes      | Payment timestamp             |
| emailSent            | boolean         | No       | Whether email was sent        |
| emailSentAt          | datetime        | Yes      | When email was sent           |
| createdAt            | datetime        | No       | Record creation time          |
| updatedAt            | datetime        | No       | Last update time              |

---

### PaymentProvider Enum

```java
public enum PaymentProvider {
    STRIPE,
    PAYSTACK,
    PAYPAL,
    CHAPA
}
```

---

### ReceiptResponseDto

Response DTO returned by the API.

```json
{
  "id": "string",
  "receiptNumber": "string",
  "paymentProvider": "STRIPE | PAYPAL | PAYSTACK | CHAPA",
  "amount": "decimal",
  "currency": "string",
  "paymentMethod": "string",
  "paymentMethodDetails": "string",
  "description": "string",
  "receiptUrl": "string",
  "paidAt": "datetime",
  "createdAt": "datetime"
}
```

---

## Error Handling

### Standard Error Response

All errors follow this format:

```json
{
  "statusCode": 400,
  "status": false,
  "message": "Error description",
  "timestamp": "2024-01-20T14:30:00Z"
}
```

### Error Codes

| HTTP Status | Exception             | Description                             |
|-------------|-----------------------|-----------------------------------------|
| 401         | UnauthorizedException | User not authenticated or access denied |
| 403         | ForbiddenException    | User lacks permission                   |
| 404         | NotFoundException     | Receipt not found                       |
| 500         | Internal Server Error | Server error                            |

---

## Receipt Generation

Receipts are automatically generated when:

1. **Stripe Payment Succeeds** - Triggered by `payment_intent.succeeded` webhook
2. **PayPal Payment Captured** - Triggered by `PAYMENT.CAPTURE.COMPLETED` webhook
3. **Paystack Transaction Succeeds** - Triggered by `charge.success` webhook
4. **Subscription Payment** - Triggered by recurring billing events

### Receipt Number Format

Receipt numbers follow the format: `REC-{YEAR}-{SEQUENCE}`

Example: `REC-2024-00001`, `REC-2024-00002`

---

## Email Notifications

When a receipt is generated, an email notification is sent to the user containing:
- Receipt number
- Payment details
- Amount and currency
- Download link for the PDF receipt

The system tracks email delivery status in the `emailSent` and `emailSentAt` fields.

---

*Documentation last updated: January 2024*
