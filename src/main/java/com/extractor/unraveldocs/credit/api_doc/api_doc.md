# Credit Pack API Documentation

## Base URLs

| Audience | Base Path              |
|----------|------------------------|
| User     | `/api/v1/credits`      |
| Admin    | `/api/v1/admin/credits` |

---

## User Endpoints

### 1. List Available Credit Packs
**`GET /api/v1/credits/packs`**

Returns all active credit packs available for purchase.

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credit packs retrieved",
  "data": [
    {
      "id": "uuid",
      "name": "VALUE_PACK",
      "displayName": "Value Pack",
      "priceInCents": 1500,
      "currency": "USD",
      "creditsIncluded": 75,
      "costPerCredit": 20.00
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
  "creditPackId": "uuid",
  "gateway": "STRIPE",
  "couponCode": "SAVE10",
  "callbackUrl": "https://example.com/callback",
  "cancelUrl": "https://example.com/cancel"
}
```

| Field          | Type   | Required | Notes                             |
|----------------|--------|----------|-----------------------------------|
| `creditPackId` | string | ✅        | UUID of the credit pack           |
| `gateway`      | string | ✅        | `STRIPE`, `PAYSTACK`, or `PAYPAL` |
| `couponCode`   | string | ❌        | Optional discount coupon          |
| `callbackUrl`  | string | ❌        | Success redirect URL              |
| `cancelUrl`    | string | ❌        | Cancel redirect URL               |

**Response:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Payment initialized",
  "data": {
    "paymentUrl": "https://checkout.stripe.com/...",
    "reference": "pi_abc123",
    "packName": "Value Pack",
    "creditsToReceive": 75,
    "amountInCents": 1350,
    "discountApplied": 150
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
    "transferId": "uuid",
    "creditsTransferred": 10,
    "senderBalanceAfter": 35,
    "recipientEmail": "jane@example.com",
    "recipientName": "Jane Doe"
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
        "transactionId": "uuid",
        "type": "PURCHASE",
        "amount": 75,
        "balanceAfter": 80,
        "description": "Purchased Value Pack (75 credits)",
        "createdAt": "2026-02-10T12:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
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
  "priceInCents": 600,
  "isActive": false
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

| Field    | Type    | Required | Notes                      |
|----------|---------|----------|----------------------------|
| `userId` | string  | ✅        | Target user's ID           |
| `amount` | integer | ✅        | Credits to allocate (min: 1)|
| `reason` | string  | ❌        | Optional admin note         |

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

## Credit Pack Pricing Examples
| Pack Name    | Price (USD) | Credits Included | Cost per Credit (USD) | User Perception      |
|--------------|-------------|------------------|-----------------------|----------------------|
| Starter Pack | $5.00       | 20               | $0.25                 | Just trying it out   |
| Value Pack   | $15.00      | 75               | $0.20                 | Good deal            |
| Power Pack   | $30.00      | 200              | $0.15                 | Best value for money |