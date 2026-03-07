# Auth API — Comprehensive Improvement Analysis

> **Scope:** Login, Verify Email, Resend Verification Email, Refresh Token, and Logout endpoints  
> **Date:** March 7, 2026

---

## 🔴 Critical — Security Vulnerabilities

### 1. Refresh Token NOT Invalidated on Logout
**Endpoint:** `POST /logout`  
**Issue:** The docs explicitly note: *"The associated refresh token is NOT automatically invalidated."* This means after a user logs out, a stolen refresh token can still be used to generate new access tokens indefinitely — effectively making logout a no-op from a security standpoint.

**Recommendation:** On logout, delete the refresh token JTI from the server-side token store. The `logout()` implementation should call `refreshTokenStore.delete(jti)` alongside blacklisting the access token.

---

### 2. Soft-Deleted Account Reactivation on Login (Step 8)
**Endpoint:** `POST /login`  
**Issue:** The login flow silently restores soft-deleted accounts: *"Restore soft-deleted account if applicable."* If an admin soft-deleted an account (e.g., for abuse, compliance, or the user requested deletion), anyone with the old credentials can silently resurrect it.

**Recommendation:**
- Soft-deleted accounts should **not** be reactivatable via login.
- Return a clear error like `410 Gone` or `400 Bad Request` with message: *"This account has been deactivated. Contact support."*
- If self-reactivation is desired, make it a separate explicit endpoint with re-verification.

---

### 3. User Enumeration via Error Differentiation
**Endpoints:** `POST /verify-email`, `POST /resend-verification-email`  
**Issue:** These endpoints return `404 Not Found` when no user exists for the given email, but `400 Bad Request` for "already verified." An attacker can enumerate registered emails by observing the difference between 404 and 400.

**Recommendation:**
- For `/resend-verification-email`: Always return `200 OK` with a generic message: *"If an account with this email exists, a verification email has been sent."*
- For `/verify-email`: Return a generic `400` for all failure cases (invalid token, no user, expired token) — do not differentiate `404` from `400`.

---

### 4. Token Storage in localStorage/sessionStorage (Frontend)
**Current Implementation:** Tokens are stored in `localStorage` or `sessionStorage`, which are accessible to any JavaScript running on the page.

**Issue:** Vulnerable to XSS attacks. If any XSS vulnerability exists (even via a third-party script), an attacker can exfiltrate both access and refresh tokens.

**Recommendation:**
- **Refresh token** → Set as an `HttpOnly`, `Secure`, `SameSite=Strict` cookie from the backend. This makes it completely inaccessible to JavaScript.
- **Access token** → Keep in-memory only (a service variable), never persisted to storage.
- This requires backend changes to set the cookie on `/login` and `/refresh-token`, and automatically include it on `/refresh-token` and `/logout` calls.

---

### 5. Resend Verification Leaks Token TTL
**Endpoint:** `POST /resend-verification-email`  
**Issue:** When a valid token already exists, the error response *"includes time remaining before resend is allowed."* This tells an attacker exactly when the verification token expires, enabling timed brute-force attacks.

**Recommendation:** Return a generic message: *"Please wait before requesting a new verification email."* without revealing the exact remaining time.

---

## 🟠 High — API Design & Logic Issues

### 6. Incorrect HTTP Status Codes on Login
**Endpoint:** `POST /login`  
**Issue:** Invalid credentials return `400 Bad Request`. A `400` means the request was syntactically malformed — it doesn't mean "wrong password." A disabled account also returns `400` which conflates two unrelated failure modes.

**Recommendation:**
| Condition | Current | Recommended |
|---|---|---|
| Invalid email/password | `400` | `401 Unauthorized` |
| Account disabled (unverified) | `400` | `403 Forbidden` with code `ACCOUNT_NOT_VERIFIED` |
| Account locked | `403` | `429 Too Many Requests` (with `Retry-After` header) |

Additionally, for security, all credential-related failures should return the **same** generic message (*"Invalid email or password"*) to prevent user enumeration.

---

### 7. No Absolute Refresh Token Expiry (Infinite Sessions)
**Endpoint:** `POST /refresh-token`  
**Issue:** The rolling refresh strategy issues a new refresh token on every refresh. If a user (or attacker) keeps refreshing, the session can persist **indefinitely** — there is no absolute maximum session lifetime documented.

