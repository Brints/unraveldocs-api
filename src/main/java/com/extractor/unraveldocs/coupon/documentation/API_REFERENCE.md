# Coupon API Reference

Complete API reference for the UnravelDocs Coupon System.

---

## Base URLs

| Environment | Base URL                                     |
|-------------|----------------------------------------------|
| Local       | `http://localhost:8080/api/v1`               |
| Staging     | `https://staging-api.unraveldocs.com/api/v1` |
| Production  | `https://api.unraveldocs.com/api/v1`         |

---

## Authentication

All endpoints require authentication via JWT Bearer token:

```http
Authorization: Bearer <jwt_token>
```

---

## Admin Endpoints

### Create Coupon

Creates a new coupon code.

```http
POST /admin/coupons
```

**Request Fields:**

| Field                 | Type        | Required | Description                                                                |
|-----------------------|-------------|----------|----------------------------------------------------------------------------|
| `customCode`          | `string`    | Yes      | Unique code for the coupon (alphanumeric, 5-20 chars)                      |
| `description`         | `string`    | Yes      | Description of the coupon                                                  |
| `discountPercentage`  | `decimal`   | Yes      | Discount percentage (e.g., 15.00 for 15%)                                  |
| `minPurchaseAmount`   | `decimal`   | No       | Minimum purchase amount to apply coupon (default: 0.00)                    |
| `validFrom`           | `datetime`  | Yes      | Start date/time for coupon validity (ISO 8601 format)                      |
| `validUntil`          | `datetime`  | Yes      | End date/time for coupon validity (ISO 8601 format)                        |
| `maxUsageCount`       | `int`       | No       | Maximum total uses for the coupon (default: unlimited)                     |
| `maxUsagePerUser`     | `int`       | No       | Maximum uses per user (default: unlimited)                                 |
| `recipientCategory`   | `string`    | Yes      | Target users: `ALL_USERS`, `NEW_USERS`, `ALL_PAID_USERS`, `SPECIFIC_USERS` |
| `specificUserIds`     | `array`     | No       | List of user IDs (UUIDs) if `recipientCategory` is `SPECIFIC_USERS`        |
| `templateId`          | `string`    | No       | ID of coupon template to base this coupon on (if any)                      |

**Request Body:**

```json
{
  "customCode": "BLACKFRIDAY20",
  "description": "Black Friday 20% off",
  "discountPercentage": 20.00,
  "minPurchaseAmount": 10.00,
  "validFrom": "2026-11-20T00:00:00Z",
  "validUntil": "2026-11-30T23:59:59Z",
  "maxUsageCount": 1000,
  "maxUsagePerUser": 1,
  "recipientCategory": "ALL_PAID_USERS",
  "specificUserIds": ["user-id-1"],
  "templateId": "template-id"
}
```

**Response (201 Created):**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Coupon created successfully",
  "data": {
    "id": "10eb1828-45c3-4da1-8c7a-4b28f2b4fb51",
    "code": "NEWSIGNUP17",
    "description": "New User 17% off",
    "recipientCategory": "NEW_USERS",
    "discountPercentage": 17.00,
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
    "createdAt": null,
    "updatedAt": null,
    "active": true,
    "currentlyValid": false,
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

### Update Coupon

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
  "description": "Updated description",
  "discountPercentage": 25.00,
  "minPurchaseAmount": 15.00,
  "validUntil": "2026-12-31T23:59:59Z",
  "maxUsageCount": 2000,
  "maxUsagePerUser": 2
}
```

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon updated successfully",
  "data": {
    "id": "10eb1828-45c3-4da1-8c7a-4b28f2b4fb51",
    "code": "NEWSIGNUP17",
    "description": "NEWSIGNUP17 is now set to 22%",
    "recipientCategory": "NEW_USERS",
    "discountPercentage": 22.00,
    "minPurchaseAmount": 15.00,
    "maxUsageCount": 2000,
    "maxUsagePerUser": 2,
    "currentUsageCount": 0,
    "validFrom": "2026-01-26T20:00:00Z",
    "validUntil": "2026-02-01T23:59:59Z",
    "templateId": null,
    "templateName": null,
    "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
    "createdByName": "Admin User",
    "createdAt": "2026-01-26T13:33:57.710198Z",
    "updatedAt": "2026-01-26T13:33:57.710198Z",
    "active": true,
    "currentlyValid": false,
    "customCode": true,
    "expired": false
  }
}
```

---

### Deactivate Coupon

Deactivates a coupon (soft delete).

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

