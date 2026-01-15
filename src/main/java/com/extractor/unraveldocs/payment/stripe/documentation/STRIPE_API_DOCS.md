# Stripe Payment Gateway API Documentation

This document provides comprehensive documentation for all Stripe payment gateway endpoints in the UnravelDocs API.

---

## Table of Contents

1. [Customer Endpoints](#customer-endpoints)
2. [Payment Endpoints](#payment-endpoints)
3. [Subscription Endpoints](#subscription-endpoints)
4. [Webhook Endpoints](#webhook-endpoints)

---

## Authentication

All endpoints (except webhooks) require Bearer token authentication.

```
Authorization: Bearer <access_token>
```

---

## Customer Endpoints

Base URL: `/api/v1/stripe/customer`

### Get Customer Details

Retrieves customer information including payment methods.

**Endpoint:** `GET /api/v1/stripe/customer/details`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Response:**
```json
{
  "id": "cus_abc123",
  "email": "user@example.com",
  "name": "John Doe",
  "defaultPaymentMethodId": "pm_xyz789",
  "paymentMethods": [
    {
      "id": "pm_xyz789",
      "type": "card",
      "card": {
        "brand": "visa",
        "last4": "4242",
        "expMonth": 12,
        "expYear": 2025
      }
    }
  ],
  "createdAt": 1704067200
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| id | string | Stripe customer ID |
| email | string | Customer's email address |
| name | string | Customer's full name |
| defaultPaymentMethodId | string | Default payment method ID |
| paymentMethods | array | List of attached payment methods |
| createdAt | long | Unix timestamp of customer creation |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Customer details retrieved successfully |
| 500 | Failed to retrieve customer details (Stripe error) |

---

### Attach Payment Method

Attaches a payment method to the customer's account.

**Endpoint:** `POST /api/v1/stripe/customer/payment-method/attach`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| paymentMethodId | string | Yes | Stripe payment method ID to attach |

**Response:**
- `200 OK` - Payment method attached successfully (empty body)
- `500 Internal Server Error` - Failed to attach payment method

---

### Set Default Payment Method

Sets a payment method as the customer's default.

**Endpoint:** `POST /api/v1/stripe/customer/payment-method/set-default`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| paymentMethodId | string | Yes | Stripe payment method ID to set as default |

**Response:**
- `200 OK` - Default payment method set successfully (empty body)
- `500 Internal Server Error` - Failed to set default payment method

---

## Payment Endpoints

Base URL: `/api/v1/stripe/payment`

### Create Payment Intent

Creates a payment intent for one-time payments.

**Endpoint:** `POST /api/v1/stripe/payment/create-payment-intent`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "amount": 5000,
  "currency": "usd",
  "paymentMethodTypes": ["card"],
  "description": "Payment for document processing",
  "receiptEmail": "user@example.com",
  "metadata": {
    "orderId": "order_123"
  },
  "captureMethod": false
}
```

**Request Fields:**
| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| amount | long | Yes | - | Amount in cents (minimum 50) |
| currency | string | Yes | "usd" | 3-letter ISO currency code |
| paymentMethodTypes | array | No | ["card"] | Payment method types to allow |
| description | string | No | null | Payment description |
| receiptEmail | string | No | null | Email for payment receipt |
| metadata | object | No | null | Custom key-value metadata |
| captureMethod | boolean | No | null | Auto (false) or manual (true) capture |

**Response:**
```json
{
  "id": "pi_abc123",
  "status": "requires_payment_method",
  "amount": 5000,
  "currency": "usd",
  "clientSecret": "pi_abc123_secret_xyz",
  "paymentMethodId": null,
  "receiptUrl": null,
  "description": "Payment for document processing",
  "createdAt": 1704067200
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| id | string | Payment intent ID |
| status | string | Payment intent status |
| amount | long | Amount in smallest currency unit |
| currency | string | Currency code |
| clientSecret | string | Client secret for frontend confirmation |
| paymentMethodId | string | Attached payment method ID (if any) |
| receiptUrl | string | Receipt URL (after successful payment) |
| description | string | Payment description |
| createdAt | long | Unix timestamp of creation |

**Payment Intent Statuses:**
- `requires_payment_method` - Awaiting payment method
- `requires_confirmation` - Needs confirmation
- `requires_action` - Requires additional action (e.g., 3D Secure)
- `processing` - Payment is processing
- `requires_capture` - Ready for capture (manual capture)
- `succeeded` - Payment successful
- `canceled` - Payment canceled

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Payment intent created successfully |
| 500 | Failed to create payment intent |

---

### Get Payment Intent

Retrieves the status and details of a payment intent.

**Endpoint:** `GET /api/v1/stripe/payment/intent/{paymentIntentId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| paymentIntentId | string | Yes | Stripe payment intent ID |

**Response:**
```json
{
  "id": "pi_abc123",
  "status": "succeeded",
  "amount": 5000,
  "currency": "usd",
  "clientSecret": "pi_abc123_secret_xyz",
  "paymentMethodId": "pm_xyz789",
  "receiptUrl": "https://pay.stripe.com/receipts/...",
  "description": "Payment for document processing",
  "createdAt": 1704067200
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Payment intent retrieved successfully |
| 404 | Payment intent not found |

---

### Process Refund

Processes a refund for a payment.

**Endpoint:** `POST /api/v1/stripe/payment/refund`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "paymentIntentId": "pi_abc123",
  "amount": 2500,
  "reason": "requested_by_customer"
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| paymentIntentId | string | Yes | Payment intent ID to refund |
| amount | long | No | Amount to refund in cents (null = full refund) |
| reason | string | No | Refund reason: `duplicate`, `fraudulent`, or `requested_by_customer` |

**Response:**
```json
{
  "id": "re_abc123",
  "status": "succeeded",
  "amount": 2500,
  "currency": "usd",
  "reason": "requested_by_customer",
  "paymentIntentId": "pi_abc123",
  "createdAt": 1704153600
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| id | string | Refund ID |
| status | string | Refund status: `pending`, `succeeded`, `failed`, `canceled` |
| amount | long | Refunded amount in cents |
| currency | string | Currency code |
| reason | string | Refund reason |
| paymentIntentId | string | Original payment intent ID |
| createdAt | long | Unix timestamp of refund |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Refund processed successfully |
| 500 | Failed to process refund |

---

### Get Payment History

Retrieves paginated payment history for the authenticated user.

**Endpoint:** `GET /api/v1/stripe/payment/history`

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
      "stripePaymentIntentId": "pi_abc123",
      "userId": "user_uuid",
      "amount": 5000,
      "currency": "usd",
      "status": "succeeded",
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

## Subscription Endpoints

Base URL: `/api/v1/stripe/subscription`

### Create Checkout Session

Creates a Stripe Checkout session for subscription or one-time payment.

**Endpoint:** `POST /api/v1/stripe/subscription/create-checkout-session`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "priceId": "price_abc123",
  "mode": "subscription",
  "successUrl": "https://yourapp.com/success",
  "cancelUrl": "https://yourapp.com/cancel",
  "quantity": 1,
  "trialPeriodDays": 14,
  "promoCode": "SAVE20",
  "metadata": {
    "userId": "user_123"
  }
}
```

**Request Fields:**
| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| priceId | string | Yes | - | Stripe price ID |
| mode | string | No | "subscription" | `subscription` or `payment` |
| successUrl | string | No | null | Redirect URL on success |
| cancelUrl | string | No | null | Redirect URL on cancel |
| quantity | long | No | 1 | Quantity of items |
| trialPeriodDays | int | No | null | Trial period in days |
| promoCode | string | No | null | Promotional code |
| metadata | object | No | null | Custom metadata |

**Response:**
```json
{
  "sessionId": "cs_abc123",
  "sessionUrl": "https://checkout.stripe.com/pay/cs_abc123",
  "customerId": "cus_xyz789",
  "expiresAt": 1704153600
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| sessionId | string | Checkout session ID |
| sessionUrl | string | URL to redirect user for checkout |
| customerId | string | Stripe customer ID |
| expiresAt | long | Unix timestamp when session expires |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Checkout session created successfully |
| 500 | Failed to create checkout session |

---

### Create Subscription Directly

Creates a subscription directly without checkout flow.

**Endpoint:** `POST /api/v1/stripe/subscription/create`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | string | Yes | Bearer token |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "priceId": "price_abc123",
  "quantity": 1,
  "trialPeriodDays": 14,
  "promoCode": "SAVE20",
  "paymentBehavior": "default_incomplete",
  "metadata": {
    "source": "api"
  },
  "defaultPaymentMethodId": "pm_xyz789"
}
```

**Request Fields:**
| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| priceId | string | Yes | - | Stripe price ID |
| quantity | long | No | 1 | Subscription quantity |
| trialPeriodDays | int | No | null | Trial period in days |
| promoCode | string | No | null | Promotional code |
| paymentBehavior | string | No | "default_incomplete" | How to handle payment failures |
| metadata | object | No | null | Custom metadata |
| defaultPaymentMethodId | string | No | null | Default payment method |

**Response:**
```json
{
  "id": "sub_abc123",
  "status": "active",
  "customerId": "cus_xyz789",
  "currentPeriodStart": 1704067200,
  "currentPeriodEnd": 1706745600,
  "trialEnd": null,
  "defaultPaymentMethodId": "pm_xyz789",
  "latestInvoiceId": "in_abc123",
  "items": [
    {
      "id": "si_abc123",
      "priceId": "price_abc123",
      "quantity": 1,
      "unitAmount": 1999,
      "currency": "usd"
    }
  ],
  "cancelAtPeriodEnd": false
}
```

**Subscription Statuses:**
- `incomplete` - Initial payment attempt failed
- `incomplete_expired` - First invoice not paid within 23 hours
- `trialing` - In trial period
- `active` - Subscription is active
- `past_due` - Payment failed but still retrying
- `canceled` - Subscription has been canceled
- `unpaid` - All payment retries exhausted
- `paused` - Subscription is paused

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription created successfully |
| 500 | Failed to create subscription |

---

### Cancel Subscription

Cancels a subscription.

**Endpoint:** `POST /api/v1/stripe/subscription/{subscriptionId}/cancel`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | Stripe subscription ID |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| immediately | boolean | No | false | Cancel immediately or at period end |

**Response:**
```json
{
  "id": "sub_abc123",
  "status": "canceled",
  "customerId": "cus_xyz789",
  "currentPeriodStart": 1704067200,
  "currentPeriodEnd": 1706745600,
  "cancelAtPeriodEnd": true,
  "items": [...]
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription canceled successfully |
| 500 | Failed to cancel subscription |

---

### Pause Subscription

Pauses a subscription.

**Endpoint:** `POST /api/v1/stripe/subscription/{subscriptionId}/pause`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | Stripe subscription ID |

**Response:**
```json
{
  "id": "sub_abc123",
  "status": "paused",
  ...
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription paused successfully |
| 500 | Failed to pause subscription |

---

### Resume Subscription

Resumes a paused subscription.

**Endpoint:** `POST /api/v1/stripe/subscription/{subscriptionId}/resume`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | Stripe subscription ID |

**Response:**
```json
{
  "id": "sub_abc123",
  "status": "active",
  ...
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription resumed successfully |
| 500 | Failed to resume subscription |

---

### Get Subscription Details

Retrieves subscription details.

**Endpoint:** `GET /api/v1/stripe/subscription/{subscriptionId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| subscriptionId | string | Yes | Stripe subscription ID |

**Response:**
```json
{
  "id": "sub_abc123",
  "status": "active",
  "customerId": "cus_xyz789",
  "currentPeriodStart": 1704067200,
  "currentPeriodEnd": 1706745600,
  "trialEnd": null,
  "defaultPaymentMethodId": "pm_xyz789",
  "latestInvoiceId": "in_abc123",
  "items": [
    {
      "id": "si_abc123",
      "priceId": "price_abc123",
      "quantity": 1,
      "unitAmount": 1999,
      "currency": "usd"
    }
  ],
  "cancelAtPeriodEnd": false
}
```

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Subscription retrieved successfully |
| 404 | Subscription not found |

---

## Webhook Endpoints

Base URL: `/api/v1/stripe/webhook`

### Handle Stripe Webhook

Receives and processes Stripe webhook events.

**Endpoint:** `POST /api/v1/stripe/webhook`

**Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Stripe-Signature | string | Yes | Webhook signature for verification |

**Request Body:** Raw JSON payload from Stripe

**Supported Event Types:**
| Event Type | Description |
|------------|-------------|
| `checkout.session.completed` | Checkout session completed successfully |
| `payment_intent.succeeded` | Payment was successful |
| `payment_intent.payment_failed` | Payment attempt failed |
| `customer.subscription.created` | New subscription created |
| `customer.subscription.updated` | Subscription updated |
| `customer.subscription.deleted` | Subscription canceled |
| `invoice.payment_succeeded` | Invoice payment successful |
| `invoice.payment_failed` | Invoice payment failed |

**Response:**
| Body | Description |
|------|-------------|
| `Webhook processed successfully` | Event processed |
| `Event already processed` | Duplicate event (idempotency) |
| `Event type not handled` | Unknown event type |
| `Signature verification failed` | Invalid signature |
| `Error processing webhook` | Processing error |

**Status Codes:**
| Code | Description |
|------|-------------|
| 200 | Webhook received (includes errors - prevents retries) |
| 400 | Signature verification failed |
| 500 | Internal processing error |

---

## Error Responses

All endpoints may return the following error format on failure:

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to process request",
  "path": "/api/v1/stripe/..."
}
```

---

## Rate Limiting

Stripe API has its own rate limits. The application does not add additional rate limiting for Stripe endpoints.

---

## Testing

Use Stripe test mode with test API keys and the following test card numbers:

| Card Number | Description |
|-------------|-------------|
| 4242424242424242 | Succeeds and charges immediately |
| 4000002500003155 | Requires 3D Secure authentication |
| 4000000000009995 | Card declined |
| 4000000000000341 | Attaching card succeeds, but payment fails |

---

## Configuration

The following environment variables must be configured:

```properties
stripe.api-key=sk_test_xxx
stripe.public-key=pk_test_xxx
stripe.webhook-secret=whsec_xxx
```

---

*Documentation last updated: January 2024*