**Recommendation:** Implement an absolute session lifetime (e.g., 30 days). After this period, the user must re-authenticate regardless of rolling refreshes. Store the original `iat` (issued-at) from the first login and check it during refresh.

---

### 8. Login Response Mixes Tokens with Profile Data
**Endpoint:** `POST /login`  
**Issue:** The login response returns 15+ user profile fields alongside tokens. This conflates authentication (getting tokens) with profile retrieval, leads to bloated responses, and leaks sensitive fields like `role`, `isPlatformAdmin`, `isOrganizationAdmin`, `createdAt`.

**Recommendation:**
- Login should return: `{ accessToken, refreshToken, tokenType, expiresIn }` only.
- User profile should be retrieved via `GET /api/v1/users/me` (authenticated).
- This follows the separation of concerns principle and reduces attack surface.

---

### 9. No "Logout All Devices" / Revoke All Sessions
**Issue:** There's no mechanism to invalidate all active sessions for a user. This is critical for scenarios like:
- Password change/reset
- Account compromise
- User-initiated "sign out everywhere"

**Recommendation:** Add `POST /api/v1/auth/logout-all` that:
1. Deletes all refresh token JTIs for the user from the token store.
2. Optionally blacklists all outstanding access tokens (or increment a per-user token version counter that invalidates all prior tokens).

---

### 10. Default Subscription Assignment on Every Login (Step 9)
**Endpoint:** `POST /login`  
**Issue:** *"A default subscription is assigned if the user has none."* This could silently re-assign a subscription to a user whose subscription was intentionally removed (expired, cancelled, revoked by admin).

**Recommendation:** Only assign a default subscription during registration (which it already does). On login, if no subscription exists, either:
- Do nothing and let the UI handle it (show "choose a plan" page).
- Only assign if `user.subscription == null AND user.hasNeverHadSubscription == true`.

---

### 11. Inconsistent Response DTOs Between Login and Refresh
**Issue:**
| Field | Login Response | Refresh Response |
|---|---|---|
| `tokenType` | ❌ Missing | ✅ `"Bearer"` |
| `accessExpirationInMs` | ❌ Missing | ✅ `900000` |
| User profile fields | ✅ 15+ fields | ✅ Only `id`, `email` |

This inconsistency forces the frontend to handle two different response shapes for similar operations.

