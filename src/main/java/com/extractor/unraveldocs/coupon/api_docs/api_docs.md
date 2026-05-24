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
  "statusCode": 400,
  "status": "success",
  "message": "Coupon has expired",
  "data": {
    "message": "Coupon has expired",
    "couponData": null,
    "errorCode": "COUPON_EXPIRED",
    "valid": false
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

**Response (400 Bad Request - Invalid):**

```json
{
  "statusCode": 400,
  "error": "Invalid Coupon",
  "message": "Coupon is not yet valid"
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
