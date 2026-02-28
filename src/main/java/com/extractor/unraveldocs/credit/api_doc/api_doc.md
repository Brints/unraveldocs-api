# Credit Pack API Documentation

## Base URLs

| Audience | Base Path               |
|----------|-------------------------|
| User     | `/api/v1/credits`       |
| Admin    | `/api/v1/admin/credits` |

---

## User Endpoints

### 1. List Available Credit Packs
**`GET /api/v1/credits/packs`**
**`GET /api/v1/credits/packs?currency=NGN`**

Returns all active credit packs. Use `?currency=NGN` to include converted prices.

| Param      | Type   | Required | Notes                                         |
|------------|--------|----------|-----------------------------------------------|
| `currency` | string | ❌        | ISO currency code (e.g., `NGN`, `EUR`, `GBP`) |

**Response (USD, no currency param):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credit packs retrieved",
  "data": [
    {
      "id": "e2d6f74d-4422-4ace-b9cb-aa6f50067074",
      "name": "STARTER_PACK",
      "displayName": "Starter Pack",
      "priceInCents": 500,
      "currency": "USD",
      "creditsIncluded": 20,
      "costPerCredit": 25.00,
      "convertedPriceInCents": 775000,
      "convertedCurrency": "NGN",
      "formattedPrice": "₦7,750.00",
      "formattedOriginalPrice": "$5.00",
      "exchangeRate": 1550.00
    },
    {
      "id": "bbc04958-0530-4dc0-9be7-0dc5fdabd1a3",
      "name": "VALUE_PACK",
      "displayName": "Value Pack",
      "priceInCents": 1500,
      "currency": "USD",
      "creditsIncluded": 75,
      "costPerCredit": 20.00,
      "convertedPriceInCents": 2325000,
      "convertedCurrency": "NGN",
      "formattedPrice": "₦23,250.00",
      "formattedOriginalPrice": "$15.00",
      "exchangeRate": 1550.00
    },
    {
      "id": "8ac54c55-6bed-4d02-8924-4cc98aad1d4f",
      "name": "POWER_PACK",
      "displayName": "Power Pack",
      "priceInCents": 3000,
      "currency": "USD",
      "creditsIncluded": 200,
      "costPerCredit": 15.00,
      "convertedPriceInCents": 4650000,
      "convertedCurrency": "NGN",
      "formattedPrice": "₦46,500.00",
      "formattedOriginalPrice": "$30.00",
      "exchangeRate": 1550.00
    }
  ]
}
```

**Response (with `?currency=GBP`):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credit packs retrieved",
  "data": [
    {
      "id": "e2d6f74d-4422-4ace-b9cb-aa6f50067074",
      "name": "STARTER_PACK",
      "displayName": "Starter Pack",
      "priceInCents": 500,
      "currency": "USD",
      "creditsIncluded": 20,
      "costPerCredit": 25.00,
      "convertedPriceInCents": 395,
      "convertedCurrency": "GBP",
      "formattedPrice": "£3.95",
      "formattedOriginalPrice": "$5.00",
      "exchangeRate": 0.79
    },
    {
      "id": "bbc04958-0530-4dc0-9be7-0dc5fdabd1a3",
      "name": "VALUE_PACK",
      "displayName": "Value Pack",
      "priceInCents": 1500,
      "currency": "USD",
      "creditsIncluded": 75,
      "costPerCredit": 20.00,
      "convertedPriceInCents": 1185,
      "convertedCurrency": "GBP",
      "formattedPrice": "£11.85",
      "formattedOriginalPrice": "$15.00",
      "exchangeRate": 0.79
    },
    {
      "id": "8ac54c55-6bed-4d02-8924-4cc98aad1d4f",
      "name": "POWER_PACK",
      "displayName": "Power Pack",
      "priceInCents": 3000,
      "currency": "USD",
      "creditsIncluded": 200,
      "costPerCredit": 15.00,
      "convertedPriceInCents": 2370,
      "convertedCurrency": "GBP",
      "formattedPrice": "£23.70",
      "formattedOriginalPrice": "$30.00",
      "exchangeRate": 0.79
    }
  ]
}
```

---

### 2. Get Credit Balance
**`GET /api/v1/credits/balance`**