**Recommendation:** Standardize a `TokenResponseDto` used by both endpoints:
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "accessExpiresIn": 900000,
  "refreshExpiresIn": 604800000
}
```

---

### 12. Logout Should Return `204 No Content`
**Endpoint:** `POST /logout`  
**Issue:** Returns `200 OK` with `"data": null`. Since logout produces no resource, `204 No Content` is the correct status code.

**Recommendation:** Return `204 No Content` with an empty body.

---

## 🟡 Medium — Missing Features & Documentation Gaps

### 13. No Rate Limiting Documentation
**Endpoints:** `/login`, `/verify-email`, `/resend-verification-email`  
**Issue:** While login attempts are tracked per-user, there's no mention of **IP-based rate limiting**. An attacker can perform credential-stuffing attacks across many accounts from the same IP without being throttled.

**Recommendation:**
- Apply IP-based rate limiting on all auth endpoints (e.g., 10 req/min on `/login`, 3 req/min on `/resend-verification-email`).
- Return `429 Too Many Requests` with a `Retry-After` header.
- Document the rate limits in the API docs.

---

### 14. Token Lifetimes Not Documented
**Issue:** The docs never specify:
- Access token TTL (presumably 15 min based on `accessExpirationInMs: 900000` in refresh response, but not confirmed)
- Refresh token TTL
- Verification token TTL is mentioned as 3 hours but not in the endpoint docs

**Recommendation:** Add a **Token Lifetimes** table:
| Token | TTL | Notes |
|---|---|---|
| Access Token | 15 minutes | Short-lived; use refresh token to renew |
| Refresh Token | 7 days | Rolling; absolute max 30 days |
| Email Verification Token | 3 hours | Re-requestable after expiry |

---

### 15. Account Lock Threshold & Unlock Flow Not Documented
**Issue:** The docs say *"Account is locked due to too many failed attempts"* but don't specify:
- How many attempts trigger a lock?
- Is the lockout time-based or permanent?
- How does the user unlock their account?

**Recommendation:** Document:
- Lock threshold (e.g., 5 failed attempts)
- Lockout duration (e.g., 15 minutes, progressive backoff)
- Unlock mechanism (time-based auto-unlock, admin action, or password reset)

---

### 16. No Concurrent Session Policy Documented
**Issue:** Can a user log in from multiple devices simultaneously? Does a new login invalidate previous tokens? The docs are silent on this.

**Recommendation:** Document the expected behavior:
- **Option A:** Unlimited concurrent sessions (current implicit behavior)
- **Option B:** Single session — new login invalidates previous refresh tokens
- **Option C:** Configurable max sessions (e.g., 5 devices)

---

### 17. Verify Email Race Condition
**Endpoint:** `POST /verify-email`  
**Issue:** Two concurrent verification requests with the same valid token could both pass validation before either commits. Both would set `status = VERIFIED`, which is harmless, but could result in two WelcomeEvents being published to Kafka → duplicate welcome emails.

**Recommendation:** Use optimistic locking (`@Version` on `UserVerification`) or a database-level `WHERE status = 'PENDING'` condition in the update query to ensure only one request succeeds.

---

### 18. Refresh Token Race Condition (Undocumented Behavior)
**Endpoint:** `POST /refresh-token`  
**Issue:** If two concurrent requests use the same refresh token (e.g., two browser tabs), the first deletes the old JTI and the second fails with `401`. While this is actually good for theft detection, it's not documented and causes poor UX.

**Recommendation:**
- Document this behavior explicitly.
- The frontend interceptor already handles this well (queuing concurrent requests behind a refresh lock), but the API docs should note: *"Concurrent refresh requests with the same token will result in only the first succeeding. Clients should serialize refresh requests."*

---

### 19. No `Retry-After` Header on Rate Limit / Account Lock Errors
**Endpoints:** `/login`, `/resend-verification-email`  
**Issue:** When an account is locked or resend is rate-limited, the response doesn't include a `Retry-After` header, forcing clients to parse error messages to determine when to retry.

**Recommendation:** Include `Retry-After: <seconds>` header on:
- `403 Forbidden` (account locked) → seconds until unlock
- `400 Bad Request` (resend cooldown) → seconds until resend allowed
- `429 Too Many Requests` (rate limit) → seconds until rate limit resets

---

### 20. Missing Machine-Readable Error Codes
**Issue:** Errors rely on HTTP status + human-readable messages. The frontend `transformError()` method has to **parse message strings** to determine error types:
```typescript
if (message.toLowerCase().includes('disabled') ||
    message.toLowerCase().includes('account is disabled')) {
  code = AuthErrorCodes.ACCOUNT_DISABLED;
}
```
This is fragile — any backend message change breaks the frontend.

**Recommendation:** Add a machine-readable `errorCode` field to error responses:
```json
{
  "statusCode": 400,
  "status": "error",
  "message": "Account is disabled. Please verify your email.",
  "errorCode": "ACCOUNT_DISABLED",
  "data": null
}
```
Standardized codes: `INVALID_CREDENTIALS`, `ACCOUNT_DISABLED`, `ACCOUNT_LOCKED`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `EMAIL_ALREADY_VERIFIED`, `USER_NOT_FOUND`, `RATE_LIMITED`

---

### 21. Kafka Event Failure Handling Not Documented
**Issue:** After email verification, a `WelcomeEvent` is published to Kafka. What happens if Kafka is unavailable? Is the verification still committed? The docs mention `afterCommit` but don't document failure semantics.

**Recommendation:** Document:
- Events are published **after** transaction commit (so verification is never rolled back due to Kafka failure).
- Kafka events use at-least-once delivery (potential duplicate emails).
- Failed events should be retried via a dead-letter queue (DLQ).

---

### 22. Login Attempt Decay / Expiry Not Documented
**Issue:** Failed login attempts are tracked and reset on success, but there's no mention of time-based decay. A user who fails 4 of 5 attempts, then tries again 6 months later, would get locked out on the first failure.

**Recommendation:** Implement and document a sliding window for failed attempts (e.g., only count attempts from the last 30 minutes).

---

## 🔵 Low — Frontend-Specific Observations

### 23. `LoginRequest` Has `rememberMe` Not in API Spec
The frontend `LoginRequest` model has `rememberMe?: boolean`, but the backend `LoginRequestDto` only has `email` and `password`. The `rememberMe` flag is purely a frontend concern (localStorage vs sessionStorage) — this is fine, but worth noting for API alignment.

---

### 24. Frontend `transformError()` Is Fragile
As noted in #20, the frontend parses error messages via string matching. This should be refactored once the backend provides machine-readable error codes:
```typescript
// Current (fragile):
if (message.toLowerCase().includes('disabled')) {  }

