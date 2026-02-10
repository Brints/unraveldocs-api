# Coupon System Documentation

## Overview

The UnravelDocs Coupon System is a comprehensive solution for managing promotional discounts. It supports creating, distributing, validating, and tracking coupon codes for various user segments and subscription plans.

## Table of Contents

1. [Architecture](#architecture)
2. [Core Components](#core-components)
3. [Features](#features)
4. [API Endpoints](#api-endpoints)
5. [Data Models](#data-models)
6. [Recipient Categories](#recipient-categories)
7. [Caching Strategy](#caching-strategy)
8. [Notification System](#notification-system)
9. [Scheduled Tasks](#scheduled-tasks)
10. [Testing](#testing)
11. [Configuration](#configuration)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Coupon System Architecture                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐       │
│  │  Admin          │     │  User           │     │  Scheduled      │       │
│  │  Controller     │     │  Controller     │     │  Jobs           │       │
│  └────────┬────────┘     └────────┬────────┘     └────────┬────────┘       │
│           │                       │                       │                 │
│           ▼                       ▼                       ▼                 │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │                     Service Layer                                │       │
│  │  ┌───────────────┐ ┌───────────────────┐ ┌──────────────────┐  │       │
│  │  │ CouponService │ │ ValidationService │ │ NotificationSvc  │  │       │
│  │  └───────────────┘ └───────────────────┘ └──────────────────┘  │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│           │                       │                       │                 │
│           ▼                       ▼                       ▼                 │
│  ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐       │
│  │  Redis Cache    │     │  PostgreSQL     │     │  Email/Push     │       │
│  │  (CouponCache)  │     │  (Repository)   │     │  (Notification) │       │
│  └─────────────────┘     └─────────────────┘     └─────────────────┘       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Core Components

### Controllers

| Controller | Path | Description |
|------------|------|-------------|
| `AdminCouponController` | `/api/v1/admin/coupons` | Admin operations: CRUD, analytics, bulk generation |
| `CouponController` | `/api/v1/coupons` | User operations: validate, apply, get available coupons |

### Services

| Service | Description |
|---------|-------------|
| `CouponServiceImpl` | Core coupon CRUD operations |
| `CouponValidationServiceImpl` | Validates coupons, checks eligibility, calculates discounts |
| `CouponNotificationServiceImpl` | Sends email and push notifications |
| `CouponCacheServiceImpl` | Redis-based caching for fast lookups |
| `BulkCouponGenerationService` | Generates multiple coupons from templates |

### Helpers

| Helper | Description |
|--------|-------------|
| `CouponCodeGenerator` | Generates unique alphanumeric coupon codes |
| `CouponMapper` | Maps between entities and DTOs |

---

## Features

### 1. Coupon Creation
- **Custom codes**: Admin can specify custom coupon codes (e.g., `SUMMER2024`)
- **Auto-generated codes**: System generates unique 8-character alphanumeric codes
- **Prefixed codes**: Codes with custom prefix (e.g., `SAVE20-A3B7C9D1`)

### 2. Discount Types
- **Percentage discount**: 1-100% off subscription price
- **Minimum purchase**: Optional minimum amount requirement

### 3. Usage Controls
- **Global usage limit**: Maximum total redemptions
- **Per-user limit**: Maximum redemptions per user
- **Validity period**: Start and end dates for coupon validity

### 4. Recipient Targeting
- Target specific user segments (see [Recipient Categories](#recipient-categories))
- Assign to specific users by ID

### 5. Bulk Generation
- Generate multiple coupons from a template
- Distribute to user segments automatically

### 6. Analytics & Tracking
- Track usage count, revenue impact
- View usage history per coupon
- Export analytics reports

---

## API Endpoints

### Admin Endpoints (`/api/v1/admin/coupons`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/` | Create a new coupon |
| `PUT` | `/{couponId}` | Update coupon details |
| `DELETE` | `/{couponId}` | Deactivate a coupon |
| `GET` | `/{couponId}` | Get coupon by ID |
| `GET` | `/` | List all coupons (paginated, filterable) |
| `GET` | `/{couponId}/usage` | Get coupon usage history |
| `GET` | `/{couponId}/analytics` | Get coupon analytics |
| `POST` | `/bulk` | Generate bulk coupons |

### User Endpoints (`/api/v1/coupons`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/validate/{code}` | Validate a coupon code |
| `POST` | `/apply` | Apply coupon and calculate discount |
| `GET` | `/{code}` | Get coupon details by code |
| `GET` | `/available` | Get user's available coupons |

---

## Data Models

### Coupon Entity

```java
@Entity
@Table(name = "coupons")
public class Coupon {
    String id;                      // UUID
    String code;                    // Unique coupon code
    String description;             // Coupon description
    BigDecimal discountPercentage;  // 1-100
    BigDecimal minPurchaseAmount;   // Optional minimum
    OffsetDateTime validFrom;       // Start date
    OffsetDateTime validUntil;      // End date
    int maxUsageCount;              // Global limit
    int currentUsageCount;          // Current usage
    int maxUsagePerUser;            // Per-user limit
    RecipientCategory recipientCategory;  // Target segment
    boolean isActive;               // Active status
    User createdBy;                 // Admin who created
    // ... timestamps, relationships
}
```

### CouponUsage Entity

```java
@Entity
@Table(name = "coupon_usages")
public class CouponUsage {
    String id;
    Coupon coupon;
    User user;
    BigDecimal originalAmount;
    BigDecimal discountAmount;
    BigDecimal finalAmount;
    String paymentReference;
    String subscriptionPlan;
    OffsetDateTime usedAt;
}
```

### CouponRecipient Entity

```java
@Entity
@Table(name = "coupon_recipients")
public class CouponRecipient {
    String id;
    Coupon coupon;
    User user;
    boolean isNotified;
    boolean isExpiryNotified;
    boolean isUsed;
    OffsetDateTime notifiedAt;
    OffsetDateTime usedAt;
}
```

---

## Recipient Categories

| Category | Description | Eligibility Criteria |
|----------|-------------|---------------------|
| `ALL_PAID_USERS` | All users with paid subscriptions | Active subscription, not FREE plan |
| `INDIVIDUAL_PLAN` | Individual plan subscribers | Plan name = INDIVIDUAL |
| `TEAM_PLAN` | Team plan subscribers | Plan name = TEAM |
| `ENTERPRISE_PLAN` | Enterprise plan subscribers | Plan name = ENTERPRISE |
| `FREE_TIER_ACTIVE` | Active free tier users | FREE plan with ≥20 OCR pages used |
| `EXPIRED_SUBSCRIPTION` | Recently expired subscriptions | Expired within 3 months |
| `NEW_USERS` | Newly registered users | Registered within 30 days |
| `HIGH_ACTIVITY_USERS` | Heavy platform users | >50% resource usage |
| `SPECIFIC_USERS` | Specific user list | User ID in recipient list |

---

## Caching Strategy

### Redis Cache Implementation

The coupon system uses Redis for high-performance caching:

```java
public interface CouponCacheService {
    Optional<CouponData> getCouponByCode(String code);
    void cacheCoupon(Coupon coupon);
    void cacheCouponData(String code, CouponData couponData);
    void invalidateCoupon(String code);
    void invalidateAll();
    void warmCache();
    boolean isCached(String code);
    CacheStats getCacheStats();
}
```

### Cache Keys
- Pattern: `coupon:{code}` (e.g., `coupon:SAVE20OFF`)
- TTL: Configurable, default 1 hour

### Cache Warming
- On application startup, active coupons are pre-loaded into cache
- Scheduled job refreshes cache periodically

---

## Notification System

### Notification Types

| Type | Description | Trigger |
|------|-------------|---------|
| `COUPON_RECEIVED` | New coupon assigned | Coupon creation/distribution |
| `COUPON_EXPIRING_7_DAYS` | Expiry reminder | 7 days before expiry |
| `COUPON_EXPIRING_3_DAYS` | Expiry reminder | 3 days before expiry |
| `COUPON_EXPIRING_1_DAY` | Expiry reminder | 1 day before expiry |
| `COUPON_EXPIRED` | Coupon has expired | After expiry date |
| `COUPON_APPLIED` | Coupon was used | After successful redemption |

### Email Templates

| Template | Purpose |
|----------|---------|
| `coupon-notification.html` | New coupon announcement |
| `coupon-expiration.html` | Expiry reminder |

### Notification Flow

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│  Coupon Event   │────▶│  NotificationService │────▶│  EmailOrchest.  │
└─────────────────┘     └──────────────────────┘     └─────────────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │  PushNotificationSvc │
                        └──────────────────────┘
```

---

## Scheduled Tasks

### CouponExpirationJob

Runs daily to handle expiring and expired coupons:

```java
@Scheduled(cron = "0 0 9 * * ?")  // 9 AM daily
public void processCouponExpirations() {
    // 1. Deactivate expired coupons
    // 2. Send 7-day expiry reminders
    // 3. Send 3-day expiry reminders
    // 4. Send 1-day expiry reminders
}
```

### Configurable Schedule

```properties
coupon.expiration.notification.days=7,3,1
coupon.expiration.check.cron=0 0 9 * * ?
```

---

## Testing

### Test Classes

| Test Class | Coverage |
|------------|----------|
| `CouponServiceImplTest` | Create, get, list operations |
| `CouponValidationServiceImplTest` | Validation, eligibility, discount calculation |
| `CouponCodeGeneratorTest` | Code generation, validation |

### Running Tests

```bash
# Run all coupon tests
mvn test -Dtest="com.extractor.unraveldocs.coupon.**"

# Run specific test class
mvn test -Dtest="CouponServiceImplTest"
```

### Test Patterns Used
- `@ExtendWith(MockitoExtension.class)` for unit tests
- `@Mock` for dependencies
- `@InjectMocks` for service under test
- `@Nested` for organized test structure
- `@DisplayName` for readable test names

---

## Configuration

### Application Properties

```yaml
# Coupon Configuration
coupon:
  code:
    length: 8
    prefix: ""
  cache:
    ttl: 3600  # seconds
    enabled: true
  expiration:
    notification:
      days: 7,3,1
    check:
      cron: "0 0 9 * * ?"
  validation:
    min-code-length: 6
    max-code-length: 50
```

### Redis Configuration

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}
```

---

## Error Codes

| Code | Description |
|------|-------------|
| `COUPON_NOT_FOUND` | Coupon code does not exist |
| `COUPON_INACTIVE` | Coupon is deactivated |
| `COUPON_EXPIRED` | Coupon past validity date |
| `COUPON_NOT_YET_VALID` | Coupon before validity date |
| `USAGE_LIMIT_REACHED` | Global usage limit exceeded |
| `PER_USER_LIMIT_REACHED` | User's usage limit exceeded |
| `USER_NOT_ELIGIBLE` | User doesn't match recipient category |
| `MIN_PURCHASE_NOT_MET` | Amount below minimum purchase |

---

## Best Practices

### Creating Coupons
1. Use descriptive codes for marketing campaigns (e.g., `BLACKFRIDAY20`)
2. Set reasonable usage limits to prevent abuse
3. Target specific user segments for personalized offers
4. Set validity periods to create urgency

### Monitoring
1. Review coupon analytics regularly
2. Monitor redemption rates
3. Track revenue impact
4. Identify popular/unused coupons

### Security
1. Coupon codes are case-insensitive
2. Rate limiting on validation endpoints
3. Admin-only access for creation/modification
4. Audit logging for all coupon operations

---

## Related Documentation

- [FEATURE_COUPON_SYSTEM.md](../FEATURE_COUPON_SYSTEM.md) - Feature overview and implementation status
- [Email Templates](../../resources/templates/) - Thymeleaf templates for notifications
- [API Documentation](swagger-ui.html) - Swagger/OpenAPI documentation

---

*Last Updated: January 2026*
