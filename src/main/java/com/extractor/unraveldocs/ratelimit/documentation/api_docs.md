# Rate Limit Package — Documentation

> **Package:** `com.extractor.unraveldocs.ratelimit`  
> **Applies to:** `/api/v1/ai/**`  
> **Last Updated:** February 26, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Architecture](#architecture)
4. [Configuration](#configuration)
   - [RateLimitConfig](#ratelimitconfig)
   - [RateLimitWebConfig](#ratelimitwebconfig)
5. [Service](#service)
   - [RateLimitService](#ratelimitservice)
6. [Interceptor](#interceptor)
   - [RateLimitInterceptor](#ratelimitinterceptor)
7. [Tier Defaults & Plan Mapping](#tier-defaults--plan-mapping)
8. [Response Headers](#response-headers)
9. [Error Handling](#error-handling)
10. [Configuration Reference](#configuration-reference)
11. [Flow Diagrams](#flow-diagrams)

---

## Overview

The **Rate Limit** package enforces **per-subscription-tier** request limits on all AI endpoints (`/api/v1/ai/**`). It uses the **token bucket algorithm** (via [Bucket4j](https://github.com/bucket4j/bucket4j)) with two layered guards per user:

| Guard | Scope | Purpose |
|---|---|---|
| **Per-minute bucket** | Short-term | Caps burst and sustained request rate per minute |
| **Daily bucket** | Long-term | Hard daily ceiling that resets every 24 hours |

Every authenticated user gets their own pair of buckets, stored in an in-memory `ConcurrentHashMap`. Limits scale with the user's active subscription plan.

**Key design decisions:**
- Limits are applied **only to AI endpoints** — all other API routes are unrestricted.
- If rate limiting is disabled (`rate-limit.enabled=false`), `consumeToken()` returns `Long.MAX_VALUE` with no bucket interaction.
- Users with no subscription (or a null plan) are treated as the **FREE** tier.
- The daily bucket is checked **first**; if it is exhausted, the minute bucket is **not** consumed (preventing double-counting).
- If the minute bucket rejects, the daily token is **refunded** atomically to prevent inconsistency.

---

## Package Structure

```
ratelimit/
├── config/
│   ├── RateLimitConfig.java        # @ConfigurationProperties(prefix="rate-limit") — enabled flag + per-tier TierLimit map
│   └── RateLimitWebConfig.java     # Registers RateLimitInterceptor on /api/v1/ai/** only
├── interceptor/
│   └── RateLimitInterceptor.java   # HandlerInterceptor: resolves user + plan → calls RateLimitService → sets response headers
├── service/
│   └── RateLimitService.java       # Token bucket engine: ConcurrentHashMap<userId:scope, Bucket4j Bucket>
└── documentation/
    └── api_docs.md                 # This file
```

---

## Architecture

```
HTTP Request → /api/v1/ai/**
        │
        ▼
RateLimitInterceptor.preHandle()
        │
        ├─ SecurityContextHolder.getAuthentication()
        │       └─ null or unauthenticated → pass through (let Spring Security handle it)
        │
        ├─ userRepository.findByEmail(auth.getName())
        │       └─ not found → pass through
        │
        ├─ resolveUserPlan(userId)
        │       └─ userSubscriptionRepository.findByUserIdWithPlan(userId)
        │               └─ no subscription or null plan → null (= FREE tier)
        │
        ├─ rateLimitService.consumeToken(userId, plan)
        │       │
        │       ├─ !rateLimitConfig.isEnabled() → return Long.MAX_VALUE
        │       │
        │       ├─ TierLimit = rateLimitConfig.getTierLimit(plan)
        │       │
        │       ├─ minuteBucket = buckets.computeIfAbsent(userId+":minute", createMinuteBucket)
        │       ├─ dailyBucket  = buckets.computeIfAbsent(userId+":daily",  createDailyBucket)
        │       │
        │       ├─ dailyBucket.tryConsume(1) == false
        │       │       └─ throw TooManyRequestsException("Daily AI request limit exceeded")
        │       │
        │       ├─ minuteBucket.tryConsume(1) == false
        │       │       ├─ dailyBucket.addTokens(1)   [refund daily token]
        │       │       └─ throw TooManyRequestsException("Rate limit exceeded. Max N per minute")
        │       │
        │       └─ return minuteBucket.getAvailableTokens()
        │
        ├─ response.setHeader("X-RateLimit-Limit", limit)
        ├─ response.setHeader("X-RateLimit-Remaining", remaining)
        └─ return true  →  proceed to controller
```

---

## Configuration

### `RateLimitConfig`
**Package:** `com.extractor.unraveldocs.ratelimit.config`  
**Prefix:** `rate-limit`

```java
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {
    private boolean enabled = true;
    private Map<String, TierLimit> tiers = new HashMap<>();

    public static class TierLimit {
        private int requestsPerMinute;
        private int burstCapacity;
        private int dailyLimit;
    }
}
```

**`getTierLimit(SubscriptionPlans plan)`**

Resolves the correct `TierLimit` from the `tiers` map using a `plan → tierKey` translation, then falls back to FREE tier defaults if the key is not present in configuration.

**Plan → Tier Key mapping:**

| `SubscriptionPlans` value(s) | Tier Key | Map lookup key |
|---|---|---|
| `STARTER_MONTHLY`, `STARTER_YEARLY` | starter | `tiers.get("starter")` |
| `PRO_MONTHLY`, `PRO_YEARLY` | pro | `tiers.get("pro")` |
| `BUSINESS_MONTHLY`, `BUSINESS_YEARLY` | business | `tiers.get("business")` |
| *(anything else / null)* | free | hardcoded defaults |

**Free tier defaults (hardcoded fallback):**

| Setting | Default Value |
|---|---|
| `requestsPerMinute` | `5` |
| `burstCapacity` | `8` |
| `dailyLimit` | `50` |

---

### `RateLimitWebConfig`
**Package:** `com.extractor.unraveldocs.ratelimit.config`

Registers the `RateLimitInterceptor` **only** on paths matching `/api/v1/ai/**` via `InterceptorRegistry`. All other application routes are unaffected.

```java
registry.addInterceptor(rateLimitInterceptor)
        .addPathPatterns("/api/v1/ai/**");
```

---

## Service

### `RateLimitService`
**Package:** `com.extractor.unraveldocs.ratelimit.service`

The core rate-limiting engine. Manages a `ConcurrentHashMap<String, Bucket>` keyed by `"{userId}:minute"` and `"{userId}:daily"`.

#### `consumeToken(String userId, SubscriptionPlans plan)`

| Step | Description |
|---|---|
| 1 | If `!rateLimitConfig.isEnabled()` → return `Long.MAX_VALUE` (bypass) |
| 2 | Resolve `TierLimit` from plan |
| 3 | `computeIfAbsent` per-minute bucket for `userId:minute` |
| 4 | `computeIfAbsent` daily bucket for `userId:daily` |
| 5 | `dailyBucket.tryConsume(1)` → false → throw `TooManyRequestsException` with daily limit message |
| 6 | `minuteBucket.tryConsume(1)` → false → refund daily token (`dailyBucket.addTokens(1)`) → throw `TooManyRequestsException` with per-minute limit message |
| 7 | Return `minuteBucket.getAvailableTokens()` |

**Returns:** Remaining tokens in the per-minute bucket (used for `X-RateLimit-Remaining` response header).  
**Throws:** `TooManyRequestsException` (HTTP 429) on either bucket exhaustion.

#### `getLimit(SubscriptionPlans plan)`

Returns `TierLimit.requestsPerMinute` for the given plan. Used to populate the `X-RateLimit-Limit` response header without consuming a token.

---

### Bucket Construction

#### Per-Minute Bucket (`createMinuteBucket`)

Uses **Bucket4j classic** with a greedy refill:

```
Capacity = burstCapacity       (allows short bursts above sustained rate)
Refill   = requestsPerMinute tokens per 1 minute (greedy — tokens become available continuously)
```

> **Greedy vs interval refill:** Greedy refill distributes tokens evenly over the period rather than adding them all at once at the interval boundary. This prevents "clock edge" bursting where all tokens refill simultaneously.

#### Daily Bucket (`createDailyBucket`)

```
Capacity = dailyLimit
Refill   = dailyLimit tokens per 24 hours (greedy — resets gradually)
```

---

## Interceptor

### `RateLimitInterceptor`
**Package:** `com.extractor.unraveldocs.ratelimit.interceptor`  
**Implements:** `HandlerInterceptor`  
**Registered on:** `/api/v1/ai/**`

**`preHandle()` steps:**

1. Get `Authentication` from `SecurityContextHolder`. If null or unauthenticated → return `true` (pass through; let Spring Security reject it).
2. Look up user by email (`auth.getName()`). If not found → return `true` (pass through).
3. Call `resolveUserPlan(userId)` → queries `userSubscriptionRepository.findByUserIdWithPlan(userId)`. Returns `null` if no subscription or null plan (treated as FREE).
4. Call `rateLimitService.consumeToken(userId, plan)` — may throw `TooManyRequestsException` (HTTP 429).
5. Set response headers:
   - `X-RateLimit-Limit` → `rateLimitService.getLimit(plan)` (per-minute cap)
   - `X-RateLimit-Remaining` → tokens remaining from `consumeToken()` return value
6. Return `true` → proceed to controller.

**`resolveUserPlan(userId)`:** Uses `userSubscriptionRepository.findByUserIdWithPlan(userId)`. Returns `null` if the `Optional` is empty or the plan is null.

---

## Tier Defaults & Plan Mapping

The following table shows the **recommended** configuration values. These should be set in `application.properties` under the `rate-limit.tiers.*` prefix.

| Tier | Plans Included | `requestsPerMinute` | `burstCapacity` | `dailyLimit` |
|---|---|---|---|---|
| **free** *(hardcoded fallback)* | No subscription / unknown plan | 5 | 8 | 50 |
| **starter** | `STARTER_MONTHLY`, `STARTER_YEARLY` | 15 | 20 | 200 |
| **pro** | `PRO_MONTHLY`, `PRO_YEARLY` | 30 | 40 | 500 |
| **business** | `BUSINESS_MONTHLY`, `BUSINESS_YEARLY` | 60 | 80 | 1000 |

> **Note:** The free tier values above (`5 rpm / 50 daily`) are baked into `getDefaultFreeTier()` and apply even when `rate-limit.tiers.free` is not defined in properties. All other tiers must be explicitly configured.

---

## Response Headers

Every request to `/api/v1/ai/**` from an authenticated user receives the following standard rate limit headers in the response, regardless of whether the limit was hit:

| Header | Value | Description |
|---|---|---|
| `X-RateLimit-Limit` | `int` | Maximum requests per minute for the user's tier |
| `X-RateLimit-Remaining` | `long` | Tokens remaining in the per-minute bucket after this request |

**Example response headers (Pro plan user, 28 tokens remaining):**
```
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 28
```

> **No `X-RateLimit-Reset` header is currently set.** If needed, Bucket4j can provide the estimated time until the next token is available via `bucket.getAvailableTokens()` or `ProbeSnapshot`.

---

## Error Handling

When a rate limit is exceeded, `RateLimitService` throws a `TooManyRequestsException`. This is a custom exception that should be mapped to HTTP `429 Too Many Requests` by the global exception handler.

**Per-minute exhaustion error message:**
```
Rate limit exceeded. Maximum {N} AI requests per minute. Please try again shortly.
```

**Daily exhaustion error message:**
```
Daily AI request limit of {N} exceeded. Limit resets in 24 hours.
```

**Example 429 response body (handled by global exception handler):**
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Maximum 30 AI requests per minute. Please try again shortly.",
  "timestamp": "2026-02-26T14:35:00Z"
}
```

> **Retry-After header:** Not currently set. Adding `response.setHeader("Retry-After", "60")` in the exception handler or interceptor is recommended for full RFC 6585 compliance.

---

## Configuration Reference

Full `application.properties` configuration for all tiers:

```properties
# Master toggle — set to false to disable rate limiting globally
rate-limit.enabled=true

# Free tier (hardcoded fallback — does not need to be in properties)
# rate-limit.tiers.free.requests-per-minute=5
# rate-limit.tiers.free.burst-capacity=8
# rate-limit.tiers.free.daily-limit=50

# Starter plan (STARTER_MONTHLY + STARTER_YEARLY)
rate-limit.tiers.starter.requests-per-minute=15
rate-limit.tiers.starter.burst-capacity=20
rate-limit.tiers.starter.daily-limit=200

# Pro plan (PRO_MONTHLY + PRO_YEARLY)
rate-limit.tiers.pro.requests-per-minute=30
rate-limit.tiers.pro.burst-capacity=40
rate-limit.tiers.pro.daily-limit=500

# Business plan (BUSINESS_MONTHLY + BUSINESS_YEARLY)
rate-limit.tiers.business.requests-per-minute=60
rate-limit.tiers.business.burst-capacity=80
rate-limit.tiers.business.daily-limit=1000
```

---

## Flow Diagrams

### Happy Path — Token Consumed

```
User (Pro plan, 28/30 remaining) → GET /api/v1/ai/extract
        │
        ▼
RateLimitInterceptor.preHandle()
        │
        ├─ auth = SecurityContext → "user@example.com"
        ├─ user = userRepository.findByEmail("user@example.com")
        ├─ plan = PRO_MONTHLY (from subscription)
        │
        ▼
RateLimitService.consumeToken(userId, PRO_MONTHLY)
        │
        ├─ enabled = true
        ├─ tierLimit = {requestsPerMinute=30, burstCapacity=40, dailyLimit=500}
        ├─ minuteBucket = buckets["userId:minute"]  ← 28 tokens remaining
        ├─ dailyBucket  = buckets["userId:daily"]   ← 492 tokens remaining
        │
        ├─ dailyBucket.tryConsume(1)  → true (491 remaining)
        ├─ minuteBucket.tryConsume(1) → true (27 remaining)
        └─ return 27
        │
        ▼
response.setHeader("X-RateLimit-Limit", "30")
response.setHeader("X-RateLimit-Remaining", "27")
        │
        ▼
Controller handles request → 200 OK
```

---

### Per-Minute Rate Limit Hit

```
User (Free plan, 0/5 per-minute remaining, 47/50 daily remaining)
        │
        ▼
RateLimitService.consumeToken(userId, null [FREE])
        │
        ├─ dailyBucket.tryConsume(1)  → true (46 remaining)
        ├─ minuteBucket.tryConsume(1) → FALSE (0 remaining)
        │
        ├─ dailyBucket.addTokens(1)   ← refund the daily token (back to 47)
        └─ throw TooManyRequestsException(
               "Rate limit exceeded. Maximum 5 AI requests per minute. Please try again shortly."
           )
        │
        ▼
Global Exception Handler → 429 Too Many Requests
{
  "message": "Rate limit exceeded. Maximum 5 AI requests per minute. Please try again shortly."
}
```

---

### Daily Limit Hit

```
User (Starter plan, 15/15 per-minute remaining, 0/200 daily remaining)
        │
        ▼
RateLimitService.consumeToken(userId, STARTER_MONTHLY)
        │
        ├─ dailyBucket.tryConsume(1) → FALSE (0 remaining)
        └─ throw TooManyRequestsException(
               "Daily AI request limit of 200 exceeded. Limit resets in 24 hours."
           )
        │
        ▼
Global Exception Handler → 429 Too Many Requests
{
  "message": "Daily AI request limit of 200 exceeded. Limit resets in 24 hours."
}
```

---

### Rate Limiting Disabled

```
application.properties: rate-limit.enabled=false
        │
        ▼
RateLimitService.consumeToken(userId, plan)
        │
        └─ !rateLimitConfig.isEnabled() → return Long.MAX_VALUE
        │
        ▼
response.setHeader("X-RateLimit-Limit", "30")         ← still set from getLimit()
response.setHeader("X-RateLimit-Remaining", "9223372036854775807")  ← Long.MAX_VALUE
        │
        ▼
Controller handles request → no throttling applied
```

---

### Bucket Lifecycle

```
First request from new user:
  buckets.computeIfAbsent("userId:minute", ...)
      └─ createMinuteBucket(tierLimit)
              └─ Bucket.classic(burstCapacity=40, Refill.greedy(30, 1min))
                    └─ starts FULL (40 tokens available)

  buckets.computeIfAbsent("userId:daily", ...)
      └─ createDailyBucket(tierLimit)
              └─ Bucket.classic(500, Refill.greedy(500, 24h))
                    └─ starts FULL (500 tokens available)

Subsequent requests:
  buckets.computeIfAbsent("userId:minute", ...)
      └─ returns existing bucket (NOT recreated)

Token refill (greedy — continuous):
  Per-minute: +30 tokens per 60 seconds = +0.5 tokens/second
  Daily:      +500 tokens per 86400 seconds = +0.0058 tokens/second

⚠️ NOTE: Buckets are in-memory (ConcurrentHashMap).
   On application restart, all buckets are reset to full capacity.
   For distributed rate limiting, replace the in-memory map with
   a Redis-backed Bucket4j ProxyManager.
```