Returns the authenticated user's current credit balance.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credit balance retrieved",
  "data": {
    "balance": 45,
    "totalPurchased": 75,
    "totalUsed": 30
  }
}
```

---

### 3. Purchase a Credit Pack
**`POST /api/v1/credits/purchase`**

Initializes a credit pack purchase. Supports optional coupon code for discounts.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "creditPackId": "e2d6f74d-4422-4ace-b9cb-aa6f50067074",
  "gateway": "STRIPE or PAYPAL or PAYSTACK",
  "couponCode": "SAVE10",
  "callbackUrl": "https://example.com/callback",
  "cancelUrl": "https://example.com/cancel",
  "currency": "NGN"
}
```

| Field          | Type   | Required | Notes                             |
|----------------|--------|----------|-----------------------------------|
| `creditPackId` | string | ✅        | UUID of the credit pack           |
| `gateway`      | string | ✅        | `STRIPE`, `PAYSTACK`, or `PAYPAL` |
| `couponCode`   | string | ❌        | Optional discount coupon          |
| `callbackUrl`  | string | ❌        | Success redirect URL              |
| `cancelUrl`    | string | ❌        | Cancel redirect URL               |
| `currency`     | string | ✅        | Payment currency (e.g., `NGN`).   |

**Request Body (Paystack Example):**
```json
{
  "creditPackId": "e2d6f74d-4422-4ace-b9cb-aa6f50067074",
  "gateway": "PAYSTACK",
  "couponCode": "",
  "callbackUrl": "https://8hw23p5s-8080.uks1.devtunnels.ms/payment/callback",
  "cancelUrl": "",
  "currency": "NGN"
}
```
**Response (Paystack NGN Example):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Payment initialized",
  "data": {
    "paymentUrl": "https://checkout.paystack.com/sz7hd0pgg1plfth",
    "reference": "PAY_C2692DAD5D344214",
    "packName": "Starter Pack",
    "creditsToReceive": 20,
    "amountInCents": 775000,
    "discountApplied": 0,
    "currency": "NGN",
    "formattedAmount": "₦7,750.00",
    "exchangeRate": 1550.00
  }
}
```

**Response (Paystack KES Example — with currency conversion):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Payment initialized",
  "data": {
    "paymentUrl": "https://checkout.paystack.com/g89w5qoguzgruvt",
    "reference": "PAY_DB33E9B306AF4610",
    "packName": "Starter Pack",
    "creditsToReceive": 20,
    "amountInCents": 76500,
    "discountApplied": 0,
    "currency": "KES",
    "formattedAmount": "Ksh765.00",
    "exchangeRate": 153.00
  }
}
```

**Stripe Response Example:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Payment initialized",
  "data": {
    "paymentUrl": "https://checkout.stripe.com/c/pay/cs_test_a1PiwMllMKQSd2ASaAWtMIlbbd2jWSPjMu4lit0ljxdycuUDShI3dnolUT#fidnandhYHdWcXxpYCc%2FJ2FgY2RwaXEnKSdkdWxOYHwnPyd1blpxYHZxWjA0S1NIbDBPVWBQSEY3T0lfSE1ufGk0fH19SmE0QExrXz1uQUx2bTxHSzF3ZFVUSFROPEF3TGNEX2xqYjR8aDRdYzdKcnB2PTNfSE9mT2I9ZzVGRzA3aUQwNTU9akdIT11sVicpJ2N3amhWYHdzYHcnP3F3cGApJ2dkZm5id2pwa2FGamlqdyc%2FJyZjY2NjY2MnKSdpZHxqcHFRfHVgJz8ndmxrYmlgWmxxYGgnKSdga2RnaWBVaWRmYG1qaWFgd3YnP3F3cGB4JSUl",
    "reference": "cs_test_a1PiwMllMKQSd2ASaAWtMIlbbd2jWSPjMu4lit0ljxdycuUDShI3dnolUT",
    "packName": "Starter Pack",
    "creditsToReceive": 20,
    "amountInCents": 500,
    "discountApplied": 0,
    "currency": "USD",
    "formattedAmount": "$5.00",
    "exchangeRate": 1
  }
}
```

**Stripe Response Example (with currency conversion):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Payment initialized",
  "data": {
    "paymentUrl": "https://checkout.stripe.com/c/pay/cs_test_a1o4A0Wqj5ZNimR07PTVGp5f9PG6OnhNJouyTPwRYxn6hvJGZLMZOzXBJY#fidnandhYHdWcXxpYCc%2FJ2FgY2RwaXEnKSdkdWxOYHwnPyd1blpxYHZxWjA0S1NIbDBPVWBQSEY3T0lfSE1ufGk0fH19SmE0QExrXz1uQUx2bTxHSzF3ZFVUSFROPEF3TGNEX2xqYjR8aDRdYzdKcnB2PTNfSE9mT2I9ZzVGRzA3aUQwNTU9akdIT11sVicpJ2N3amhWYHdzYHcnP3F3cGApJ2dkZm5id2pwa2FGamlqdyc%2FJyZjY2NjY2MnKSdpZHxqcHFRfHVgJz8ndmxrYmlgWmxxYGgnKSdga2RnaWBVaWRmYG1qaWFgd3YnP3F3cGB4JSUl",
    "reference": "cs_test_a1o4A0Wqj5ZNimR07PTVGp5f9PG6OnhNJouyTPwRYxn6hvJGZLMZOzXBJY",
    "packName": "Starter Pack",
    "creditsToReceive": 20,
    "amountInCents": 460,
    "discountApplied": 0,
    "currency": "EUR",
    "formattedAmount": "4,60 €",
    "exchangeRate": 0.92
  }
}
```

