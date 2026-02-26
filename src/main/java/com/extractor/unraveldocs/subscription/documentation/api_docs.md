# Subscription Package — API Documentation

> **Base URLs:**
> - Public: `/api/v1/plans`
> - User: `/api/v1/subscriptions`
> - Admin: `/api/v1/admin/subscriptions`
>
> **Package:** `com.extractor.unraveldocs.subscription`  
> **Last Updated:** 2026-02-26

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Data Models](#data-models)
   - [Enums](#enums)
   - [Entities](#entities)
   - [Request DTOs](#request-dtos)
   - [Response DTOs](#response-dtos)
4. [Endpoints](#endpoints)
   - [Public — Get All Plans with Pricing](#1-get-all-plans-with-pricing-public)
   - [Public — Get Supported Currencies](#2-get-supported-currencies-public)
   - [User — Get My Subscription](#3-get-my-subscription)
   - [User — Activate Trial](#4-activate-trial)
   - [Admin — Create Subscription Plan](#5-create-subscription-plan-admin)
   - [Admin — Update Subscription Plan](#6-update-subscription-plan-admin)
   - [Admin — Assign Subscriptions to Existing Users](#7-assign-subscriptions-to-existing-users-admin)
5. [Service Layer](#service-layer)
   - [SubscriptionPlansService (Facade)](#subscriptionplansservice-facade)
   - [AddSubscriptionPlansImpl](#addsubscriptionplansimpl)
   - [UpdateSubscriptionPlanImpl](#updatesubscriptionplanimpl)
   - [AssignSubscriptionService](#assignsubscriptionservice)
   - [AssignSubscriptionToUsersImpl](#assignsubscriptiontousersimpl)
   - [UserSubscriptionServiceImpl](#usersubscriptionserviceimpl)
   - [PlanPricingServiceImpl](#planpricingserviceimpl)
   - [CurrencyConversionServiceImpl](#currencyconversionserviceimpl)
   - [SubscriptionFeatureService](#subscriptionfeatureservice)
6. [Repositories](#repositories)
7. [Scheduled Jobs](#scheduled-jobs)
8. [Currency Conversion System](#currency-conversion-system)
9. [Feature Access Control](#feature-access-control)
10. [Default Subscription Assignment Rules](#default-subscription-assignment-rules)
11. [Validation Rules](#validation-rules)
12. [Error Reference](#error-reference)
13. [Flow Diagrams](#flow-diagrams)

---

## Overview

The **Subscription** package manages the full lifecycle of subscription plans and user subscriptions in UnravelDocs. It is split across three access tiers:

| Access Tier | Base Path | Description |
|---|---|---|
| **Public** | `/api/v1/plans` | Pricing page endpoints — no auth required |
| **User** | `/api/v1/subscriptions` | Authenticated users view and activate their own subscriptions |
| **Admin** | `/api/v1/admin/subscriptions` | Admins and Super-Admins create and manage plan definitions |

Key capabilities:

| Feature | Description |
|---|---|
| Plan Management | Admins create and update `SubscriptionPlan` records with pricing, billing interval, and resource limits |
| Default Assignment | Every new user is automatically assigned the `FREE` plan on registration; Admins/Super-Admins get `BUSINESS_YEARLY`; Moderators get `PRO_YEARLY` |
| Public Pricing API | All active individual and team plans returned with real-time currency conversion (64 currencies supported) |
| Trial Activation | Users can activate a plan trial once; trial period set by the plan's `trialDays` field |
| Trial Expiry | Hourly cron reverts expired trials back to the FREE plan with push + email notifications |
| Monthly Quota Reset | Hourly cron resets `monthlyDocumentsUploaded`, `ocrPagesUsed`, and `aiOperationsUsed` when a subscription's `quotaResetDate` is reached |
| Exchange Rate Refresh | Daily cron at 06:00 fetches live exchange rates from an external API, with built-in fallback rates |
| Feature Gating | `SubscriptionFeatureService` checks whether a user's plan tier grants access to premium features |

---

## Package Structure

```
subscription/
├── config/
│   ├── CurrencyApiConfig.java                    # Externalized config for currency API base URL & API key
│   └── SubscriptionCurrencyDeserializer.java     # Jackson deserializer for SubscriptionCurrency enum
├── controller/
│   ├── PlanPricingController.java                # Public REST controller — pricing & currency endpoints
│   ├── SubscriptionController.java               # Admin REST controller — plan CRUD + bulk assignment
│   └── UserSubscriptionController.java           # User REST controller — view subscription, activate trial
├── datamodel/
│   ├── BillingIntervalUnit.java                  # Enum: MONTH | WEEK | YEAR
│   ├── SubscriptionCurrency.java                 # Enum: 64 supported currencies with symbol, name, code
│   ├── SubscriptionPlans.java                    # Enum: FREE | STARTER_MONTHLY | … | BUSINESS_YEARLY
│   └── SubscriptionStatus.java                   # Enum: ACTIVE | CANCELLED | TRIAL | EXPIRED
├── dto/
│   ├── ConvertedPrice.java                       # Price DTO with original USD amount + converted amount + rate metadata
│   ├── request/
│   │   ├── CreateSubscriptionPlanRequest.java    # Admin — create plan request
│   │   └── UpdateSubscriptionPlanRequest.java    # Admin — partial plan update request
│   └── response/
│       ├── AllPlansWithPricingResponse.java      # Pricing page response (individual + team plans + metadata)
│       ├── AllSubscriptionPlans.java             # Bulk-assignment result (assignedCount)
│       ├── CurrencyInfo.java                     # Currency code, symbol, full name
│       ├── IndividualPlanPricingDto.java          # Single individual plan with converted pricing + features
│       ├── PlanPricingResponse.java              # Generic pricing response wrapper
│       ├── SubscriptionPlansData.java            # Admin plan CRUD response payload
│       ├── SupportedCurrenciesResponse.java      # List of supported currencies + total count
│       ├── TeamPlanPricingDto.java               # Single team plan with converted pricing + features
│       └── UserSubscriptionDetailsDto.java       # Full user subscription details (status, usage, trial, billing)
├── impl/
│   ├── AddSubscriptionPlansImpl.java             # Create subscription plan logic
│   ├── AssignSubscriptionService.java            # Assigns default plan to a single user based on role
│   ├── AssignSubscriptionToUsersImpl.java        # Bulk-assigns default plans to users without one
│   ├── UpdateSubscriptionPlanImpl.java           # Update subscription plan logic
│   └── UserSubscriptionServiceImpl.java          # User subscription view, trial activation & expiry
├── interfaces/
│   ├── AddSubscriptionPlansService.java          # Contract for plan creation
│   ├── AssignSubscriptionToUsersService.java     # Contract for bulk assignment
│   └── UpdateSubscriptionPlanService.java        # Contract for plan update
├── jobs/
│   ├── ExchangeRateUpdateJob.java                # Cron: refresh exchange rates daily at 06:00
│   ├── MonthlyQuotaResetJob.java                 # Cron: reset monthly quotas hourly; init reset dates daily at 00:30
│   └── TrialExpiryJob.java                       # Cron: check and expire trials hourly
├── model/
│   ├── Discount.java                             # JPA entity — discount record linked to a SubscriptionPlan
│   ├── SubscriptionPlan.java                     # JPA entity — plan definition (pricing, limits, billing)
│   └── UserSubscription.java                     # JPA entity — per-user subscription state and usage counters
├── repository/
│   ├── DiscountRepository.java                   # JPA repository for Discount
│   ├── SubscriptionPlanRepository.java           # JPA repository for SubscriptionPlan
│   └── UserSubscriptionRepository.java           # JPA repository for UserSubscription (rich custom queries)
├── service/
│   ├── CurrencyConversionService.java            # Contract: convert(BigDecimal, SubscriptionCurrency) + refreshRates()
│   ├── CurrencyConversionServiceImpl.java        # Live + fallback currency conversion with in-memory rate cache
│   ├── PlanPricingService.java                   # Contract: getAllPlansWithPricing() + getSupportedCurrencies()
│   ├── PlanPricingServiceImpl.java               # Assembles pricing page response with currency conversion
│   ├── SubscriptionFeatureService.java           # Premium feature gating by subscription tier
│   ├── SubscriptionPlansService.java             # Facade — delegates admin plan operations
│   └── UserSubscriptionService.java              # Contract: getUserSubscriptionDetails, activateTrial, checkAndExpireTrials, validateSubscriptionEligibility
└── documentation/
    └── api_docs.md                               # This file
```

---

## Data Models

### Enums

#### `SubscriptionPlans`
**Package:** `com.extractor.unraveldocs.subscription.datamodel`

Defines all plan identifiers stored in `SubscriptionPlan.name`.

| Value | Display Name | Description |
|---|---|---|
| `FREE` | `"Free"` | Default plan assigned to all new regular users |
| `STARTER_MONTHLY` | `"Starter_Monthly"` | Starter tier, billed monthly |
| `STARTER_YEARLY` | `"Starter_Yearly"` | Starter tier, billed annually |
| `PRO_MONTHLY` | `"Pro_Monthly"` | Pro tier, billed monthly |
| `PRO_YEARLY` | `"Pro_Yearly"` | Pro tier, billed annually; default for Moderators |
| `BUSINESS_MONTHLY` | `"Business_Monthly"` | Business tier, billed monthly |
| `BUSINESS_YEARLY` | `"Business_Yearly"` | Business tier, billed annually; default for Admins/Super-Admins |

**Helper:** `fromString(String planName)` — case-insensitive lookup; throws `IllegalArgumentException` if not found.

---

#### `SubscriptionStatus`
**Package:** `com.extractor.unraveldocs.subscription.datamodel`

| Value | Display Name | Description |
|---|---|---|
| `ACTIVE` | `"Active"` | Subscription is live and in good standing |
| `CANCELLED` | `"Cancelled"` | Subscription cancelled by the user |
| `TRIAL` | `"Trial"` | User is within a free trial period |
| `EXPIRED` | `"Expired"` | Billing period ended without renewal |

**Helper:** `fromString(String statusName)` — case-insensitive lookup.

---

#### `BillingIntervalUnit`
**Package:** `com.extractor.unraveldocs.subscription.datamodel`

| Value | String Value | Description |
|---|---|---|
| `MONTH` | `"month"` | Monthly billing cycle |
| `WEEK` | `"week"` | Weekly billing cycle |
| `YEAR` | `"year"` | Annual billing cycle |

**Helper:** `fromValue(String value)` — case-insensitive lookup.

---

#### `SubscriptionCurrency`
**Package:** `com.extractor.unraveldocs.subscription.datamodel`

64 global currencies are supported. Each entry carries a `symbol`, `fullName`, and ISO `code`.

Selected currencies:

| Code          | Symbol | Full Name                                      |
|---------------|--------|------------------------------------------------|
| `USD`         | `$`    | United States Dollar                           |
| `EUR`         | `€`    | Euro                                           |
| `GBP`         | `£`    | British Pound Sterling                         |
| `NGN`         | `₦`    | Nigerian Naira                                 |
| `GHS`         | `GH₵`  | Ghanaian Cedi                                  |
| `KES`         | `KSh`  | Kenyan Shilling                                |
| `ZAR`         | `R`    | South African Rand                             |

**Helpers:**
- `fromIdentifier(String)` — matches by `code`, `fullName`, or `name()` (case-insensitive)
- `isValidCurrency(SubscriptionCurrency)` — validates a currency value
- `getAllValidCurrencies()` — returns `String[]` of `"CODE(Full Name)"` entries

---

### Entities

#### `SubscriptionPlan`
**Package:** `com.extractor.unraveldocs.subscription.model`  
**Table:** `subscription_plans`

| Column                   | Type                          | Nullable | Description                                                            |
|--------------------------|-------------------------------|----------|------------------------------------------------------------------------|
| `id`                     | `String` (UUID)               | No       | Primary key                                                            |
| `name`                   | `SubscriptionPlans` (enum)    | No       | Unique plan identifier                                                 |
| `price`                  | `BigDecimal` (DECIMAL 10,2)   | No       | Base price stored in USD                                               |
| `currency`               | `SubscriptionCurrency` (enum) | No       | The base currency for the price (typically USD)                        |
| `billing_interval_unit`  | `BillingIntervalUnit` (enum)  | No       | Unit of billing cadence                                                |
| `billing_interval_value` | `Integer`                     | No       | Number of units per billing cycle (e.g., `1` = monthly, `12` = yearly) |
| `document_upload_limit`  | `Integer`                     | No       | Max documents uploadable per billing period                            |
| `ocr_page_limit`         | `Integer`                     | No       | Max OCR pages processable per billing period                           |
| `is_active`              | `boolean`                     | No       | Whether this plan appears in public listings                           |
| `paystack_plan_code`     | `String`                      | Yes      | Paystack payment gateway plan code                                     |
| `stripe_price_id`        | `String`                      | Yes      | Stripe price ID                                                        |
| `paypal_plan_code`       | `String`                      | Yes      | PayPal plan code                                                       |
| `storage_limit`          | `Long`                        | No       | Maximum storage in bytes                                               |
| `trial_days`             | `Integer`                     | No       | Default `10`; days of free trial offered by this plan                  |
| `ai_operations_limit`    | `Integer`                     | No       | Default `0`; monthly AI operation allowance                            |
| `created_at`             | `OffsetDateTime`              | No       | Auto-set on insert                                                     |
| `updated_at`             | `OffsetDateTime`              | No       | Auto-set on update                                                     |

**Relationships:**
- `discounts` — `@OneToMany` → `Discount`; lazy-loaded

---

#### `UserSubscription`
**Package:** `com.extractor.unraveldocs.subscription.model`  
**Table:** `user_subscriptions`

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `String` (UUID) | No | Primary key |
| `user_id` | FK → `User` | No | One-to-one user relationship (lazy) |
| `plan_id` | FK → `SubscriptionPlan` | No | Many-to-one plan reference (lazy) |
| `payment_gateway_subscription_id` | `String` | Yes | External subscription reference (Paystack / Stripe / PayPal) |
| `status` | `String` | No | `"ACTIVE"`, `"TRIAL"`, `"CANCELLED"`, `"EXPIRED"` |
| `current_period_start` | `OffsetDateTime` | Yes | Start of the current billing period |
| `current_period_end` | `OffsetDateTime` | Yes | End of the current billing period |
| `auto_renew` | `boolean` | No | Default `false` |
| `trial_ends_at` | `OffsetDateTime` | Yes | When the trial expires; `null` if not on trial |
| `has_used_trial` | `boolean` | No | Default `false`; prevents second trial |
| `storage_used` | `Long` | No | Cumulative storage used in bytes (NOT reset monthly) |
| `ocr_pages_used` | `Integer` | No | OCR pages used in the current billing period (reset monthly) |
| `monthly_documents_uploaded` | `Integer` | No | Documents uploaded in the current billing period (reset monthly) |
| `ai_operations_used` | `Integer` | No | AI operations used in the current billing period (reset monthly) |
| `quota_reset_date` | `OffsetDateTime` | Yes | Next date when monthly quotas will be reset |
| `created_at` | `OffsetDateTime` | No | Auto-set on insert |
| `updated_at` | `OffsetDateTime` | No | Auto-set on update |

---

#### `Discount`
**Package:** `com.extractor.unraveldocs.subscription.model`  
**Table:** `discounts`

| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `String` (UUID) | No | Primary key |
| `name` | `String` | No | Human-readable discount name |
| `plan_id` | FK → `SubscriptionPlan` | No | Plan this discount applies to (many-to-one, lazy) |
| `discount_percent` | `BigDecimal` (DECIMAL 5,2) | No | Percentage reduction (e.g., `20.00` = 20% off) |
| `start_date` | `OffsetDateTime` | No | When the discount becomes active |
| `valid_until` | `OffsetDateTime` | No | When the discount expires |
| `is_active` | `boolean` | No | Whether the discount is currently applicable |
| `created_at` | `OffsetDateTime` | No | Auto-set on insert |
| `updated_at` | `OffsetDateTime` | No | Auto-set on update |

---

### Request DTOs

#### `CreateSubscriptionPlanRequest`
**Package:** `com.extractor.unraveldocs.subscription.dto.request`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `SubscriptionPlans` | ✅ | Not null | Plan identifier enum value |
| `price` | `BigDecimal` | ✅ | Not null; ≥ 0 | Base price in USD |
| `currency` | `SubscriptionCurrency` | ✅ | Not null | Base currency (typically `USD`) |
| `billingIntervalUnit` | `BillingIntervalUnit` | ✅ | Not null | `MONTH`, `WEEK`, or `YEAR` |
| `billingIntervalValue` | `Integer` | ✅ | Not null; ≥ 1 | Number of interval units per cycle |
| `documentUploadLimit` | `Integer` | ✅ | Not null | Document upload cap per billing period |
| `ocrPageLimit` | `Integer` | ✅ | Not null | OCR page cap per billing period |
| `trialDays` | `Integer` | ❌ | ≥ 0 | Days of free trial; defaults to `10` if omitted |

---

#### `UpdateSubscriptionPlanRequest`
**Package:** `com.extractor.unraveldocs.subscription.dto.request`

All fields are **optional** — only provided fields are applied.

| Field | Type | Required | Description |
|---|---|---|---|
| `newPlanPrice` | `BigDecimal` | ❌ | New price in USD |
| `newPlanCurrency` | `SubscriptionCurrency` | ❌ | New base currency (validated against enum; uses `SubscriptionCurrencyDeserializer`) |
| `billingIntervalUnit` | `BillingIntervalUnit` | ❌ | New billing interval unit |
| `billingIntervalValue` | `Integer` | ❌ | New billing interval value |
| `documentUploadLimit` | `Integer` | ❌ | New document upload cap |
| `ocrPageLimit` | `Integer` | ❌ | New OCR page cap |

---

### Response DTOs

#### `SubscriptionPlansData`
Returned inside `UnravelDocsResponse<SubscriptionPlansData>` for admin create/update operations.

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Plan UUID |
| `planName` | `SubscriptionPlans` | Plan identifier enum |
| `planPrice` | `BigDecimal` | Base price in USD |
| `planCurrency` | `SubscriptionCurrency` | Base currency |
| `billingIntervalUnit` | `BillingIntervalUnit` | Billing interval unit |
| `billingIntervalValue` | `Integer` | Billing interval value |
| `documentUploadLimit` | `Integer` | Document upload cap |
| `ocrPageLimit` | `Integer` | OCR page cap |
| `isActive` | `boolean` | Plan active status |
| `createdAt` | `OffsetDateTime` | Creation timestamp |
| `updatedAt` | `OffsetDateTime` | Last update timestamp |

---

#### `UserSubscriptionDetailsDto`
Returned inside `UnravelDocsResponse<UserSubscriptionDetailsDto>`. Null fields are omitted from the JSON (`@JsonInclude(NON_NULL)`).

| Field | Type | Description |
|---|---|---|
| `subscriptionId` | `String` | Subscription UUID |
| `status` | `String` | `"ACTIVE"`, `"TRIAL"`, `"CANCELLED"`, `"EXPIRED"`, or `"none"` |
| `planId` | `String` | Plan UUID |
| `planName` | `String` | Raw enum name (e.g., `"PRO_MONTHLY"`) |
| `planDisplayName` | `String` | Human-readable name (e.g., `"Pro Monthly"`) |
| `planPrice` | `BigDecimal` | Plan base price |
| `currency` | `String` | Currency code |
| `billingInterval` | `String` | `"monthly"`, `"yearly"`, `"weekly"`, or `"N units"` |
| `currentPeriodStart` | `OffsetDateTime` | Billing period start |
| `currentPeriodEnd` | `OffsetDateTime` | Billing period end |
| `autoRenew` | `Boolean` | Auto-renewal status |
| `isOnTrial` | `Boolean` | Whether `trialEndsAt` is in the future |
| `trialEndsAt` | `OffsetDateTime` | Trial expiry timestamp |
| `hasUsedTrial` | `Boolean` | Whether trial was ever activated |
| `trialDaysRemaining` | `Integer` | Days remaining in trial (0 if expired, null if never trialled) |
| `storageLimit` | `Long` | Max storage in bytes |
| `storageUsed` | `Long` | Cumulative storage consumed |
| `documentUploadLimit` | `Integer` | Monthly document upload cap |
| `documentsUploaded` | `Integer` | Documents uploaded this period |
| `ocrPageLimit` | `Integer` | Monthly OCR page cap |
| `ocrPagesUsed` | `Integer` | OCR pages consumed this period |
| `paymentGatewaySubscriptionId` | `String` | External gateway reference |
| `createdAt` | `OffsetDateTime` | Subscription creation timestamp |
| `updatedAt` | `OffsetDateTime` | Last update timestamp |

---

#### `AllPlansWithPricingResponse`

| Field | Type | Description |
|---|---|---|
| `individualPlans` | `List<IndividualPlanPricingDto>` | All active individual subscription plans with converted pricing |
| `teamPlans` | `List<TeamPlanPricingDto>` | All active team subscription plans with converted pricing |
| `displayCurrency` | `SubscriptionCurrency` | The currency prices are displayed in |
| `exchangeRateTimestamp` | `OffsetDateTime` | When the exchange rate data was last refreshed |

---

#### `IndividualPlanPricingDto`

| Field | Type | Description |
|---|---|---|
| `planId` | `String` | Plan UUID |
| `planName` | `String` | Raw enum name |
| `displayName` | `String` | Formatted display name (e.g., `"Pro Monthly"`) |
| `billingInterval` | `String` | `"MONTH"` or `"YEAR"` |
| `price` | `ConvertedPrice` | Pricing object with original USD amount + converted amount + formatted string + exchange rate + rate timestamp |
| `documentUploadLimit` | `Integer` | Document upload cap |
| `ocrPageLimit` | `Integer` | OCR page cap |
| `isActive` | `boolean` | Plan active status |
| `features` | `List<String>` | Derived feature list based on plan tier |

---

#### `ConvertedPrice`

| Field | Type | Description |
|---|---|---|
| `originalAmountUsd` | `BigDecimal` | The price in USD before conversion |
| `convertedAmount` | `BigDecimal` | The price in the target currency (rounded to 2 d.p.) |
| `currency` | `SubscriptionCurrency` | Target currency |
| `formattedPrice` | `String` | Locale-formatted price string (e.g., `"₦45,000.00"`) |
| `exchangeRate` | `BigDecimal` | Rate used: 1 USD = X target currency |
| `rateTimestamp` | `OffsetDateTime` | When this exchange rate was last refreshed |

---

#### `SupportedCurrenciesResponse`

| Field | Type | Description |
|---|---|---|
| `currencies` | `List<CurrencyInfo>` | All 64 supported currencies |
| `totalCount` | `int` | Total number of currencies returned |

**`CurrencyInfo`:**

| Field | Type | Description |
|---|---|---|
| `code` | `String` | ISO code (e.g., `"NGN"`) |
| `symbol` | `String` | Currency symbol (e.g., `"₦"`) |
| `name` | `String` | Full name (e.g., `"Nigerian Naira"`) |

---

## Endpoints

### 1. Get All Plans with Pricing (Public)

| Property          | Value           |
|-------------------|-----------------|
| **Method**        | `GET`           |
| **Path**          | `/api/v1/plans` |
| **Auth Required** | No              |

**Query Parameters**

| Parameter  | Type     | Required | Default | Description                                |
|------------|----------|----------|---------|--------------------------------------------|
| `currency` | `String` | ❌        | `"USD"` | ISO currency code (e.g., `"NGN"`, `"EUR"`) |

**Success Response — `200 OK`**

```json
{
    "individualPlans": [
        {
            "planId": "2db26e3f-8311-49c3-a05a-4e3f2e690175",
            "planName": "FREE",
            "displayName": "Free",
            "billingInterval": "MONTH",
            "price": {
                "originalAmountUsd": 0.00,
                "convertedAmount": 0.00,
                "currency": "NGN",
                "formattedPrice": "₦0.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
            },
            "documentUploadLimit": 5,
            "ocrPageLimit": 25,
            "features": [
                "Basic document processing",
                "Limited OCR pages",
                "Email support"
            ],
            "active": true
        },
        {
            "planId": "53ee631a-429a-47f1-85e7-4958728132db",
            "planName": "STARTER_MONTHLY",
            "displayName": "Starter Monthly",
            "billingInterval": "MONTH",
            "price": {
                "originalAmountUsd": 9.00,
                "convertedAmount": 13950.00,
                "currency": "NGN",
                "formattedPrice": "₦13,950.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
            "planId": "cb3ac114-16cf-4879-ae92-499470f5089e",
            "planName": "STARTER_YEARLY",
            "displayName": "Starter Yearly",
            "billingInterval": "YEAR",
            "price": {
                "originalAmountUsd": 90.00,
                "convertedAmount": 139500.00,
                "currency": "NGN",
                "formattedPrice": "₦139,500.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
            "planId": "674c742b-b8bf-46ac-96bf-f969473b2af1",
            "planName": "PRO_MONTHLY",
            "displayName": "Pro Monthly",
            "billingInterval": "MONTH",
            "price": {
                "originalAmountUsd": 19.00,
                "convertedAmount": 29450.00,
                "currency": "NGN",
                "formattedPrice": "₦29,450.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
            "planId": "5b40f60d-0436-4dd9-9d44-6005566f2519",
            "planName": "PRO_YEARLY",
            "displayName": "Pro Yearly",
            "billingInterval": "YEAR",
            "price": {
                "originalAmountUsd": 190.00,
                "convertedAmount": 294500.00,
                "currency": "NGN",
                "formattedPrice": "₦294,500.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
            "planId": "1356641e-72fc-4254-9266-10816c522195",
            "planName": "BUSINESS_MONTHLY",
            "displayName": "Business Monthly",
            "billingInterval": "MONTH",
            "price": {
                "originalAmountUsd": 49.00,
                "convertedAmount": 75950.00,
                "currency": "NGN",
                "formattedPrice": "₦75,950.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
            "planId": "02ca4bbd-01a4-4ba0-9f92-00b4c8ebc6c9",
            "planName": "BUSINESS_YEARLY",
            "displayName": "Business Yearly",
            "billingInterval": "YEAR",
            "price": {
                "originalAmountUsd": 490.00,
                "convertedAmount": 759500.00,
                "currency": "NGN",
                "formattedPrice": "₦759,500.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
            "planId": "183955fa-85a8-4f25-9e69-9da526269484",
            "planName": "TEAM_PREMIUM",
            "displayName": "Team Premium",
            "description": "Perfect for small teams. Includes 200 documents per month with up to 10 members.",
            "monthlyPrice": {
                "originalAmountUsd": 29.00,
                "convertedAmount": 44950.00,
                "currency": "NGN",
                "formattedPrice": "₦44,950.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
            },
            "yearlyPrice": {
                "originalAmountUsd": 290.00,
                "convertedAmount": 449500.00,
                "currency": "NGN",
                "formattedPrice": "₦449,500.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
            "planId": "11a4a019-2629-4c43-bb50-7ea2ea5e4753",
            "planName": "TEAM_ENTERPRISE",
            "displayName": "Team Enterprise",
            "description": "For larger teams that need unlimited documents, admin roles, and email invitations.",
            "monthlyPrice": {
                "originalAmountUsd": 79.00,
                "convertedAmount": 122450.00,
                "currency": "NGN",
                "formattedPrice": "₦122,450.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
            },
            "yearlyPrice": {
                "originalAmountUsd": 790.00,
                "convertedAmount": 1224500.00,
                "currency": "NGN",
                "formattedPrice": "₦1,224,500.00",
                "exchangeRate": 1550.00,
                "rateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
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
    "exchangeRateTimestamp": "2026-02-26T03:47:53.7202577+01:00"
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Invalid currency code — includes hint to call `/api/v1/plans/currencies` |

---

### 2. Get Supported Currencies (Public)

| Property | Value |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/v1/plans/currencies` |
| **Auth Required** | No |

**Success Response — `200 OK`**

```json
{
   "currencies": [
      {
         "code": "USD",
         "symbol": "$",
         "name": "United States Dollar"
      },
      {
         "code": "EUR",
         "symbol": "€",
         "name": "Euro"
      },
      {
         "code": "GBP",
         "symbol": "£",
         "name": "British Pound Sterling"
      },
      {
         "code": "INR",
         "symbol": "₹",
         "name": "Indian Rupee"
      },
      {
         "code": "JPY",
         "symbol": "¥",
         "name": "Japanese Yen"
      },
      {
         "code": "AUD",
         "symbol": "A$",
         "name": "Australian Dollar"
      },
      {
         "code": "CAD",
         "symbol": "C$",
         "name": "Canadian Dollar"
      },
      {
         "code": "CNY",
         "symbol": "¥",
         "name": "Chinese Yuan Renminbi"
      },
      {
         "code": "RUB",
         "symbol": "₽",
         "name": "Russian Ruble"
      },
      {
         "code": "BRL",
         "symbol": "R$",
         "name": "Brazilian Real"
      },
      {
         "code": "NGN",
         "symbol": "₦",
         "name": "Nigerian Naira"
      },
      {
         "code": "ZAR",
         "symbol": "R",
         "name": "South African Rand"
      },
      {
         "code": "MXN",
         "symbol": "$",
         "name": "Mexican Peso"
      },
      {
         "code": "KRW",
         "symbol": "₩",
         "name": "South Korean Won"
      },
      {
         "code": "CHF",
         "symbol": "CHF",
         "name": "Swiss Franc"
      },
      {
         "code": "SEK",
         "symbol": "kr",
         "name": "Swedish Krona"
      },
      {
         "code": "NZD",
         "symbol": "$",
         "name": "New Zealand Dollar"
      },
      {
         "code": "AED",
         "symbol": "د.إ",
         "name": "United Arab Emirates Dirham"
      },
      {
         "code": "SGD",
         "symbol": "$",
         "name": "Singapore Dollar"
      },
      {
         "code": "HKD",
         "symbol": "$",
         "name": "Hong Kong Dollar"
      },
      {
         "code": "TRY",
         "symbol": "₺",
         "name": "Turkish Lira"
      },
      {
         "code": "PLN",
         "symbol": "zł",
         "name": "Polish Zloty"
      },
      {
         "code": "NOK",
         "symbol": "kr",
         "name": "Norwegian Krone"
      },
      {
         "code": "DKK",
         "symbol": "kr",
         "name": "Danish Krone"
      },
      {
         "code": "THB",
         "symbol": "฿",
         "name": "Thai Baht"
      },
      {
         "code": "IDR",
         "symbol": "Rp",
         "name": "Indonesian Rupiah"
      },
      {
         "code": "MYR",
         "symbol": "RM",
         "name": "Malaysian Ringgit"
      },
      {
         "code": "PHP",
         "symbol": "₱",
         "name": "Philippine Peso"
      },
      {
         "code": "VND",
         "symbol": "₫",
         "name": "Vietnamese Dong"
      },
      {
         "code": "ARS",
         "symbol": "$",
         "name": "Argentine Peso"
      },
      {
         "code": "CLP",
         "symbol": "$",
         "name": "Chilean Peso"
      },
      {
         "code": "COP",
         "symbol": "$",
         "name": "Colombian Peso"
      },
      {
         "code": "PEN",
         "symbol": "S/",
         "name": "Peruvian Sol"
      },
      {
         "code": "ILS",
         "symbol": "₪",
         "name": "Israeli New Shekel"
      },
      {
         "code": "KZT",
         "symbol": "₸",
         "name": "Kazakhstani Tenge"
      },
      {
         "code": "UAH",
         "symbol": "₴",
         "name": "Ukrainian Hryvnia"
      },
      {
         "code": "RON",
         "symbol": "lei",
         "name": "Romanian Leu"
      },
      {
         "code": "HUF",
         "symbol": "Ft",
         "name": "Hungarian Forint"
      },
      {
         "code": "CZK",
         "symbol": "Kč",
         "name": "Czech Koruna"
      },
      {
         "code": "BGN",
         "symbol": "лв.",
         "name": "Bulgarian Lev"
      },
      {
         "code": "HRK",
         "symbol": "kn",
         "name": "Croatian Kuna"
      },
      {
         "code": "ISK",
         "symbol": "kr",
         "name": "Icelandic Króna"
      },
      {
         "code": "LTL",
         "symbol": "Lt",
         "name": "Lithuanian Litas"
      },
      {
         "code": "LVL",
         "symbol": "Ls",
         "name": "Latvian Lats"
      },
      {
         "code": "EGP",
         "symbol": "ج.م",
         "name": "Egyptian Pound"
      },
      {
         "code": "PKR",
         "symbol": "₨",
         "name": "Pakistani Rupee"
      },
      {
         "code": "TWD",
         "symbol": "NT$",
         "name": "New Taiwan Dollar"
      },
      {
         "code": "ZMW",
         "symbol": "ZK",
         "name": "Zambian Kwacha"
      },
      {
         "code": "KES",
         "symbol": "KSh",
         "name": "Kenyan Shilling"
      },
      {
         "code": "GHS",
         "symbol": "GH₵",
         "name": "Ghanaian Cedi"
      },
      {
         "code": "MAD",
         "symbol": "د.م.",
         "name": "Moroccan Dirham"
      },
      {
         "code": "JMD",
         "symbol": "J$",
         "name": "Jamaican Dollar"
      },
      {
         "code": "BDT",
         "symbol": "৳",
         "name": "Bangladeshi Taka"
      },
      {
         "code": "LKR",
         "symbol": "Rs",
         "name": "Sri Lankan Rupee"
      },
      {
         "code": "MUR",
         "symbol": "₨",
         "name": "Mauritian Rupee"
      },
      {
         "code": "TND",
         "symbol": "د.ت",
         "name": "Tunisian Dinar"
      },
      {
         "code": "OMR",
         "symbol": "ر.ع.",
         "name": "Omani Rial"
      },
      {
         "code": "QAR",
         "symbol": "ر.ق",
         "name": "Qatari Riyal"
      },
      {
         "code": "KWD",
         "symbol": "د.ك",
         "name": "Kuwaiti Dinar"
      },
      {
         "code": "BHD",
         "symbol": "د.ب",
         "name": "Bahraini Dinar"
      },
      {
         "code": "JOD",
         "symbol": "د.أ",
         "name": "Jordanian Dinar"
      }
   ],
   "totalCount": 61
}
```

---

### 3. Get My Subscription

| Property | Value |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/v1/subscriptions/me` |
| **Auth Required** | Yes — `@CurrentUser` injects the authenticated `User` |

**Success Response — `200 OK`**

```json
{
   "statusCode": 200,
   "status": "success",
   "message": "Subscription details retrieved successfully",
   "data": {
      "subscriptionId": "f40366d3-f1fb-4de7-a928-5be2315942d1",
      "status": "ACTIVE",
      "planId": "2db26e3f-8311-49c3-a05a-4e3f2e690175",
      "planName": "FREE",
      "planDisplayName": "Free",
      "planPrice": 0.00,
      "currency": "USD",
      "billingInterval": "monthly",
      "currentPeriodStart": "2026-02-16T15:00:00.258935Z",
      "autoRenew": false,
      "isOnTrial": false,
      "hasUsedTrial": true,
      "storageLimit": 125829120,
      "storageUsed": 3046324,
      "documentUploadLimit": 5,
      "documentsUploaded": 4,
      "ocrPageLimit": 25,
      "ocrPagesUsed": 2,
      "createdAt": "2026-02-06T13:49:04.761758Z",
      "updatedAt": "2026-02-20T15:56:06.311647Z"
   }
}
```

> If the user has no subscription record, a minimal "Free Plan" response is returned with `status: "none"` and `isOnTrial: false` — no error is thrown.

**Error Responses**

| Status | Condition |
|---|---|
| `401 Unauthorized` | Not authenticated |

---

### 4. Activate Trial

| Property | Value |
|---|---|
| **Method** | `POST` |
| **Path** | `/api/v1/subscriptions/trial/{planId}` |
| **Auth Required** | Yes — `@CurrentUser` |

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `planId` | `String` | UUID of the `SubscriptionPlan` to trial |

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Trial activated successfully",
  "data": null
}
```

**Side Effects**
- Subscription status set to `"TRIAL"`.
- `trialEndsAt = now + plan.trialDays`.
- `hasUsedTrial` set to `true` (cannot trial again).
- Monthly usage counters (`storageUsed`, `ocrPagesUsed`, `monthlyDocumentsUploaded`) reset to zero.
- Push notification `TRIAL_ACTIVATED` sent to user.
- Email `sendTrialActivatedEmail(...)` dispatched via `UserEmailTemplateService`.

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Plan does not support trials (`trialDays` is 0 or null) |
| `400 Bad Request` | User has already used their trial (`hasUsedTrial == true`) |
| `400 Bad Request` | User already has an active paid subscription |
| `401 Unauthorized` | Not authenticated |
| `404 Not Found` | `planId` does not match any plan |

---

### 5. Create Subscription Plan (Admin)

| Property | Value |
|---|---|
| **Method** | `POST` |
| **Path** | `/api/v1/admin/subscriptions/plans` |
| **Auth Required** | Yes — `ROLE_ADMIN` or `ROLE_SUPER_ADMIN` |

**Request Body:** `CreateSubscriptionPlanRequest`

```json
{
  "name": "STARTER_MONTHLY",
  "price": 9.99,
  "currency": "USD",
  "billingIntervalUnit": "MONTH",
  "billingIntervalValue": 1,
  "documentUploadLimit": 50,
  "ocrPageLimit": 200,
  "trialDays": 14
}
```

**Success Response — `201 Created`**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "Subscription plan created successfully.",
  "data": {
    "id": "plan-uuid",
    "planName": "STARTER_MONTHLY",
    "planPrice": 9.99,
    "planCurrency": "USD",
    "billingIntervalUnit": "MONTH",
    "billingIntervalValue": 1,
    "documentUploadLimit": 50,
    "ocrPageLimit": 200,
    "isActive": true,
    "createdAt": "2026-02-26T10:00:00Z",
    "updatedAt": "2026-02-26T10:00:00Z"
  }
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | A plan with the same `name` already exists |
| `403 Forbidden` | User is not an Admin or Super-Admin |

---

### 6. Update Subscription Plan (Admin)

| Property | Value |
|---|---|
| **Method** | `PUT` |
| **Path** | `/api/v1/admin/subscriptions/plans/{planId}` |
| **Auth Required** | Yes — `ROLE_ADMIN` or `ROLE_SUPER_ADMIN` |
| **Content-Type** | `application/json` |

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `planId` | `String` | UUID of the plan to update |

**Request Body:** `UpdateSubscriptionPlanRequest`

```json
{
  "newPlanPrice": 12.99,
  "newPlanCurrency": "USD",
  "billingIntervalUnit": "MONTH",
  "billingIntervalValue": 1,
  "documentUploadLimit": 75,
  "ocrPageLimit": 300
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Subscription plan updated successfully.",
  "data": {
    "id": "plan-uuid",
    "planName": "STARTER_MONTHLY",
    "planPrice": 12.99,
    "planCurrency": "USD",
    "billingIntervalUnit": "MONTH",
    "billingIntervalValue": 1,
    "documentUploadLimit": 75,
    "ocrPageLimit": 300,
    "isActive": true,
    "createdAt": "2026-02-26T10:00:00Z",
    "updatedAt": "2026-02-27T15:30:00Z"
  }
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | `newPlanCurrency` is not a valid `SubscriptionCurrency` value |
| `403 Forbidden` | User is not an Admin or Super-Admin |
| `404 Not Found` | `planId` does not match any plan |

---

### 7. Assign Subscriptions to Existing Users (Admin)

| Property | Value |
|---|---|
| **Method** | `POST` |
| **Path** | `/api/v1/admin/subscriptions/assign-subscriptions-to-existing-users` |
| **Auth Required** | Yes — `ROLE_ADMIN` or `ROLE_SUPER_ADMIN` |

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Successfully assigned subscriptions to 42 users.",
  "data": {
    "assignedCount": 42
  }
}
```

**Logic:** Finds all `User` records with `subscription IS NULL` and assigns each the appropriate default plan based on their `Role` (see [Default Subscription Assignment Rules](#default-subscription-assignment-rules)). Skips users whose default plan is not yet created in the database and logs a warning.

**Error Responses**

| Status | Condition |
|---|---|
| `403 Forbidden` | User is not an Admin or Super-Admin |

---

## Service Layer

### `SubscriptionPlansService` (Facade)
**Package:** `com.extractor.unraveldocs.subscription.service`

Thin admin-facing facade — delegates to interface implementations.

| Method | Delegates To | Description |
|---|---|---|
| `createSubscriptionPlan(CreateSubscriptionPlanRequest)` | `AddSubscriptionPlansService` | Create a new plan |
| `updateSubscriptionPlan(String planId, UpdateSubscriptionPlanRequest)` | `UpdateSubscriptionPlanService` | Patch an existing plan |
| `assignSubscriptionsToExistingUsers()` | `AssignSubscriptionToUsersService` | Bulk-assign default plans |

---

### `AddSubscriptionPlansImpl`
**Package:** `com.extractor.unraveldocs.subscription.impl`  
**Implements:** `AddSubscriptionPlansService`  
**Transactional:** Yes

**Logic:**
```
1. findByName(request.name) → BadRequestException if plan name already exists
2. Build SubscriptionPlan entity (isActive = true, trialDays defaults to 10 if not provided)
3. save(newPlan)
4. Map SubscriptionPlan → SubscriptionPlansData
5. Return HTTP 201
```

---

### `UpdateSubscriptionPlanImpl`
**Package:** `com.extractor.unraveldocs.subscription.impl`  
**Implements:** `UpdateSubscriptionPlanService`  
**Transactional:** Yes

**Logic:**
```
1. findPlanById(planId) → NotFoundException if not found
2. For each non-null field in request → apply change to plan
   - newPlanCurrency: additionally validate via SubscriptionCurrency.isValidCurrency() → BadRequestException if invalid
3. save(plan)
4. Map SubscriptionPlan → SubscriptionPlansData
5. Return HTTP 200
```

---

### `AssignSubscriptionService`
**Package:** `com.extractor.unraveldocs.subscription.impl`

Utility service used internally (not exposed via REST). Assigns a single default `UserSubscription` to a given `User` based on their `Role`.

**Role → Plan mapping:**

| Role | Default Plan |
|---|---|
| `SUPER_ADMIN` | `BUSINESS_YEARLY` |
| `ADMIN` | `BUSINESS_YEARLY` |
| `MODERATOR` | `PRO_YEARLY` |
| `USER` | `FREE` |

**Logic:**
```
1. Determine plan name from user role
2. findByName(planName) → if plan doesn't exist, log warning and return null
3. Build UserSubscription (status = ACTIVE, hasUsedTrial = false)
4. Return UserSubscription (not yet persisted — caller is responsible for saving)
```

---

### `AssignSubscriptionToUsersImpl`
**Package:** `com.extractor.unraveldocs.subscription.impl`  
**Implements:** `AssignSubscriptionToUsersService`  
**Transactional:** Yes

**Logic:**
```
1. findBySubscriptionIsNull() → get all users with no subscription
2. For each user:
   ├─ assignDefaultSubscription(user) → UserSubscription (or null if plan missing)
   └─ If non-null: user.setSubscription(), save(user), increment counter
3. Return HTTP 200 with assignedCount
```

---

### `UserSubscriptionServiceImpl`
**Package:** `com.extractor.unraveldocs.subscription.impl`  
**Implements:** `UserSubscriptionService`

#### `getUserSubscriptionDetails(User)`
**Transaction:** Read-only
```
1. findByUserIdWithPlan(user.id) (eager-loads plan via JOIN FETCH)
2. If empty → return default "none" / "Free Plan" DTO (no error)
3. Calculate isOnTrial (trialEndsAt != null && trialEndsAt > now)
4. Calculate trialDaysRemaining (days between now and trialEndsAt; 0 if expired)
5. Format billingInterval as human-readable string
6. Build and return UserSubscriptionDetailsDto
```

#### `activateTrial(User, String planId)`
**Transaction:** Read-write
```
1. findById(planId) → IllegalArgumentException if not found
2. Validate plan.trialDays > 0 → IllegalArgumentException if not
3. findByUserId(user.id) or create new UserSubscription
4. hasUsedTrial == true → BadRequestException
5. Status is ACTIVE and plan is not FREE → BadRequestException
6. Set status = "TRIAL", trialEndsAt = now + trialDays, hasUsedTrial = true
7. Reset usage counters to zero
8. save(subscription)
9. Send TRIAL_ACTIVATED push notification
10. Send trial activation email
```

#### `checkAndExpireTrials()`
**Transaction:** Read-write — called by `TrialExpiryJob` every hour
```
1. findByTrialEndsAtBetweenAndStatusEquals(1 year ago, now, "TRIAL")
2. Load FREE plan
3. For each expired trial:
   a. Set plan = FREE, status = "ACTIVE", trialEndsAt = null, autoRenew = false
   b. save(subscription)
   c. Send TRIAL_EXPIRED push notification
   d. Send trial expiry email
```

#### `validateSubscriptionEligibility(User)`
**Transaction:** Read-only — guard used before purchasing a new plan
```
1. findByUserIdWithPlan(user.id)
2. If active and not FREE → BadRequestException
```

---

### `PlanPricingServiceImpl`
**Package:** `com.extractor.unraveldocs.subscription.service`  
**Implements:** `PlanPricingService`

#### `getAllPlansWithPricing(SubscriptionCurrency currency)`
```
1. findAll() individual plans → filter isActive
2. findAllActive() team plans (from TeamSubscriptionPlanRepository)
3. For each individual plan:
   ├─ currencyConversionService.convert(plan.price, currency)
   └─ Build IndividualPlanPricingDto (with derived feature list by plan tier)
4. For each team plan:
   ├─ convert monthly price + yearly price
   └─ Build TeamPlanPricingDto (with derived feature list)
5. Get rate timestamp from a sample conversion
6. Return AllPlansWithPricingResponse
```

**Feature derivation by plan tier:**

| Tier | Features |
|---|---|
| FREE | Basic document processing, Limited OCR pages, Email support |
| STARTER | Standard processing, Increased OCR, Priority email support, API access |
| PRO | Advanced processing, High OCR, Priority support, Full API, Custom integrations |
| BUSINESS / ENTERPRISE | Unlimited processing, Unlimited OCR, 24/7 premium support, Full API, Custom integrations, Dedicated account manager |

#### `getSupportedCurrencies()`
```
1. Stream all SubscriptionCurrency enum values
2. Map each → CurrencyInfo (code, symbol, fullName)
3. Return SupportedCurrenciesResponse (currencies list + totalCount)
```

---

### `CurrencyConversionServiceImpl`
**Package:** `com.extractor.unraveldocs.subscription.service`  
**Implements:** `CurrencyConversionService`

Maintains an **in-memory `ConcurrentHashMap<String, BigDecimal>`** of exchange rates. On `@PostConstruct`, seeds the map with hardcoded fallback rates (≈ Dec 2024 values for 29 currencies). Live rates are fetched from `exchangerate-api.com` via `RestTemplate`.

**Conversion logic:**
```
convert(amountUsd, targetCurrency):
  1. If targetCurrency == USD → return as-is with rate = 1
  2. Lookup rate from ratesCache (key = currency code)
  3. If not in cache → attempt live API fetch → fallback to FALLBACK_RATES → default to 1.0
  4. convertedAmount = amountUsd × rate (rounded to 2 d.p. HALF_UP)
  5. formattedPrice = locale-formatted string with currency symbol
  6. Return ConvertedPrice
```

**Cache eviction:** `@CacheEvict` annotation on `refreshRates()` clears the `"exchangeRates"` Spring cache when rates are refreshed.

**Fallback rates** are embedded in the source code for resilience when the external API is unavailable. Key fallbacks include: `NGN: 1550.00`, `GBP: 0.79`, `EUR: 0.92`, `INR: 83.50`, etc.

---

### `SubscriptionFeatureService`
**Package:** `com.extractor.unraveldocs.subscription.service`

Provides programmatic feature gating — called by other packages (e.g., document processing, search) to enforce plan-tier restrictions.

**Premium features (`Feature` enum):**

| Feature | Description |
|---|---|
| `DOCUMENT_MOVE` | Move documents between collections |
| `DOCUMENT_ENCRYPTION` | Encrypt stored documents |
| `ADVANCED_SEARCH` | Advanced search/filtering capabilities |
| `PRIORITY_OCR` | Priority queue for OCR processing |

**Plans with premium access (STARTER and above):**
`STARTER_MONTHLY`, `STARTER_YEARLY`, `PRO_MONTHLY`, `PRO_YEARLY`, `BUSINESS_MONTHLY`, `BUSINESS_YEARLY`

**Methods:**

| Method | Returns | Description |
|---|---|---|
| `hasFeatureAccess(String userId, Feature feature)` | `boolean` | Returns `true` if user's plan is in the premium tier set |
| `requireFeatureAccess(String userId, Feature feature)` | `void` | Calls `hasFeatureAccess`; throws `ForbiddenException` if access denied |
| `hasPaidSubscription(String userId)` | `boolean` | Returns `true` if user's plan is not `FREE` |

---

## Repositories

### `SubscriptionPlanRepository`
**Extends:** `JpaRepository<SubscriptionPlan, String>`

| Method | Description |
|---|---|
| `findByName(SubscriptionPlans name)` | Look up a plan by its enum name — used during default assignment and trial activation |
| `findPlanById(String planId)` | Look up a plan by UUID — used during plan updates |

---

### `UserSubscriptionRepository`
**Extends:** `JpaRepository<UserSubscription, String>`

| Method | Description |
|---|---|
| `findByUserId(String userId)` | Look up a user's subscription by user ID |
| `findByUserIdWithPlan(String userId)` | `JOIN FETCH us.plan` — eager loads plan to avoid N+1 |
| `findByPaymentGatewaySubscriptionId(String id)` | Look up by external gateway subscription ID (webhooks) |
| `findByCurrentPeriodEndBetweenAndAutoRenewFalse(start, end)` | Find subscriptions expiring soon with auto-renew off |
| `findByTrialEndsAtBetweenAndStatusEquals(start, end, status)` | Find expired trials — used by `TrialExpiryJob` |
| `findActivePaidSubscriptions()` | All `ACTIVE` non-FREE subscriptions — used by coupon targeting |
| `findActiveSubscriptionsByPlanName(String planName)` | Active subscriptions by plan name — used by coupon targeting |
| `findFreeTierWithHighActivity()` | FREE-plan users with `ocrPagesUsed >= 20` — used by coupon targeting |
| `findRecentlyExpiredSubscriptions(OffsetDateTime threeMonthsAgo)` | Churned users within 3 months — used by coupon targeting |
| `findHighActivitySubscriptions()` | Users using > 50% of their OCR page limit — used by coupon targeting |
| `findSubscriptionsNeedingQuotaReset(OffsetDateTime now)` | Subscriptions where `quotaResetDate <= now` — used by quota reset job |
| `findSubscriptionsWithoutQuotaResetDate()` | Subscriptions with `quotaResetDate IS NULL` — used by init job |

---

## Scheduled Jobs

### `ExchangeRateUpdateJob`
**Package:** `com.extractor.unraveldocs.subscription.jobs`  
**Schedule:** `0 0 6 * * *` — daily at 06:00

Calls `CurrencyConversionService.refreshRates()` to fetch live exchange rates from the external API. On failure, logs the error and retains the existing cached rates (degraded mode with fallback rates).

---

### `MonthlyQuotaResetJob`
**Package:** `com.extractor.unraveldocs.subscription.jobs`

**Two scheduled methods:**

#### `resetMonthlyQuotas`
**Schedule:** `0 0 * * * *` — every hour (checks rather than resets all)

```
1. findSubscriptionsNeedingQuotaReset(now) — where quotaResetDate <= now
2. If empty → skip
3. For each subscription:
   a. Record previous monthlyDocumentsUploaded, ocrPagesUsed, aiOperationsUsed (for logging)
   b. Reset all three counters to 0
   c. quotaResetDate = first day of next month at 00:00:00 UTC
   d. save(subscription)
```

**Why hourly?** To recover from cases where the server was down on the 1st of the month, ensuring no subscription misses its reset.

#### `initializeQuotaResetDates`
**Schedule:** `0 30 0 * * *` — daily at 00:30

```
1. findSubscriptionsWithoutQuotaResetDate()
2. If empty → skip
3. Calculate nextResetDate = first day of next month at 00:00:00 UTC
4. For each subscription: set quotaResetDate = nextResetDate, save
```

Ensures all subscriptions (including newly created ones) always have a `quotaResetDate` set.

---

### `TrialExpiryJob`
**Package:** `com.extractor.unraveldocs.subscription.jobs`  
**Schedule:** `0 0 * * * *` — every hour

Delegates to `UserSubscriptionService.checkAndExpireTrials()`:
```
1. Find all subscriptions with status = "TRIAL" and trialEndsAt <= now
2. Load the FREE plan
3. For each expired trial:
   a. Set plan = FREE, status = "ACTIVE", trialEndsAt = null, autoRenew = false
   b. save(subscription)
   c. Send TRIAL_EXPIRED push notification + trial expiry email
```

---

## Currency Conversion System

The currency system uses a two-tier approach for resilience:

```
Request for converted price
        │
        ▼
  ratesCache.get(currency.code)
        │
  ┌─────┴─────┐
  │ Cache HIT │──────────────────────────────► Use cached rate
  └─────┬─────┘
        │ Cache MISS
        ▼
  Fetch from exchangerate-api.com
        │
  ┌─────┴──────────┐
  │ API Success    │──────────────────────────► Update cache, use live rate
  └─────┬──────────┘
        │ API Failure / Network error
        ▼
  FALLBACK_RATES.get(currency.code)
        │
  ┌─────┴───────────────────┐
  │ Fallback rate found     │────────────────► Use fallback rate
  └─────┬───────────────────┘
        │ No fallback
        ▼
  Default rate = 1.0 (treat as USD)
```

**Rate refresh:** The `ExchangeRateUpdateJob` calls `refreshRates()` daily at 06:00, which fetches fresh rates from the API and updates the in-memory `ratesCache`. On refresh, the Spring `"exchangeRates"` cache is evicted via `@CacheEvict`.

---

## Feature Access Control

Other packages call `SubscriptionFeatureService` to gate premium features:

**Access matrix:**

| Feature | FREE | STARTER | PRO | BUSINESS |
|---|---|---|---|---|
| `DOCUMENT_MOVE` | ❌ | ✅ | ✅ | ✅ |
| `DOCUMENT_ENCRYPTION` | ❌ | ✅ | ✅ | ✅ |
| `ADVANCED_SEARCH` | ❌ | ✅ | ✅ | ✅ |
| `PRIORITY_OCR` | ❌ | ✅ | ✅ | ✅ |

---

## Default Subscription Assignment Rules

Applied automatically during:
- **User registration** (`SignupUserImpl`) — called before the user is saved
- **Login** (`LoginUserImpl`) — safety net if the user somehow has no subscription
- **Admin bulk-assign** (`AssignSubscriptionToUsersImpl`) — backfills existing users

| Role | Default Plan |
|---|---|
| `USER` | `FREE` |
| `MODERATOR` | `PRO_YEARLY` |
| `ADMIN` | `BUSINESS_YEARLY` |
| `SUPER_ADMIN` | `BUSINESS_YEARLY` |

> ⚠️ If the required default plan does not exist in the database (e.g., plans haven't been seeded by an admin), `assignDefaultSubscription()` returns `null` and logs a warning. The user is saved without a subscription until the bulk-assign endpoint is called.

---

## Validation Rules

| Rule | Applied To | Details |
|---|---|---|
| `@NotNull` | `name`, `price`, `currency`, `billingIntervalUnit`, `billingIntervalValue`, `documentUploadLimit`, `ocrPageLimit` | All required for plan creation |
| `@Min(0)` | `price` | Must be a non-negative number |
| `@Min(1)` | `billingIntervalValue` | Must be at least 1 billing unit |
| `@Min(0)` | `trialDays` | Cannot be negative |
| Unique plan name | `AddSubscriptionPlansImpl` | Enforced in service layer via `findByName` check — `BadRequestException` if duplicate |
| Valid currency | `UpdateSubscriptionPlanImpl` | `SubscriptionCurrency.isValidCurrency()` check — `BadRequestException` if invalid |
| Plan supports trial | `UserSubscriptionServiceImpl` | `plan.trialDays > 0` — `IllegalArgumentException` if not |
| Trial not already used | `UserSubscriptionServiceImpl` | `hasUsedTrial == false` — `BadRequestException` if already used |
| No active paid sub | `UserSubscriptionServiceImpl` | Cannot trial if already on a paid `ACTIVE` plan |

---

## Error Reference

| Exception Class | HTTP Status | Common Trigger |
|---|---|---|
| `BadRequestException` | `400 Bad Request` | Duplicate plan name, invalid currency, trial already used, active paid subscription exists |
| `IllegalArgumentException` | `400 Bad Request` | Invalid currency code (public pricing endpoint), plan not found, plan doesn't support trials |
| `ForbiddenException` | `403 Forbidden` | Non-admin accessing admin endpoints; feature access denied (from `SubscriptionFeatureService`) |
| `NotFoundException` | `404 Not Found` | Plan not found by ID during update |
| `409 Conflict` | `409 Conflict` | (Documented in Swagger for trial endpoint) — effectively returned as `400 Bad Request` in current implementation |

---

## Flow Diagrams

### Plan Creation Flow (Admin)

```
Admin Client
  │
  ├─ POST /api/v1/admin/subscriptions/plans
  │   Authorization: Bearer <token>  [ROLE_ADMIN or ROLE_SUPER_ADMIN]
  │
  ▼
SubscriptionController
  │
  ▼
SubscriptionPlansService → AddSubscriptionPlansImpl
  │
  ├─ [Plan name exists?] ──────────────────────────► 400 Bad Request
  │
  ├─ Build SubscriptionPlan entity
  ├─ save(newPlan)
  ├─ Map → SubscriptionPlansData
  │
  ▼
201 Created + SubscriptionPlansData
```

---

### Public Pricing Page Flow

```
Browser / Client
  │
  ├─ GET /api/v1/plans?currency=NGN
  │
  ▼
PlanPricingController
  │
  ├─ Parse currency code → SubscriptionCurrency.fromIdentifier("NGN")
  ├─ [Invalid currency?] ─────────────────────────► 400 Bad Request
  │
  ▼
PlanPricingServiceImpl.getAllPlansWithPricing(NGN)
  │
  ├─ Fetch all active individual plans
  ├─ Fetch all active team plans
  │
  ├─ For each individual plan:
  │     └─ CurrencyConversionService.convert(price, NGN)
  │           ├─ Cache HIT → use cached rate
  │           ├─ Cache MISS → fetch live rate → fallback if API down
  │           └─ Return ConvertedPrice
  │
  ├─ For each team plan:
  │     └─ convert monthly + yearly price
  │
  └─ Assemble AllPlansWithPricingResponse
  │
  ▼
200 OK + AllPlansWithPricingResponse
```

---

### Trial Activation Flow

```
User Client
  │
  ├─ POST /api/v1/subscriptions/trial/{planId}
  │   Authorization: Bearer <token>
  │
  ▼
UserSubscriptionController
  │
  ▼
UserSubscriptionServiceImpl.activateTrial(user, planId)
  │
  ├─ findById(planId) ────────────────────────────► 404 / IllegalArgumentException
  ├─ [plan.trialDays <= 0?] ──────────────────────► 400 Bad Request
  ├─ findByUserId(user.id) or create new sub
  ├─ [hasUsedTrial?] ─────────────────────────────► 400 Bad Request
  ├─ [Active paid sub?] ──────────────────────────► 400 Bad Request
  │
  ├─ Set status = "TRIAL", trialEndsAt = now + trialDays
  ├─ Set hasUsedTrial = true
  ├─ Reset usage counters to zero
  ├─ save(subscription)
  │
  ├─ Send TRIAL_ACTIVATED push notification
  └─ Send trial activation email
  │
  ▼
200 OK
```

---

### Trial Expiry Flow (Scheduled)

```
[Every Hour — TrialExpiryJob]
  │
  ├─ UserSubscriptionServiceImpl.checkAndExpireTrials()
  │
  ├─ Find subscriptions: status = "TRIAL" AND trialEndsAt <= now
  ├─ Load FREE plan
  │
  ├─ For each expired trial:
  │   ├─ plan = FREE, status = "ACTIVE"
  │   ├─ trialEndsAt = null, autoRenew = false
  │   ├─ save(subscription)
  │   ├─ Send TRIAL_EXPIRED push notification
  │   └─ Send trial expiry email
  │
  └─ Done
```

---

### Monthly Quota Reset Flow (Scheduled)

```
[Every Hour — MonthlyQuotaResetJob]
  │
  ├─ findSubscriptionsNeedingQuotaReset(now)
  │     WHERE quotaResetDate IS NOT NULL AND quotaResetDate <= now
  │
  ├─ If empty → skip
  │
  └─ For each subscription:
      ├─ monthlyDocumentsUploaded = 0
      ├─ ocrPagesUsed = 0
      ├─ aiOperationsUsed = 0
      ├─ quotaResetDate = first day of next month 00:00:00 UTC
      └─ save(subscription)

[Daily at 00:30 — initializeQuotaResetDates]
  │
  ├─ findSubscriptionsWithoutQuotaResetDate()
  └─ For each: set quotaResetDate = first day of next month, save
```

---

### Default Subscription Assignment (on Registration)

```
SignupUserImpl.registerUser()
  │
  ├─ Build User entity (role = USER)
  │
  ├─ AssignSubscriptionService.assignDefaultSubscription(user)
  │     ├─ Role → plan name: USER → FREE
  │     ├─ findByName(FREE)
  │     │     └─ [Plan missing?] log warning, return null
  │     └─ Build UserSubscription (status = ACTIVE, hasUsedTrial = false)
  │
  ├─ user.setSubscription(subscription)
  └─ userRepository.save(user)  ← cascades to UserSubscription
```