// Recommended (robust):
if (error.error?.errorCode === 'ACCOUNT_DISABLED') {  }
```

---

### 25. `verifyEmail()` Returns `PasswordResetResponse` Type
In `auth.service.ts`, the `verifyEmail()` method returns `Promise<PasswordResetResponse>` — an incorrect type reuse. It should have its own response type or use a generic `ApiResponse` type.

---

## 📊 Summary & Priority Matrix

| #  | Issue                                   | Severity    | Effort | Priority |
|----|-----------------------------------------|-------------|--------|----------|
| 1  | Refresh token not invalidated on logout | 🔴 Critical | Low    | **P0**   |
| 2  | Soft-deleted account reactivation       | 🔴 Critical | Low    | **P0**   |
| 3  | User enumeration via error codes        | 🔴 Critical | Low    | **P1**   |
| 4  | Token storage in localStorage           | 🔴 Critical | High   | **P1**   |
| 5  | Resend verification leaks TTL           | 🔴 Critical | Low    | **P1**   |
| 6  | Wrong HTTP status codes on login        | 🟠 High     | Medium | **P1**   |
| 7  | No absolute refresh token expiry        | 🟠 High     | Medium | **P1**   |
| 8  | Login response mixes tokens & profile   | 🟠 High     | Medium | **P2**   |
| 9  | No "logout all devices"                 | 🟠 High     | Medium | **P2**   |
| 10 | Subscription re-assignment on login     | 🟠 High     | Low    | **P2**   |
| 11 | Inconsistent response DTOs              | 🟠 High     | Medium | **P2**   |
| 12 | Logout should return 204                | 🟠 High     | Low    | **P3**   |
| 13 | No rate limiting documented             | 🟡 Medium   | Medium | **P2**   |
| 14 | Token lifetimes not documented          | 🟡 Medium   | Low    | **P2**   |
| 15 | Lock threshold undocumented             | 🟡 Medium   | Low    | **P2**   |
| 16 | Concurrent session policy               | 🟡 Medium   | Low    | **P2**   |
| 17 | Verify email race condition             | 🟡 Medium   | Medium | **P3**   |
| 18 | Refresh race condition undocumented     | 🟡 Medium   | Low    | **P3**   |
| 19 | No `Retry-After` header                 | 🟡 Medium   | Low    | **P3**   |
| 20 | No machine-readable error codes         | 🟡 Medium   | Medium | **P2**   |
| 21 | Kafka failure handling undocumented     | 🟡 Medium   | Low    | **P3**   |
| 22 | Login attempt decay undocumented        | 🟡 Medium   | Low    | **P3**   |
| 23 | `rememberMe` not in API spec            | 🔵 Low      | None   | **P4**   |
| 24 | Frontend `transformError()` fragile     | 🔵 Low      | Medium | **P3**   |
| 25 | Wrong return type for `verifyEmail()`   | 🔵 Low      | Low    | **P4**   |

---

## Recommended Action Plan

**Phase 1 — Immediate (Security Fixes):**
1. Invalidate refresh token on logout (#1)
2. Block login for soft-deleted accounts (#2)
3. Unify error responses to prevent user enumeration (#3, #5)
4. Add machine-readable error codes to all error responses (#20)

**Phase 2 — Short-term (API Design):**
5. Fix HTTP status codes (#6)
6. Implement absolute session lifetime (#7)
7. Add rate limiting with `Retry-After` headers (#13, #19)
8. Document token lifetimes, lock thresholds, concurrent session policy (#14, #15, #16)

**Phase 3 — Medium-term (Architecture):**
9. Separate login response from profile data (#8)
10. Standardize token response DTOs (#11)
11. Add "logout all devices" endpoint (#9)
12. Move refresh token to HttpOnly cookie (#4)
13. Fix subscription assignment logic (#10)

**Phase 4 — Maintenance:**
14. Fix verify-email race condition (#17)
15. Document Kafka failure semantics (#21)
16. Frontend cleanup (#24, #25)