**Paypal Response Example:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Payment initialized",
  "data": {
    "paymentUrl": "https://www.sandbox.paypal.com/checkoutnow?token=21F722126U3384446",
    "reference": "21F722126U3384446",
    "packName": "Starter Pack",
    "creditsToReceive": 20,
    "amountInCents": 500,
    "discountApplied": 0,
    "currency": "USD",
    "formattedAmount": "$5.00",
    "exchangeRate": 1
  }
}
```

**Paypal Response Example (with currency conversion):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Payment initialized",
  "data": {
    "paymentUrl": "https://www.sandbox.paypal.com/checkoutnow?token=9E373121MN466550E",
    "reference": "9E373121MN466550E",
    "packName": "Starter Pack",
    "creditsToReceive": 20,
    "amountInCents": 395,
    "discountApplied": 0,
    "currency": "GBP",
    "formattedAmount": "£3.95",
    "exchangeRate": 0.79
  }
}
```
---

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

**Transfer Rules:**

| Rule                   | Regular User       | Admin / Super Admin |
|------------------------|--------------------|---------------------|
| Min balance after send | ≥ 5 credits        | ≥ 5 credits         |
| Monthly cap            | 30 credits/month   | **No cap**          |
| Self-transfer          | ❌ Not allowed      | ❌ Not allowed       |

**Error Responses:**
- `400` — Insufficient balance, monthly cap exceeded, or self-transfer attempt
- `404` — Recipient email not found

---

### 5. Get Transaction History
**`GET /api/v1/credits/transactions`**

Returns paginated credit transaction history.

**Headers:** `Authorization: Bearer <token>`

**Query Params:**

| Param  | Default | Description             |
|--------|---------|-------------------------|
| `page` | 0       | Page number (0-indexed) |
| `size` | 20      | Items per page          |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Transactions retrieved",
  "data": {
    "content": [
      {
        "transactionId": "cc5e0b85-cf64-4256-a944-02714a2394bb",
        "type": "TRANSFER_SENT",
        "amount": 6,
        "balanceAfter": 2147483561,
        "description": "Transferred 6 credits to afiaaniebiet0@gmail.com",
        "createdAt": "2026-02-16T10:09:17.82303Z"
      },
      {
        "transactionId": "3127d38f-4a9c-46ab-aaee-e274ec91110e",
        "type": "TRANSFER_SENT",
        "amount": 80,
        "balanceAfter": 2147483567,
        "description": "Transferred 80 credits to afiaaniebiet0@gmail.com",
        "createdAt": "2026-02-16T09:46:37.597739Z"
      },
      {
        "transactionId": "f7cacffd-6a78-4ab8-bb0f-ed1ea3422310",
        "type": "ADMIN_ALLOCATION",
        "amount": 2147483647,
        "balanceAfter": 2147483647,
        "description": "Unlimited credits assigned to admin/super admin",
        "createdAt": "2026-02-16T09:27:35.874245Z"
      },
      {
        "transactionId": "e2d6f74d-4422-4ace-b9cb-aa6f50067074",
        "type": "PURCHASE",
        "amount": 75,
        "balanceAfter": 80,
        "description": "Purchased Value Pack (75 credits)",
        "createdAt": "2026-02-10T12:00:00Z"
      }
    ],
    "empty": false,
    "first": true,
    "last": true,
    "number": 0,
    "numberOfElements": 3,
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
    "totalElements": 3,
    "totalPages": 1
  }
}
```

**Transaction Types:** `PURCHASE`, `DEDUCTION`, `REFUND`, `BONUS`, `ADMIN_ADJUSTMENT`, `TRANSFER_SENT`, `TRANSFER_RECEIVED`, `ADMIN_ALLOCATION`

---

### 6. Calculate Credit Cost
**`POST /api/v1/credits/calculate`**

Calculates how many credits are needed to process uploaded files.

**Headers:** `Authorization: Bearer <token>`

**Request:** `multipart/form-data` with `files` field

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Page count calculated",
  "data": {
    "totalPages": 12,
    "creditsRequired": 12,
    "currentBalance": 45,
    "hasEnoughCredits": true
  }
}
```

