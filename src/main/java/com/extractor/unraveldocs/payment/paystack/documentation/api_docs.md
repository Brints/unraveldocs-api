# Paystack Payment Gateway API Documentation

This document provides comprehensive documentation for all Paystack payment gateway endpoints in the UnravelDocs API.

---

## Table of Contents

1. [Transaction Endpoints](#transaction-endpoints)
2. [Subscription Endpoints](#subscription-endpoints)
3. [Callback Endpoints](#callback-endpoints)
4. [Webhook Endpoints](#webhook-endpoints)

---

## Authentication

All endpoints (except webhooks and callbacks) require Bearer token authentication.

```
Authorization: Bearer <access_token>
```

---

## Transaction Endpoints

Base URL: `/api/v1/paystack`

### Initialize Transaction

Initializes a payment transaction or subscription.

**Endpoint:** `POST /api/v1/paystack/transaction/initialize`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "email": "goldenlee87@gmail.com",
  "amount": 1395000,
  "currency": "NGN",
  "callback_url": "https://8hw23p5s-8080.uks1.devtunnels.ms/payment/callback",
  "coupon_code": "PROMO17",
  "planCode": "STARTER_MONTHLY",
  "metadata": {
    "plan_code": "STARTER_MONTHLY",
    "billingInterval": "monthly",
    "source": "billing_page"
  }
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | string | Yes | Customer's email address (valid email format) |
| amount | long | Yes | Amount in kobo (lowest currency unit). e.g., 500000 = ₦5,000 |
| callbackUrl | string | No | URL to redirect after payment |
| reference | string | No | Unique transaction reference (auto-generated if not provided) |
| currency | string | No | Currency code (default: NGN) |
| planCode | string | No | Paystack plan code for subscriptions |
| subscriptionStartDate | string | No | When subscription should start |
| channels | array | No | Payment channels to allow: `card`, `bank`, `ussd`, `qr`, `mobile_money`, `bank_transfer`, `eft` |
| metadata | object | No | Custom metadata (max 1MB) |
| subaccount | string | No | Subaccount code for split payments |
| splitCode | string | No | Split code for multi-split payments |
| bearer | string | No | Who bears transaction charges: `account` or `subaccount` |

**Response:**
```json
{
  "status": true,
  "message": "Transaction initialized successfully",
  "data": {
    "authorizationUrl": "https://checkout.paystack.com/xxx",
    "accessCode": "xxx",
    "reference": "TXN_123456"
  }
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| authorizationUrl | string | URL to redirect customer for payment |
| accessCode | string | Access code for Paystack inline checkout |
| reference | string | Transaction reference |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Transaction initialized successfully |
| 400 | Invalid request |
| 500 | Failed to initialize transaction |

---

### Verify Transaction

Verifies the status of a transaction.

**Endpoint:** `GET /api/v1/paystack/transaction/verify/{reference}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reference | string | Yes | Transaction reference |

**Response:**
```json
{
  "status": true,
  "message": "Transaction verified successfully",
  "data": {
    "id": 123456789,
    "domain": "test",
    "status": "success",
    "reference": "TXN_123456",
    "amount": 500000,
    "message": null,
    "gatewayResponse": "Successful",
    "paidAt": "2024-01-01T12:00:00.000Z",
    "createdAt": "2024-01-01T11:55:00.000Z",
    "channel": "card",
    "currency": "NGN",
    "ipAddress": "192.168.1.1",
    "metadata": {
      "orderId": "order_123"
    },
    "customer": {
      "id": 12345,
      "email": "user@example.com",
      "customerCode": "CUS_xxx",
      "firstName": "John",
      "lastName": "Doe",
      "phone": null
    },
    "authorization": {
      "authorizationCode": "AUTH_xxx",
      "bin": "408408",
      "last4": "4081",
      "expMonth": "12",
      "expYear": "2025",
      "channel": "card",
      "cardType": "visa",
      "bank": "Test Bank",
      "countryCode": "NG",
      "brand": "visa",
      "reusable": true,
      "signature": "SIG_xxx",
      "accountName": null
    },
    "plan": null,
    "fees": 7500,
    "feesSplit": null,
    "requestedAmount": 500000,
    "transactionDate": "2024-01-01T12:00:00.000Z"
  }
}
```

**Transaction Statuses:**
| Status | Description |
|--------|-------------|
| success | Transaction successful |
| failed | Transaction failed |
| abandoned | Customer abandoned transaction |
| pending | Transaction is pending |
| reversed | Transaction was reversed |
| queued | Transaction is queued |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Transaction verified successfully |
| 404 | Transaction not found |

---

### Charge Authorization

Charges a previously saved authorization for recurring payments.

**Endpoint:** `POST /api/v1/paystack/transaction/charge-authorization`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| authorizationCode | string | Yes | Saved authorization code |
| amount | long | Yes | Amount in kobo |
| currency | string | No | Currency code (default: NGN) |

**Response:**
```json
{
  "status": true,
  "message": "Authorization charged successfully",
  "data": {
    "id": 123456789,
    "status": "success",
    "reference": "TXN_auto_123",
    "amount": 500000,
    ...
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Authorization charged successfully |
| 400 | Invalid authorization or amount |
| 500 | Charge failed |

---

### Get Payment History

Retrieves paginated payment history for the authenticated user.

**Endpoint:** `GET /api/v1/paystack/transaction/history`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | int | No | 0 | Page number (0-indexed) |
| size | int | No | 20 | Page size |
| sort | string | No | createdAt,desc | Sort field and direction |

**Response:**
```json
{
  "content": [
    {
      "id": "4eef4136-7b55-4ed4-9639-b16a79d9cbcb",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5887470161,
      "reference": "PAY_FEAD9DA26804495F",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "CREDIT_PURCHASE",
      "status": "SUCCEEDED",
      "amount": 7750.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 216.25,
      "channel": "bank_transfer",
      "gateway_response": "Approved",
      "description": null,
      "failure_message": null,
      "paid_at": null,
      "created_at": "2026-02-28T17:18:41.783946Z"
    },
    {
      "id": "9eb6ab37-7f39-4a9d-8cf2-6375031adb50",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5887477954,
      "reference": "PAY_A07F94D615DE495C",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "CREDIT_PURCHASE",
      "status": "SUCCEEDED",
      "amount": 7750.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 216.25,
      "channel": "bank",
      "gateway_response": "Approved",
      "description": null,
      "failure_message": null,
      "paid_at": "2026-02-28T17:22:05Z",
      "created_at": "2026-02-28T17:21:20.115705Z"
    },
    {
      "id": "9e48f1c9-971c-471b-a661-eed61964537a",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5806118962,
      "reference": "PAY_65FC174401F84163",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "ONE_TIME",
      "status": "SUCCEEDED",
      "amount": 1395000.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 309.25,
      "channel": "card",
      "gateway_response": "Successful",
      "description": null,
      "failure_message": null,
      "paid_at": "2026-02-04T11:41:11Z",
      "created_at": "2026-02-04T11:40:54.192271Z"
    },
    {
      "id": "4eca6ce3-93cc-4cb5-bad9-4879b2e50c8a",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5806020654,
      "reference": "PAY_339D28D506024740",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "ONE_TIME",
      "status": "SUCCEEDED",
      "amount": 1395000.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 309.25,
      "channel": "card",
      "gateway_response": "Successful",
      "description": null,
      "failure_message": null,
      "paid_at": "2026-02-04T11:07:06Z",
      "created_at": "2026-02-04T11:06:54.840672Z"
    },
    {
      "id": "1589bb15-ccf7-46d2-9e4d-472ddc7868b6",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5806030818,
      "reference": "PAY_171959BD4ED84AFC",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "ONE_TIME",
      "status": "SUCCEEDED",
      "amount": 1395000.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 309.25,
      "channel": "card",
      "gateway_response": "Successful",
      "description": null,
      "failure_message": null,
      "paid_at": "2026-02-04T11:10:10Z",
      "created_at": "2026-02-04T11:10:02.915075Z"
    },
    {
      "id": "28ce5d8d-de7c-4355-a41b-b654643485e5",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": null,
      "reference": "PAY_1811FC2891724984",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "ONE_TIME",
      "status": "PENDING",
      "amount": 13950.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": null,
      "channel": null,
      "gateway_response": null,
      "description": null,
      "failure_message": null,
      "paid_at": null,
      "created_at": "2026-02-04T11:29:40.659217Z"
    },
    {
      "id": "c210f540-8746-4dcb-a047-ed0d8cc9131f",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5806134792,
      "reference": "PAY_3103135BB491471F",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "ONE_TIME",
      "status": "SUCCEEDED",
      "amount": 1395000.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 309.25,
      "channel": "card",
      "gateway_response": "Successful",
      "description": null,
      "failure_message": null,
      "paid_at": "2026-02-04T11:45:58Z",
      "created_at": "2026-02-04T11:45:51.795486Z"
    },
    {
      "id": "3767c75a-84d4-4213-b912-efa9f4657f51",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": null,
      "reference": "PAY_F8C799EF5A414402",
      "plan_code": "STARTER_MONTHLY",
      "subscription_code": null,
      "payment_type": "SUBSCRIPTION",
      "status": "PENDING",
      "amount": 1395000.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": null,
      "channel": null,
      "gateway_response": null,
      "description": null,
      "failure_message": null,
      "paid_at": null,
      "created_at": "2026-02-04T12:41:25.466361Z"
    },
    {
      "id": "ad3ba095-abbb-4f36-8041-3526a2b23967",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5806307806,
      "reference": "PAY_4000836E2A6D4635",
      "plan_code": "STARTER_MONTHLY",
      "subscription_code": null,
      "payment_type": "SUBSCRIPTION",
      "status": "SUCCEEDED",
      "amount": 1395000.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 309.25,
      "channel": "card",
      "gateway_response": "Successful",
      "description": null,
      "failure_message": null,
      "paid_at": "2026-02-04T12:42:39Z",
      "created_at": "2026-02-04T12:42:24.155731Z"
    },
    {
      "id": "b241278c-f89d-49dd-9a1d-07def6e444db",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5887424683,
      "reference": "PAY_AB5B9462F36E4207",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "CREDIT_PURCHASE",
      "status": "SUCCEEDED",
      "amount": 7750.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 216.25,
      "channel": "card",
      "gateway_response": "Successful",
      "description": null,
      "failure_message": null,
      "paid_at": "2026-02-28T17:03:36Z",
      "created_at": "2026-02-28T17:03:22.128727Z"
    },
    {
      "id": "b98a2d15-1a06-4d3a-ae92-ae581b5168ab",
      "userId": "3e3c6fc7-e48b-4682-ab54-0e9375a039b8",
      "userEmail": "afiaaniebiet0@gmail.com",
      "transaction_id": 5887463975,
      "reference": "PAY_BD7460CD6ED04F93",
      "plan_code": null,
      "subscription_code": null,
      "payment_type": "CREDIT_PURCHASE",
      "status": "SUCCEEDED",
      "amount": 7750.00,
      "currency": "NGN",
      "amount_refunded": null,
      "fees": 216.25,
      "channel": "bank_transfer",
      "gateway_response": "Approved",
      "description": null,
      "failure_message": null,
      "paid_at": null,
      "created_at": "2026-02-28T17:16:29.861235Z"
    }
  ],
  "empty": false,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 11,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "unpaged": false
  },
  "size": 20,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "totalElements": 11,
  "totalPages": 1
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Payment history retrieved successfully |

---

### Get Payment by Reference

Retrieves a specific payment by its reference.

**Endpoint:** `GET /api/v1/paystack/transaction/{reference}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reference | string | Yes | Transaction reference |

**Response:** PaystackPayment entity

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Payment retrieved successfully |
| 404 | Payment not found |

---

## Subscription Endpoints

Base URL: `/api/v1/paystack`

### Create Subscription

Creates a new Paystack subscription.

**Endpoint:** `POST /api/v1/paystack/subscription`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "customer": "CUS_xxx",
  "planName": "PRO_MONTHLY",
  "authorization": "AUTH_xxx",
  "startDate": "2024-02-01T00:00:00Z"
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customer | string | Yes | Customer email or code |
| planName | string | Yes | Subscription plan name |
| authorization | string | No | Authorization code for charging |
| startDate | string | No | Subscription start date (ISO 8601) |

**Response:**
```json
{
  "status": true,
  "message": "Subscription created successfully",
  "data": {
    "id": "uuid",
    "subscriptionCode": "SUB_xxx",
    "status": "active",
    "emailToken": "xxx",
    "amount": 500000,
    "nextPaymentDate": "2024-02-01T00:00:00Z",
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

**Subscription Statuses:**
| Status | Description |
|--------|-------------|
| active | Subscription is active |
| non-renewing | Will not renew |
| attention | Requires attention |
| completed | Subscription completed |
| cancelled | Subscription cancelled |

**Status Codes:**
| Code | Description |
|------|-------------|
| 201 | Subscription created successfully |
| 400 | Invalid request |
| 500 | Failed to create subscription |

---

### Get Subscription by Code

Retrieves a subscription by its code.

**Endpoint:** `GET /api/v1/paystack/subscription/{subscriptionCode}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionCode | string | Yes | Paystack subscription code |

**Response:** PaystackSubscription entity

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription retrieved successfully |
| 404 | Subscription not found |

---

### Get Active Subscription

Retrieves the active subscription for the authenticated user.

**Endpoint:** `GET /api/v1/paystack/subscription/active`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Response:** PaystackSubscription entity

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Active subscription found |
| 404 | No active subscription |

---

### Get Subscription History

Retrieves paginated subscription history.

**Endpoint:** `GET /api/v1/paystack/subscriptions`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | int | No | 0 | Page number |
| size | int | No | 20 | Page size |

**Response:** Paginated PaystackSubscription entities

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription history retrieved successfully |

---

### Enable Subscription

Enables a previously disabled subscription.

**Endpoint:** `POST /api/v1/paystack/subscription/{subscriptionCode}/enable`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionCode | string | Yes | Paystack subscription code |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| emailToken | string | Yes | Email token for subscription management |

**Response:**
```json
{
  "status": true,
  "message": "Subscription enabled successfully",
  "data": {
    "subscriptionCode": "SUB_xxx",
    "status": "active",
    ...
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription enabled successfully |
| 400 | Invalid email token |
| 404 | Subscription not found |

---

### Disable Subscription

Disables (cancels) a subscription.

**Endpoint:** `POST /api/v1/paystack/subscription/{subscriptionCode}/disable`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionCode | string | Yes | Paystack subscription code |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| emailToken | string | Yes | Email token for subscription management |

**Response:**
```json
{
  "status": true,
  "message": "Subscription disabled successfully",
  "data": {
    "subscriptionCode": "SUB_xxx",
    "status": "cancelled",
    ...
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription disabled successfully |
| 400 | Invalid email token |
| 404 | Subscription not found |

---

## Callback Endpoints

Base URL: `/api/v1/paystack`

### Payment Callback

Callback URL for Paystack payment redirect.

**Endpoint:** `GET /api/v1/paystack/callback`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reference | string | Yes | Transaction reference |
| trxref | string | No | Alternative reference (fallback) |

**Response:**
```json
{
  "status": true,
  "message": "Payment success",
  "data": {
    "id": 123456789,
    "status": "success",
    "reference": "TXN_123456",
    "amount": 500000,
    ...
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Callback processed successfully |

---

## Webhook Endpoints

Base URL: `/api/v1/paystack/webhook`

### Handle Paystack Webhook

Receives and processes Paystack webhook events.

**Endpoint:** `POST /api/v1/paystack/webhook`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| x-paystack-signature | string | No | HMAC SHA512 signature for verification |

**Request Body:**
```json
{
  "event": "charge.success",
  "data": {
    "id": 123456789,
    "domain": "test",
    "status": "success",
    "reference": "TXN_123456",
    "amount": 500000,
    "gateway_response": "Successful",
    "paid_at": "2024-01-01T12:00:00.000Z",
    "created_at": "2024-01-01T11:55:00.000Z",
    "channel": "card",
    "currency": "NGN",
    "customer": {
      "id": 12345,
      "email": "user@example.com",
      "customer_code": "CUS_xxx"
    },
    "authorization": {
      "authorization_code": "AUTH_xxx",
      "card_type": "visa",
      "last4": "4081",
      "exp_month": "12",
      "exp_year": "2025",
      "reusable": true
    },
    "plan": null,
    "metadata": {}
  }
}
```

**Supported Event Types:**
| Event Type | Description |
|------------|-------------|
| `charge.success` | Successful payment |
| `charge.failed` | Failed payment |
| `subscription.create` | Subscription created |
| `subscription.enable` | Subscription enabled |
| `subscription.disable` | Subscription disabled |
| `subscription.not_renew` | Subscription set to not renew |
| `invoice.create` | Invoice created |
| `invoice.payment_failed` | Invoice payment failed |
| `invoice.update` | Invoice updated |
| `transfer.success` | Transfer successful |
| `transfer.failed` | Transfer failed |
| `transfer.reversed` | Transfer reversed |
| `refund.pending` | Refund is pending |
| `refund.processed` | Refund processed |
| `refund.failed` | Refund failed |

**Response:**
| Body | Description |
|------|-------------|
| `Webhook processed successfully` | Event processed |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Webhook processed successfully |
| 400 | Invalid signature or payload |
| 500 | Processing error |

---

### Webhook Health Check

Checks if the webhook endpoint is healthy.

**Endpoint:** `GET /api/v1/paystack/webhook/health`

**Response:**
```
Webhook endpoint is healthy
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Endpoint is healthy |

---

## Error Responses

All endpoints may return the following error format:

```json
{
  "status": false,
  "message": "Error description"
}
```

Or for webhook errors:

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid webhook signature",
  "path": "/api/v1/paystack/webhook"
}
```

---

## Currency and Amount Notes

**Amount Representation:**
- All amounts are in the **lowest currency unit** (kobo for NGN, cents for USD)
- Example: ₦5,000 = 500000 kobo
- Example: $50.00 = 5000 cents

**Supported Currencies:**
| Currency | Code | Country |
|----------|------|---------|
| Nigerian Naira | NGN | Nigeria |
| Ghanaian Cedi | GHS | Ghana |
| South African Rand | ZAR | South Africa |
| Kenyan Shilling | KES | Kenya |
| US Dollar | USD | International |

---

## Payment Channels

| Channel       | Description               |
|---------------|---------------------------|
| card          | Debit/Credit card         |
| bank          | Bank account              |
| ussd          | USSD payment              |
| qr            | QR code payment           |
| mobile_money  | Mobile money              |
| bank_transfer | Bank transfer             |
| eft           | Electronic funds transfer |

---

## Configuration

The following environment variables must be configured:

```properties
paystack.secret-key=sk_test_xxx
paystack.public-key=pk_test_xxx
paystack.callback-url=https://yourapp.com/paystack/callback
paystack.webhook-secret=whsec_xxx
```

---

## Webhook Signature Verification

Paystack signs all webhook payloads with HMAC SHA512 using your secret key.

**Verification Process:**
1. Get the `x-paystack-signature` header
2. Hash the request body using HMAC SHA512 with your secret key
3. Compare the computed hash with the signature header

**Example (Java):**
```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public boolean verifySignature(String payload, String signature, String secretKey) {
    try {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(secretKeySpec);
        byte[] hash = sha512Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String computedSignature = bytesToHex(hash);
        return computedSignature.equals(signature);
    } catch (Exception e) {
        return false;
    }
}
```

---

## Testing

Use Paystack test mode with test API keys:

**Test Cards:**
| Card Number | Description |
|-------------|-------------|
| 4084084084084081 | Successful transaction |
| 4084080000005408 | Declined transaction |
| 5060666666666666666 | Verve successful |
| 507850785078507812 | Verve declined |

**Test Bank Account:**
- Bank: Test Bank
- Account Number: 0000000000

**Test Authorization Code:**
Use any previous successful test transaction's authorization code.

---

## Rate Limits

Paystack implements rate limiting on their API:
- **Live mode:** 10 requests per second
- **Test mode:** More lenient limits

The application does not add additional rate limiting.

---

*Documentation last updated: January 2024*