### Get Coupon by ID

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
    "id": "10eb1828-45c3-4da1-8c7a-4b28f2b4fb51",
    "code": "NEWSIGNUP17",
    "description": "NEWSIGNUP17 is now set to 22%",
    "recipientCategory": "NEW_USERS",
    "discountPercentage": 22.00,
    "minPurchaseAmount": 15.00,
    "maxUsageCount": 2000,
    "maxUsagePerUser": 2,
    "currentUsageCount": 0,
    "validFrom": "2026-01-26T20:00:00Z",
    "validUntil": "2026-02-01T23:59:59Z",
    "templateId": null,
    "templateName": null,
    "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
    "createdByName": "Admin User",
    "createdAt": "2026-01-26T13:33:57.710198Z",
    "updatedAt": "2026-01-26T13:39:04.12812Z",
    "active": true,
    "currentlyValid": false,
    "customCode": true,
    "expired": false
  }
}
```

---

### List All Coupons

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
        "expired": false
      },
      {
        "id": "10eb1828-45c3-4da1-8c7a-4b28f2b4fb51",
        "code": "NEWSIGNUP17",
        "description": "NEWSIGNUP17 is now set to 22%",
        "recipientCategory": "NEW_USERS",
        "discountPercentage": 22.00,
        "minPurchaseAmount": 15.00,
        "maxUsageCount": 2000,
        "maxUsagePerUser": 2,
        "currentUsageCount": 0,
        "validFrom": "2026-01-26T20:00:00Z",
        "validUntil": "2026-02-01T23:59:59Z",
        "templateId": null,
        "templateName": null,
        "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
        "createdByName": "Admin User",
        "createdAt": "2026-01-26T13:33:57.710198Z",
        "updatedAt": "2026-01-26T13:39:04.12812Z",
        "active": true,
        "currentlyValid": false,
        "customCode": true,
        "expired": false
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

---

### Get Coupon Usage History

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

### Get Coupon Analytics

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

### Bulk Generate Coupons

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

---

## User Endpoints

### Validate Coupon

Validates a coupon code for the current user.

```http
GET /coupons/validate/{code}
```

**Response (200 OK - Valid):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon is valid",
  "data": {
    "message": "Coupon is valid",
    "couponData": {
      "id": "27f09844-39ec-4354-9038-e9deeb6c81bc",
      "code": "AHXQ1PF8",
      "description": null,
      "recipientCategory": "NEW_USERS",
      "discountPercentage": 20.00,
      "minPurchaseAmount": null,
      "maxUsageCount": null,
      "maxUsagePerUser": 1,
      "currentUsageCount": 0,
      "validFrom": "2026-01-26T14:33:00Z",
      "validUntil": "2026-02-21T23:59:59Z",
      "templateId": null,
      "templateName": null,
      "createdById": "d28feb5b-4fc9-4653-aae4-285ce0a70975",
      "createdByName": "Admin User",
      "createdAt": "2026-01-26T14:32:46.30911Z",
      "updatedAt": "2026-01-26T14:32:46.30911Z",
      "active": true,
      "currentlyValid": true,
      "customCode": false,
      "expired": false
    },
    "errorCode": null,
    "valid": true
  }
}
```

**Response (400 Bad Request - Invalid):**

```json
{
  "status": "success",
  "data": {
    "isValid": false,
    "errorCode": "COUPON_EXPIRED",
    "errorMessage": "This coupon has expired"
  }
}
```

**Response (400 Bad Request - Not yet Valid)
```json
{
  "statusCode": 400,
  "status": "success",
  "message": "Coupon is not yet valid",
  "data": {
    "message": "Coupon is not yet valid",
    "couponData": null,
    "errorCode": "COUPON_NOT_YET_VALID",
    "valid": false
  }
}
```

---

### Apply Coupon

Applies a coupon to calculate discount.

```http
POST /coupons/apply
```

**Request Body:**

```json
{
  "couponCode": "BLACKFRIDAY20",
  "amount": 99.00
}
```

**Response (200 OK):**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon applied successfully",
  "data": {
    "couponCode": "AHXQ1PF8",
    "originalAmount": 99.00,
    "discountPercentage": 20.00,
    "discountAmount": 19.80,
    "finalAmount": 79.20,
    "currency": null,
    "minPurchaseAmount": null,
    "minPurchaseRequirementMet": true
  }
}
```

---

### Get Available Coupons

Gets all coupons available for the current user.

```http
GET /coupons/available
```

**Response (200 OK):**

```json
{
  "status": "success",
  "data": {
    "coupons": [
      {
        "code": "WELCOME10",
        "description": "Welcome discount for new users",
        "discountPercentage": 10.00,
        "validUntil": "2026-02-28T23:59:59Z"
      },
      {
        "code": "UPGRADE20",
        "description": "Upgrade to Pro discount",
        "discountPercentage": 20.00,
        "validUntil": "2026-03-31T23:59:59Z"
      }
    ],
    "count": 2
  }
}
```

---

## Error Response Format

All errors follow this format:

```json
{
  "status": "error",
  "message": "Human readable error message",
  "errorCode": "ERROR_CODE",
  "timestamp": "2026-01-26T12:00:00Z",
  "path": "/api/v1/coupons/validate/INVALID"
}
```

---

## Rate Limiting

| Endpoint Type | Limit              |
|---------------|--------------------|
| Validation    | 60 requests/minute |
| Apply         | 30 requests/minute |
| Admin Create  | 10 requests/minute |

---

*Last Updated: January 2026*