> **Note:** PDF files have their actual pages counted. Images count as 1 page each.

---

## Admin Endpoints

> All admin endpoints require `ADMIN` or `SUPER_ADMIN` role.

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

---

## Credit System Rules

| Rule                        | Detail                                                                                     |
|-----------------------------|--------------------------------------------------------------------------------------------|
| **Credit usage priority**   | Active plan quotas are used first. Credits only consumed when no active paid subscription. |
| **Credit deduction timing** | Credits deducted **after** successful OCR processing, not before.                          |
| **Conversion rate**         | 1 credit = 1 page processed via OCR                                                        |
| **Sign-up bonus**           | 5 free credits granted on user registration                                                |
| **Retroactive bonus**       | Existing users without credits get 5 free credits on app startup                           |
| **Admin/Super Admin**       | Receive unlimited credits (Integer.MAX_VALUE) at startup                                   |
| **OCR provider**            | Credit users use Google Vision OCR for processing                                          |
| **Purchase notifications**  | Push notification + email sent on successful purchase                                      |
| **Transfer notifications**  | Push notification + email sent to **both** sender and receiver on successful transfer      |
| **Transfer cap**            | Regular users: max 30 credits/month. Admin/Super Admin: no cap                             |
| **Transfer floor**          | Sender must retain at least 5 credits after any transfer                                   |

## Credit Purchase Flow

The purchase endpoint (`POST /api/v1/credits/purchase`) initializes payment with the chosen gateway. **Credits are automatically added to the user's balance** when the payment gateway sends a success webhook — no additional frontend action is needed after redirect.

### Flow

1. Frontend calls `POST /api/v1/credits/purchase` with pack ID and gateway
2. Backend returns a `paymentUrl` — frontend redirects the user there
3. User completes payment on the gateway's checkout page
4. Gateway sends a webhook to the backend confirming payment success
5. Backend webhook handler detects `PaymentType.CREDIT_PURCHASE` and calls `CreditPurchaseService.completePurchase()`
6. Credits are added to the user's balance, and push + email notifications are sent

### Required Metadata

When using gateway-specific endpoints directly (instead of `/api/v1/credits/purchase`), the frontend **must** include these fields in the payment metadata so the webhook can identify and fulfill the credit purchase:

| Field             | Type   | Required | Description                          |
|-------------------|--------|----------|--------------------------------------|
| `type`            | string | ✅        | Must be `"CREDIT_PURCHASE"`          |
| `creditPackId`    | string | ✅        | UUID of the credit pack being bought |
| `creditsIncluded` | number | ❌        | Number of credits in the pack        |

> **Note:** The `/api/v1/credits/purchase` endpoint sets this metadata automatically.

### Supported Gateways

| Gateway  | Webhook Event                                             | Detection Method                         |
|----------|-----------------------------------------------------------|------------------------------------------|
| Paystack | `charge.success`                                          | `PaymentType.CREDIT_PURCHASE` + metadata |
| PayPal   | `PAYMENT.CAPTURE.COMPLETED`                               | `PaymentType.CREDIT_PURCHASE` + metadata |
| Stripe   | `payment_intent.succeeded` / `checkout.session.completed` | Metadata `type` field                    |

---

## Credit Pack Pricing Examples

| Pack Name    | Price (USD) | Credits Included | Cost per Credit (USD) | User Perception      |
|--------------|-------------|------------------|-----------------------|----------------------|
| Starter Pack | $5.00       | 20               | $0.25                 | Just trying it out   |
| Value Pack   | $15.00      | 75               | $0.20                 | Good deal            |
| Power Pack   | $30.00      | 200              | $0.15                 | Best value for money |