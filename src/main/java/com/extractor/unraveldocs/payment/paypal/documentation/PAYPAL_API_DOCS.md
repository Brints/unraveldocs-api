# PayPal Payment Gateway API Documentation

This document provides comprehensive documentation for all PayPal payment gateway endpoints in the UnravelDocs API.

---

## Table of Contents

1. [Order Endpoints](#order-endpoints)
2. [Subscription Endpoints](#subscription-endpoints)
3. [Callback Endpoints](#callback-endpoints)
4. [Admin Endpoints](#admin-endpoints)
5. [Webhook Endpoints](#webhook-endpoints)

---

## Authentication

All endpoints (except webhooks and callbacks) require Bearer token authentication.

```
Authorization: Bearer <access_token>
```

Admin endpoints additionally require `SUPER_ADMIN` role.

---

## Order Endpoints

Base URL: `/api/v1/paypal`

### Create Order

Creates a new PayPal order for one-time payment.

**Endpoint:** `POST /api/v1/paypal/orders`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "amount": 49.99,
  "currency": "USD",
  "description": "Document processing service",
  "returnUrl": "https://yourapp.com/paypal/return",
  "cancelUrl": "https://yourapp.com/paypal/cancel",
  "metadata": {
    "orderId": "order_123"
  },
  "planId": null,
  "intent": "CAPTURE"
}
```

**Request Fields:**
| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| amount | decimal | Yes | - | Payment amount (minimum 0.01) |
| currency | string | Yes | - | 3-letter ISO currency code |
| description | string | No | null | Payment description |
| returnUrl | string | No | null | URL to redirect after approval |
| cancelUrl | string | No | null | URL to redirect on cancellation |
| metadata | object | No | null | Custom key-value metadata |
| planId | string | No | null | Subscription plan ID |
| intent | string | No | "CAPTURE" | `CAPTURE` or `AUTHORIZE` |

**Response:**
```json
{
  "status": true,
  "message": "Order created successfully",
  "data": {
    "orderId": "8MC585209K746392H",
    "status": "CREATED",
    "approvalUrl": "https://www.sandbox.paypal.com/checkoutnow?token=8MC585209K746392H",
    "links": [
      {
        "href": "https://api.sandbox.paypal.com/v2/checkout/orders/8MC585209K746392H",
        "rel": "self",
        "method": "GET"
      },
      {
        "href": "https://www.sandbox.paypal.com/checkoutnow?token=8MC585209K746392H",
        "rel": "approve",
        "method": "GET"
      },
      {
        "href": "https://api.sandbox.paypal.com/v2/checkout/orders/8MC585209K746392H/capture",
        "rel": "capture",
        "method": "POST"
      }
    ]
  }
}
```

**Order Statuses:**
| Status | Description |
|--------|-------------|
| CREATED | Order created, awaiting approval |
| SAVED | Order saved for later |
| APPROVED | Payer approved the order |
| VOIDED | Order voided before capture |
| COMPLETED | Payment captured successfully |
| PAYER_ACTION_REQUIRED | Payer needs to take action |

**Status Codes:**
| Code | Description |
|------|-------------|
| 201 | Order created successfully |
| 400 | Invalid request |
| 500 | Failed to create order |

---

### Capture Order

Captures payment for an approved order.

**Endpoint:** `POST /api/v1/paypal/orders/{orderId}/capture`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orderId | string | Yes | PayPal order ID |

**Response:**
```json
{
  "status": true,
  "message": "Payment captured successfully",
  "data": {
    "captureId": "29N36517RC5765138",
    "orderId": "8MC585209K746392H",
    "status": "COMPLETED",
    "amount": "49.99",
    "currency": "USD"
  }
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| captureId | string | PayPal capture ID |
| orderId | string | Original order ID |
| status | string | Capture status |
| amount | string | Captured amount |
| currency | string | Currency code |

**Capture Statuses:**
| Status | Description |
|--------|-------------|
| COMPLETED | Payment captured successfully |
| PENDING | Capture is pending |
| DECLINED | Capture was declined |
| REFUNDED | Capture was refunded |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Order captured successfully |
| 400 | Order not approved |
| 404 | Order not found |

---

### Get Order Details

Retrieves PayPal order details.

**Endpoint:** `GET /api/v1/paypal/orders/{orderId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orderId | string | Yes | PayPal order ID |

**Response:**
```json
{
  "status": true,
  "message": "Order retrieved successfully",
  "data": {
    "id": "8MC585209K746392H",
    "status": "COMPLETED",
    "amount": 49.99,
    "currency": "USD",
    "approvalUrl": null,
    "captureUrl": null,
    "links": [...],
    "createTime": "2024-01-01T12:00:00Z",
    "updateTime": "2024-01-01T12:05:00Z",
    "payer": {
      "payerId": "PAYERID123",
      "email": "payer@example.com",
      "firstName": "John",
      "lastName": "Doe"
    },
    "purchaseUnits": [
      {
        "referenceId": "default",
        "amount": {
          "currencyCode": "USD",
          "value": "49.99"
        },
        "description": "Document processing service",
        "customId": null,
        "captures": [
          {
            "id": "29N36517RC5765138",
            "status": "COMPLETED",
            "amount": {
              "currencyCode": "USD",
              "value": "49.99"
            },
            "createTime": "2024-01-01T12:05:00Z"
          }
        ]
      }
    ]
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Order retrieved successfully |
| 404 | Order not found |

---

### Refund Payment

Processes a refund for a captured payment.

**Endpoint:** `POST /api/v1/paypal/refund`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "captureId": "29N36517RC5765138",
  "amount": 25.00,
  "currency": "USD",
  "reason": "Customer request",
  "invoiceId": "INV-001",
  "noteToPayer": "Refund for partial service"
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| captureId | string | Yes | PayPal capture ID to refund |
| amount | decimal | No | Amount to refund (null = full refund) |
| currency | string | No | Currency code (if partial refund) |
| reason | string | No | Refund reason |
| invoiceId | string | No | Invoice reference |
| noteToPayer | string | No | Note to include with refund |

**Response:**
```json
{
  "status": true,
  "message": "Refund processed successfully",
  "data": {
    "refundId": "3PY63685WR921193R",
    "captureId": "29N36517RC5765138",
    "status": "COMPLETED",
    "amount": "25.00",
    "currency": "USD"
  }
}
```

**Refund Statuses:**
| Status | Description |
|--------|-------------|
| COMPLETED | Refund completed |
| PENDING | Refund is pending |
| CANCELLED | Refund was cancelled |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Refund processed successfully |
| 400 | Invalid refund request |
| 404 | Capture not found |

---

### Get Payment History

Retrieves paginated payment history for the authenticated user.

**Endpoint:** `GET /api/v1/paypal/payments/history`

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
      "id": "uuid",
      "orderId": "8MC585209K746392H",
      "userId": "user_uuid",
      "amount": 49.99,
      "currency": "USD",
      "status": "COMPLETED",
      "createdAt": "2024-01-01T12:00:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Payment history retrieved successfully |

---

### Get Payment by Order ID

Retrieves a specific payment by its order ID.

**Endpoint:** `GET /api/v1/paypal/payments/{orderId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| orderId | string | Yes | PayPal order ID |

**Response:** PayPal payment entity

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Payment retrieved successfully |
| 404 | Payment not found |

---

## Subscription Endpoints

Base URL: `/api/v1/paypal`

### Create Subscription

Creates a new PayPal subscription.

**Endpoint:** `POST /api/v1/paypal/subscriptions`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "planId": "P-BIL34567890",
  "returnUrl": "https://yourapp.com/paypal/return",
  "cancelUrl": "https://yourapp.com/paypal/cancel",
  "customId": "user_123",
  "startTime": "2024-02-01T00:00:00Z",
  "quantity": 1,
  "autoRenewal": true
}
```

**Request Fields:**
| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| planId | string | Yes | - | PayPal billing plan ID |
| returnUrl | string | No | null | URL to redirect after approval |
| cancelUrl | string | No | null | URL to redirect on cancellation |
| customId | string | No | null | Custom tracking ID |
| startTime | string | No | null | Start time (ISO 8601) |
| quantity | int | No | 1 | Subscription quantity |
| autoRenewal | boolean | No | true | Auto-renew subscription |

**Response:**
```json
{
  "status": true,
  "message": "Subscription created successfully",
  "data": {
    "subscriptionId": "I-BW452GLLEP1G",
    "status": "APPROVAL_PENDING",
    "approvalUrl": "https://www.sandbox.paypal.com/webapps/billing/subscriptions?ba_token=BA-XXX"
  }
}
```

**Subscription Statuses:**
| Status | Description |
|--------|-------------|
| APPROVAL_PENDING | Awaiting payer approval |
| APPROVED | Subscriber approved the subscription |
| ACTIVE | Subscription is active |
| SUSPENDED | Subscription is suspended |
| CANCELLED | Subscription cancelled |
| EXPIRED | Subscription expired |

**Status Codes:**
| Code | Description |
|------|-------------|
| 201 | Subscription created successfully |
| 400 | Invalid request |
| 500 | Failed to create subscription |

---

### Get Subscription Details

Retrieves PayPal subscription details.

**Endpoint:** `GET /api/v1/paypal/subscriptions/{subscriptionId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | PayPal subscription ID |

**Response:**
```json
{
  "status": true,
  "message": "Subscription retrieved successfully",
  "data": {
    "id": "I-BW452GLLEP1G",
    "planId": "P-BIL34567890",
    "status": "ACTIVE",
    "startTime": "2024-01-01T00:00:00Z",
    "createTime": "2024-01-01T12:00:00Z",
    "updateTime": "2024-01-01T12:05:00Z",
    "approvalUrl": null,
    "customId": "user_123",
    "billingInfo": {
      "outstandingBalance": 0.00,
      "currency": "USD",
      "cycleExecutionsCount": 3,
      "failedPaymentsCount": 0,
      "lastPaymentTime": "2024-03-01T00:00:00Z",
      "lastPaymentAmount": 19.99,
      "nextBillingTime": "2024-04-01T00:00:00Z"
    },
    "subscriber": {
      "payerId": "PAYERID123",
      "email": "subscriber@example.com",
      "firstName": "John",
      "lastName": "Doe"
    },
    "links": [...]
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription retrieved successfully |
| 404 | Subscription not found |

---

### Get Active Subscription

Retrieves the active subscription for the authenticated user.

**Endpoint:** `GET /api/v1/paypal/subscriptions/active`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Response:** PayPal subscription entity

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Active subscription found |
| 404 | No active subscription |

---

### Get Subscription History

Retrieves paginated subscription history.

**Endpoint:** `GET /api/v1/paypal/subscriptions`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | int | No | 0 | Page number |
| size | int | No | 20 | Page size |

**Response:** Paginated PayPal subscription entities

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription history retrieved successfully |

---

### Cancel Subscription

Cancels a PayPal subscription.

**Endpoint:** `POST /api/v1/paypal/subscriptions/{subscriptionId}/cancel`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | PayPal subscription ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reason | string | No | Cancellation reason |

**Response:**
```json
{
  "status": true,
  "message": "Subscription cancelled successfully",
  "data": {
    "subscriptionId": "I-BW452GLLEP1G",
    "status": "CANCELLED"
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription cancelled successfully |
| 404 | Subscription not found |

---

### Suspend Subscription

Suspends a PayPal subscription.

**Endpoint:** `POST /api/v1/paypal/subscriptions/{subscriptionId}/suspend`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | PayPal subscription ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reason | string | No | Suspension reason |

**Response:**
```json
{
  "status": true,
  "message": "Subscription suspended successfully",
  "data": {
    "subscriptionId": "I-BW452GLLEP1G",
    "status": "SUSPENDED"
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription suspended successfully |
| 404 | Subscription not found |

---

### Activate Subscription

Activates/resumes a suspended subscription.

**Endpoint:** `POST /api/v1/paypal/subscriptions/{subscriptionId}/activate`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | PayPal subscription ID |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| reason | string | No | Activation reason |

**Response:**
```json
{
  "status": true,
  "message": "Subscription activated successfully",
  "data": {
    "subscriptionId": "I-BW452GLLEP1G",
    "status": "ACTIVE"
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription activated successfully |
| 404 | Subscription not found |

---

## Callback Endpoints

Base URL: `/api/v1/paypal`

### Payment Return Callback

Handles return after PayPal approval.

**Endpoint:** `GET /api/v1/paypal/return`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| token | string | No | PayPal order/subscription token |
| PayerID | string | No | Payer ID from PayPal |

**Response:**
```json
{
  "status": true,
  "message": "Return callback received",
  "data": {
    "token": "8MC585209K746392H",
    "payerId": "PAYERID123"
  }
}
```

---

### Payment Cancel Callback

Handles cancellation from PayPal.

**Endpoint:** `GET /api/v1/paypal/cancel`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| token | string | No | PayPal order/subscription token |

**Response:**
```json
{
  "status": false,
  "message": "Payment cancelled by user",
  "data": {
    "token": "8MC585209K746392H"
  }
}
```

---

## Admin Endpoints

Base URL: `/api/v1/admin/paypal`

**Required Role:** `SUPER_ADMIN`

### Set Up PayPal Billing Plans

Creates PayPal product and billing plans for all subscription tiers.

**Endpoint:** `POST /api/v1/admin/paypal/plans/setup`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token (SUPER_ADMIN) |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "PayPal plans setup completed successfully",
  "data": {
    "success": true,
    "productId": "PROD-XXX",
    "plans": [
      {
        "tier": "STARTER_MONTHLY",
        "planId": "P-XXX"
      },
      {
        "tier": "PRO_MONTHLY",
        "planId": "P-YYY"
      }
    ]
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Plans setup completed successfully |
| 207 | Partial success (some plans failed) |
| 403 | Forbidden (not SUPER_ADMIN) |

---

### List PayPal Billing Plans

Lists all existing PayPal billing plans.

**Endpoint:** `GET /api/v1/admin/paypal/plans`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token (SUPER_ADMIN) |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "PayPal plans retrieved successfully",
  "data": {
    "plans": [
      {
        "id": "P-XXX",
        "name": "STARTER_MONTHLY",
        "status": "ACTIVE",
        "productId": "PROD-XXX"
      }
    ],
    "count": 1
  }
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Plans retrieved successfully |
| 403 | Forbidden (not SUPER_ADMIN) |

---

### Deactivate PayPal Billing Plan

Deactivates a PayPal billing plan.

**Endpoint:** `POST /api/v1/admin/paypal/plans/{planId}/deactivate`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| planId | string | Yes | PayPal plan ID |

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token (SUPER_ADMIN) |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "PayPal plan deactivated successfully",
  "data": {
    "planId": "P-XXX"
  }
}
```

**Note:** Existing subscriptions will continue until cancelled.

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Plan deactivated successfully |
| 403 | Forbidden (not SUPER_ADMIN) |
| 404 | Plan not found |

---

## Webhook Endpoints

Base URL: `/api/v1/paypal`

### Handle PayPal Webhook

Receives and processes PayPal webhook events.

**Endpoint:** `POST /api/v1/paypal/webhook`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| PAYPAL-TRANSMISSION-ID | string | No | PayPal transmission ID |
| PAYPAL-TRANSMISSION-TIME | string | No | Transmission timestamp |
| PAYPAL-TRANSMISSION-SIG | string | No | Webhook signature |
| PAYPAL-CERT-URL | string | No | Certificate URL |
| PAYPAL-AUTH-ALGO | string | No | Auth algorithm |

**Request Body:** Raw JSON payload from PayPal

**Supported Event Types:**
| Event Type | Description |
|------------|-------------|
| `PAYMENT.CAPTURE.COMPLETED` | Payment captured |
| `PAYMENT.CAPTURE.DENIED` | Payment denied |
| `PAYMENT.CAPTURE.REFUNDED` | Payment refunded |
| `BILLING.SUBSCRIPTION.CREATED` | Subscription created |
| `BILLING.SUBSCRIPTION.ACTIVATED` | Subscription activated |
| `BILLING.SUBSCRIPTION.UPDATED` | Subscription updated |
| `BILLING.SUBSCRIPTION.CANCELLED` | Subscription cancelled |
| `BILLING.SUBSCRIPTION.SUSPENDED` | Subscription suspended |
| `PAYMENT.SALE.COMPLETED` | Subscription payment completed |

**Response:**
| Body | Description |
|------|-------------|
| `Webhook processed successfully` | Event processed |
| `Webhook received but processing failed: <error>` | Error (200 to prevent retries) |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Webhook received (always returns 200 to prevent retries) |

---

## Error Responses

All endpoints may return the following error format:

```json
{
  "status": false,
  "message": "Error description",
  "data": null
}
```

---

## Configuration

The following environment variables must be configured:

```properties
paypal.client-id=YOUR_CLIENT_ID
paypal.client-secret=YOUR_CLIENT_SECRET
paypal.mode=sandbox
paypal.webhook-id=YOUR_WEBHOOK_ID
paypal.return-url=https://yourapp.com/paypal/return
paypal.cancel-url=https://yourapp.com/paypal/cancel
```

**Mode Values:**
- `sandbox` - For testing
- `live` - For production

---

## Testing

Use PayPal sandbox credentials for testing:

1. Create a sandbox account at [developer.paypal.com](https://developer.paypal.com)
2. Generate sandbox API credentials
3. Use sandbox personal account for test payments

---

*Documentation last updated: January 2024*
