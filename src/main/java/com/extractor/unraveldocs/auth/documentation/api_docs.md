# Auth Package — API Documentation

> **Base URL:** `/api/v1/auth`  
> **Package:** `com.extractor.unraveldocs.auth`  
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
   - [Generate Password](#1-generate-password)
   - [Sign Up](#2-sign-up)
   - [Login](#3-login)
   - [Verify Email](#4-verify-email)
   - [Resend Verification Email](#5-resend-verification-email)
   - [Refresh Token](#6-refresh-token)
   - [Logout](#7-logout)
5. [Service Layer](#service-layer)
   - [AuthService (Facade)](#authservice-facade)
   - [SignupUserImpl](#signupuserimpl)
   - [LoginUserImpl](#loginuserimpl)
   - [EmailVerificationImpl](#emailverificationimpl)
   - [GeneratePasswordImpl](#generatepasswordimpl)
   - [RefreshTokenImpl](#refreshtokenimpl)
   - [CustomUserDetailsService](#customuserdetailsservice)
6. [Events & Async Processing](#events--async-processing)
   - [UserRegisteredEvent](#userregisteredevent)
   - [WelcomeEvent](#welcomeevent)
   - [UserRegisteredEventHandler](#userregisteredeventhandler)
7. [Security & Token Strategy](#security--token-strategy)
8. [Validation Rules](#validation-rules)
9. [Error Reference](#error-reference)
10. [Flow Diagrams](#flow-diagrams)

---

## Overview

The **Auth** package handles all authentication and authorization concerns for the UnravelDocs API. It covers the full user lifecycle from registration through email verification, login, session management (JWT-based access + rolling refresh tokens), and logout.

Key capabilities:

| Feature             | Description                                                                                                                  |
|---------------------|------------------------------------------------------------------------------------------------------------------------------|
| User Registration   | Creates a new user account, assigns a default subscription, grants sign-up credits, and sends a verification email via Kafka |
| Email Verification  | Token-based verification with configurable TTL (default 3 hours)                                                             |
| Login               | Authenticates credentials, issues JWT access + refresh tokens, tracks login attempts                                         |
| Token Refresh       | Rolling refresh token strategy — old refresh token is invalidated and a new pair is issued                                   |
| Logout              | Blacklists the current access token; clears the Spring Security context                                                      |
| Password Generation | Utility endpoint to generate cryptographically strong passwords of configurable length                                       |

---

## Package Structure

```
auth/
├── components/
│   └── UserRegisteredEventHandler.java      # Kafka event handler — sends verification email
├── config/
│   └── RoleEnumConverter.java               # JPA converter for Role enum
├── controller/
│   └── AuthController.java                  # REST controller — maps HTTP requests to AuthService
├── datamodel/
│   ├── Role.java                            # Enum: USER | MODERATOR | ADMIN | SUPER_ADMIN
│   └── VerifiedStatus.java                  # Enum: PENDING | VERIFIED | EXPIRED | FAILED
├── dto/
│   ├── LoginData.java                       # Login response payload
│   ├── PasswordMatches.java                 # Custom constraint annotation
│   ├── PasswordMatchesValidator.java        # Validator for password == confirmPassword
│   ├── RefreshLoginData.java                # Refresh token response payload
│   ├── SignupData.java                      # Signup response payload
│   └── request/
│       ├── EmailVerificationRequestDto.java # Verify email request
│       ├── GeneratePasswordDto.java         # Password generation request
│       ├── LoginRequestDto.java             # Login request
│       ├── RefreshTokenRequest.java         # Token refresh request
│       ├── ResendEmailVerificationDto.java  # Resend verification email request
│       ├── SignupRequestDto.java            # User registration request
│       └── VerifyEmailDto.java              # (alias/internal use)
├── events/
│   ├── UserRegisteredEvent.java             # Event payload published after user registration
│   └── WelcomeEvent.java                    # Event payload published after email verification
├── impl/
│   ├── EmailVerificationImpl.java           # Email verification business logic
│   ├── GeneratePasswordImpl.java            # Password generation logic
│   ├── LoginUserImpl.java                   # Login business logic
│   ├── RefreshTokenImpl.java                # Token refresh and logout logic
│   └── SignupUserImpl.java                  # User registration business logic
├── interfaces/
│   ├── EmailVerificationService.java        # Contract for email verification
│   ├── GeneratePasswordService.java         # Contract for password generation
│   ├── LoginUserService.java                # Contract for login
│   ├── RefreshTokenService.java             # Contract for token refresh / logout
│   ├── SignupUserService.java               # Contract for user registration
│   └── UserEntityById.java                  # Contract for loading User entity by ID
├── model/
│   └── UserVerification.java               # JPA entity — stores verification tokens
├── repository/
│   └── UserVerificationRepository.java      # JPA repository for UserVerification
├── service/
│   ├── AuthService.java                     # Facade — delegates to individual service implementations
│   └── CustomUserDetailsService.java        # Spring Security UserDetailsService integration
└── documentation/
    └── api_docs.md                          # This file
```

---

## Data Models

### Enums

#### `Role`
**Package:** `com.extractor.unraveldocs.auth.datamodel`

Defines the authorization roles assignable to a user account.

| Value         | String Representation | Description                            |
|---------------|-----------------------|----------------------------------------|
| `USER`        | `"user"`              | Default role for registered users      |
| `MODERATOR`   | `"moderator"`         | Elevated content-management privileges |
| `ADMIN`       | `"admin"`             | Administrative access                  |
| `SUPER_ADMIN` | `"super_admin"`       | Full platform access                   |

**Helper methods:**

| Method          | Signature                             | Description                                                      |
|-----------------|---------------------------------------|------------------------------------------------------------------|
| `fromString`    | `static Role fromString(String role)` | Case-insensitive lookup; throws `BadRequestException` if unknown |
| `getValidRoles` | `static String[] getValidRoles()`     | Returns all role string values                                   |

---

#### `VerifiedStatus`
**Package:** `com.extractor.unraveldocs.auth.datamodel`

Tracks the state of a user's email verification lifecycle.

| Value      | String Representation | Description                                   |
|------------|-----------------------|-----------------------------------------------|
| `PENDING`  | `"pending"`           | Verification email sent; awaiting user action |
| `VERIFIED` | `"verified"`          | Email successfully verified                   |
| `EXPIRED`  | `"expired"`           | Token passed its TTL without being used       |
| `FAILED`   | `"failed"`            | Verification process encountered an error     |

---

### Entities

#### `UserVerification`
**Package:** `com.extractor.unraveldocs.auth.model`  
**Table:** `user_verification`

Stores all verification and password-reset token data for a `User`. Has a one-to-one relationship with the `User` entity.

| Column                         | Type                   | Nullable | Description                                  |
|--------------------------------|------------------------|----------|----------------------------------------------|
| `id`                           | `String` (UUID)        | No       | Primary key                                  |
| `user_id`                      | `String` (FK → `User`) | No       | Owning user                                  |
| `emailVerified`                | `boolean`              | No       | `true` once email verification completes     |
| `emailVerificationToken`       | `String`               | Yes      | Hex token sent in the verification email     |
| `status`                       | `VerifiedStatus`       | No       | Current verification state                   |
| `emailVerificationTokenExpiry` | `OffsetDateTime`       | Yes      | Expiry timestamp (typically `now + 3 hours`) |
| `passwordResetToken`           | `String`               | Yes      | Token for password reset flow                |
| `passwordResetTokenExpiry`     | `OffsetDateTime`       | Yes      | Expiry for password reset token              |
| `deletedAt`                    | `OffsetDateTime`       | Yes      | Soft-delete timestamp                        |
| `createdAt`                    | `OffsetDateTime`       | No       | Auto-set on insert                           |

**Indexes:** `email_verification_token`, `password_reset_token`

---

### Request DTOs

#### `SignupRequestDto`
**Package:** `com.extractor.unraveldocs.auth.dto.request`

| Field                  | Type      | Required | Constraints                                                                         | Example                 |
|------------------------|-----------|----------|-------------------------------------------------------------------------------------|-------------------------|
| `firstName`            | `String`  | ✅        | 2–80 characters                                                                     | `"John"`                |
| `lastName`             | `String`  | ✅        | 2–80 characters                                                                     | `"Doe"`                 |
| `email`                | `String`  | ✅        | Valid email; max 100 chars                                                          | `"johndoe@example.com"` |
| `password`             | `String`  | ✅        | Min 8 chars; must contain uppercase, lowercase, digit, and special char (`@$!%*?&`) | `"P@ssw0rd123"`         |
| `confirmPassword`      | `String`  | ✅        | Must match `password` (validated by `@PasswordMatches`)                             | `"P@ssw0rd123"`         |
| `acceptTerms`          | `Boolean` | ✅        | Must be `true`                                                                      | `true`                  |
| `subscribeToMarketing` | `Boolean` | ❌        | Defaults to `false`                                                                 | `false`                 |
| `profession`           | `String`  | ❌        | Max 100 characters                                                                  | `"Software Engineer"`   |
| `organization`         | `String`  | ❌        | Max 100 characters                                                                  | `"Tech Company"`        |
| `country`              | `String`  | ✅        | —                                                                                   | `"USA"`                 |

> ⚠️ **Security note:** The `toString()` override on this record redacts `email`, `password`, and `confirmPassword` from logs.

---

#### `LoginRequestDto`
**Package:** `com.extractor.unraveldocs.auth.dto.request`

| Field      | Type     | Required | Constraints        | Example               |
|------------|----------|----------|--------------------|-----------------------|
| `email`    | `String` | ✅        | Valid email format | `"john-doe@test.com"` |
| `password` | `String` | ✅        | Not blank          | `"P@ssw0rd123"`       |

---

#### `EmailVerificationRequestDto`
**Package:** `com.extractor.unraveldocs.auth.dto.request`

| Field   | Type     | Required | Constraints        | Example              |
|---------|----------|----------|--------------------|----------------------|
| `email` | `String` | ✅        | Valid email format | `"test@example.com"` |
| `token` | `String` | ✅        | Not null           | `"fc2dec961bfb..."`  |

---

#### `ResendEmailVerificationDto`
**Package:** `com.extractor.unraveldocs.auth.dto.request`

| Field   | Type     | Required | Constraints            | Example               |
|---------|----------|----------|------------------------|-----------------------|
| `email` | `String` | ✅        | Valid email; not blank | `"john-doe@test.com"` |

---

#### `RefreshTokenRequest`
**Package:** `com.extractor.unraveldocs.auth.dto.request`

| Field          | Type     | Required | Constraints | Example         |
|----------------|----------|----------|-------------|-----------------|
| `refreshToken` | `String` | ✅        | Not blank   | `"eyJhbGci..."` |

---

#### `GeneratePasswordDto`
**Package:** `com.extractor.unraveldocs.auth.dto.request`

| Field            | Type     | Required | Constraints                                    | Example       |
|------------------|----------|----------|------------------------------------------------|---------------|
| `passwordLength` | `String` | ✅        | Numeric string; value must be ≥ 8 when parsed  | `"12"`        |
| `excludedChars`  | `String` | ❌        | Characters to omit from the generated password | `"^=Av3r@ge"` |

---

### Response DTOs

#### `SignupData`
Returned inside `UnravelDocsResponse<SignupData>` on successful registration.

| Field                 | Type             | Description                                   |
|-----------------------|------------------|-----------------------------------------------|
| `id`                  | `String`         | User UUID                                     |
| `profilePicture`      | `String`         | URL of the profile picture (nullable)         |
| `firstName`           | `String`         | Capitalized first name                        |
| `lastName`            | `String`         | Capitalized last name                         |
| `email`               | `String`         | Lowercase email address                       |
| `role`                | `Role`           | Assigned role (always `USER` on registration) |
| `lastLogin`           | `OffsetDateTime` | Null at registration                          |
| `isActive`            | `boolean`        | `false` until email is verified               |
| `isVerified`          | `boolean`        | `false` until email is verified               |
| `termsAccepted`       | `boolean`        | Reflects `acceptTerms` from request           |
| `marketingOptIn`      | `boolean`        | Reflects `subscribeToMarketing` from request  |
| `isPlatformAdmin`     | `boolean`        | `false` by default                            |
| `isOrganizationAdmin` | `boolean`        | `false` by default                            |
| `country`             | `String`         | Country from request                          |
| `profession`          | `String`         | Profession from request (nullable)            |
| `organization`        | `String`         | Organization from request (nullable)          |
| `createdAt`           | `OffsetDateTime` | Account creation timestamp                    |
| `updatedAt`           | `OffsetDateTime` | Last update timestamp                         |

---

#### `LoginData`
Returned inside `UnravelDocsResponse<LoginData>` on successful login.

| Field          | Type             | Description                           |
|----------------|------------------|---------------------------------------|
| `id`           | `String`         | User UUID                             |
| `firstName`    | `String`         | User first name                       |
| `lastName`     | `String`         | User last name                        |
| `email`        | `String`         | User email                            |
| `role`         | `Role`           | User role                             |
| `lastLogin`    | `OffsetDateTime` | Updated to `now()` on each login      |
| `isActive`     | `boolean`        | Account active status                 |
| `isVerified`   | `boolean`        | Email verification status             |
| `accessToken`  | `String`         | Short-lived JWT access token (Bearer) |
| `refreshToken` | `String`         | Long-lived JWT refresh token          |
| `createdAt`    | `OffsetDateTime` | Account creation timestamp            |
| `updatedAt`    | `OffsetDateTime` | Last update timestamp                 |

---

#### `RefreshLoginData`
Returned inside `UnravelDocsResponse<RefreshLoginData>` on successful token refresh.

| Field                  | Type     | Description                              |
|------------------------|----------|------------------------------------------|
| `id`                   | `String` | User UUID                                |
| `email`                | `String` | User email                               |
| `accessToken`          | `String` | Newly issued JWT access token            |
| `refreshToken`         | `String` | Newly issued JWT refresh token (rolling) |
| `tokenType`            | `String` | Always `"Bearer"`                        |
| `accessExpirationInMs` | `Long`   | Access token TTL in milliseconds         |

---

## Endpoints

All endpoints are prefixed with `/api/v1/auth`.

---

### 1. Generate Password

| Property          | Value                            |
|-------------------|----------------------------------|
| **Method**        | `POST`                           |
| **Path**          | `/api/v1/auth/generate-password` |
| **Auth Required** | No                               |
| **Content-Type**  | `application/json`               |

**Request Body:** `GeneratePasswordDto`

```json
{
  "passwordLength": "16",
  "excludedChars": "O0l1"
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Password successfully generated.",
  "data": {
    "generatedPassword": "xK#9mQr$vL&2Zp!n"
  }
}
```

**Error Responses**

| Status            | Condition                                                    |
|-------------------|--------------------------------------------------------------|
| `400 Bad Request` | `passwordLength` is missing or parses to a value less than 8 |

---

### 2. Sign Up

| Property          | Value                 |
|-------------------|-----------------------|
| **Method**        | `POST`                |
| **Path**          | `/api/v1/auth/signup` |
| **Auth Required** | No                    |
| **Content-Type**  | `application/json`    |

**Request Body:** `SignupRequestDto`

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "johndoe@example.com",
  "password": "P@ssw0rd123",
  "confirmPassword": "P@ssw0rd123",
  "acceptTerms": true,
  "subscribeToMarketing": false,
  "profession": "Software Engineer",
  "organization": "Tech Company",
  "country": "USA"
}
```

**Success Response — `201 Created`**

```json
{
  "statusCode": 201,
  "status": "success",
  "message": "User registered successfully",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "profilePicture": null,
    "firstName": "John",
    "lastName": "Doe",
    "email": "johndoe@example.com",
    "role": "user",
    "lastLogin": null,
    "isActive": false,
    "isVerified": false,
    "termsAccepted": true,
    "marketingOptIn": false,
    "isPlatformAdmin": false,
    "isOrganizationAdmin": false,
    "country": "USA",
    "profession": "Software Engineer",
    "organization": "Tech Company",
    "createdAt": "2026-02-26T10:00:00Z",
    "updatedAt": "2026-02-26T10:00:00Z"
  }
}
```

**Side Effects**

- A `UserVerification` record is created with status `PENDING` and a 3-hour token TTL.
- A `LoginAttempts` record is initialized for the user.
- A default subscription is assigned via `AssignSubscriptionService`.
- A `UserRegisteredEvent` is published to Kafka **after transaction commit**, which triggers a verification email.
- The user is indexed in Elasticsearch (if the indexing service is available).
- A `WELCOME` push notification is dispatched.
- Sign-up bonus credits are granted via `CreditBalanceService`.

**Error Responses**

| Status            | Condition                                                          |
|-------------------|--------------------------------------------------------------------|
| `400 Bad Request` | Validation failure (missing/invalid fields, passwords don't match) |
| `400 Bad Request` | Password is the same as the email address                          |
| `409 Conflict`    | Email already registered                                           |

---

### 3. Login

| Property          | Value                |
|-------------------|----------------------|
| **Method**        | `POST`               |
| **Path**          | `/api/v1/auth/login` |
| **Auth Required** | No                   |
| **Content-Type**  | `application/json`   |

**Request Body:** `LoginRequestDto`

```json
{
  "email": "johndoe@example.com",
  "password": "P@ssw0rd123"
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "User logged in successfully",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "firstName": "John",
    "lastName": "Doe",
    "email": "johndoe@example.com",
    "role": "user",
    "lastLogin": "2026-02-26T10:05:00Z",
    "isActive": true,
    "isVerified": true,
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
    "createdAt": "2026-02-26T10:00:00Z",
    "updatedAt": "2026-02-26T10:05:00Z"
  }
}
```

**Side Effects**

- `lastLogin` is updated to `OffsetDateTime.now()`.
- Failed login attempts are tracked; blocked users are rejected before authentication.
- Successful login resets failed login attempt counter.
- Refresh token JTI is stored in the token store for validation.
- Soft-deleted accounts are reactivated on successful login.
- A default subscription is assigned if the user has none.

**Error Responses**

| Status            | Condition                                         |
|-------------------|---------------------------------------------------|
| `400 Bad Request` | Invalid email or password                         |
| `400 Bad Request` | Account is disabled (email not verified)          |
| `403 Forbidden`   | Account is locked due to too many failed attempts |

---

### 4. Verify Email

| Property          | Value                       |
|-------------------|-----------------------------|
| **Method**        | `POST`                      |
| **Path**          | `/api/v1/auth/verify-email` |
| **Auth Required** | No                          |
| **Content-Type**  | `application/json`          |

**Request Body:** `EmailVerificationRequestDto`

```json
{
  "email": "johndoe@example.com",
  "token": "fc2dec961bfbcfb224e3d56b6516a2986904323c6520db012c2516e1e8170a3b4559772399831afc"
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Email verified successfully",
  "data": null
}
```

**Side Effects**

- `UserVerification.status` set to `VERIFIED`, token fields cleared.
- `User.isVerified` set to `true`, `User.isActive` set to `true`.
- A `WelcomeEvent` is published to Kafka **after transaction commit**, which triggers a welcome email.

**Error Responses**

| Status            | Condition                                                |
|-------------------|----------------------------------------------------------|
| `400 Bad Request` | User is already verified                                 |
| `400 Bad Request` | Token is invalid or does not match stored token          |
| `400 Bad Request` | Token has expired (`VerifiedStatus` is set to `EXPIRED`) |
| `404 Not Found`   | No user found for the given email                        |

---

### 5. Resend Verification Email

| Property          | Value                                    |
|-------------------|------------------------------------------|
| **Method**        | `POST`                                   |
| **Path**          | `/api/v1/auth/resend-verification-email` |
| **Auth Required** | No                                       |
| **Content-Type**  | `application/json`                       |

**Request Body:** `ResendEmailVerificationDto`

```json
{
  "email": "johndoe@example.com"
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "A new verification email has been sent successfully.",
  "data": null
}
```

**Side Effects**

- A new verification token is generated with a fresh 3-hour TTL.
- `UserVerification.status` reset to `PENDING`.
- A `UserRegisteredEvent` is published to Kafka **after transaction commit** to re-send the verification email.

**Error Responses**

| Status            | Condition                                                                                              |
|-------------------|--------------------------------------------------------------------------------------------------------|
| `400 Bad Request` | User is already verified                                                                               |
| `400 Bad Request` | A valid (non-expired) token already exists — response includes time remaining before resend is allowed |
| `404 Not Found`   | No user found for the given email                                                                      |

---

### 6. Refresh Token

| Property          | Value                                      |
|-------------------|--------------------------------------------|
| **Method**        | `POST`                                     |
| **Path**          | `/api/v1/auth/refresh-token`               |
| **Auth Required** | No (but a valid refresh token is required) |
| **Content-Type**  | `application/json`                         |

**Request Body:** `RefreshTokenRequest`

```json
{
  "refreshToken": "eyJhbGciOiJSUzI1NiJ9..."
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Token refreshed successfully",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "johndoe@example.com",
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "accessExpirationInMs": 900000
  }
}
```

**Rolling Refresh Token Strategy**

1. Incoming refresh token is validated (signature + JTI store + `type == "REFRESH"`).
2. Old refresh token JTI is deleted from the store.
3. A new access token **and** a new refresh token are issued.
4. The new refresh token JTI is stored.

**Error Responses**

| Status             | Condition                                                       |
|--------------------|-----------------------------------------------------------------|
| `401 Unauthorized` | Token is missing, malformed, expired, or JTI not found in store |
| `401 Unauthorized` | Token `type` claim is not `"REFRESH"`                           |
| `401 Unauthorized` | User account is not active or not verified                      |

---

### 7. Logout

| Property          | Value                                       |
|-------------------|---------------------------------------------|
| **Method**        | `POST`                                      |
| **Path**          | `/api/v1/auth/logout`                       |
| **Auth Required** | Yes — `Authorization: Bearer <accessToken>` |
| **Content-Type**  | —                                           |

**Request Body:** None (token is read from the `Authorization` header)

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Logged out successfully",
  "data": null
}
```

**Side Effects**

- The access token JTI is added to the token blacklist for the duration of its remaining TTL.
- The Spring Security context is cleared.

> ℹ️ The associated refresh token is **not** automatically invalidated in the current implementation. This is noted as a TODO in `RefreshTokenImpl`.

**Error Responses**

| Status             | Condition                                                                                             |
|--------------------|-------------------------------------------------------------------------------------------------------|
| `401 Unauthorized` | No valid `Authorization` header provided (handled by the security filter chain before the controller) |

---

## Service Layer

### `AuthService` (Facade)
**Package:** `com.extractor.unraveldocs.auth.service`

A thin orchestration layer that delegates every operation to the appropriate interface implementation. Controllers depend only on `AuthService`, keeping them decoupled from implementation details.

| Method                                                | Delegates To               | Description                 |
|-------------------------------------------------------|----------------------------|-----------------------------|
| `registerUser(SignupRequestDto)`                      | `SignupUserService`        | User registration           |
| `loginUser(LoginRequestDto)`                          | `LoginUserService`         | User login                  |
| `verifyEmail(String, String)`                         | `EmailVerificationService` | Email verification          |
| `resendEmailVerification(ResendEmailVerificationDto)` | `EmailVerificationService` | Resend verification email   |
| `generatePassword(GeneratePasswordDto)`               | `GeneratePasswordService`  | Password generation         |
| `refreshToken(RefreshTokenRequest)`                   | `RefreshTokenService`      | Token refresh               |
| `logout(HttpServletRequest)`                          | `RefreshTokenService`      | Logout / token invalidation |

---

### `SignupUserImpl`
**Package:** `com.extractor.unraveldocs.auth.impl`  
**Implements:** `SignupUserService`  
**Transactional:** Yes (`@Transactional`)

**Registration sequence:**

```
1. Check email uniqueness → ConflictException if duplicate
2. Check password ≠ email → BadRequestException if same
3. Build User entity (password BCrypt-encoded, role = USER, isActive = false, isVerified = false)
4. Build UserVerification entity (token TTL = 3 hours, status = PENDING)
5. Build LoginAttempts entity
6. Assign default subscription (AssignSubscriptionService)
7. Persist User (cascades to UserVerification, LoginAttempts, UserSubscription)
8. After transaction commit:
   a. Publish UserRegisteredEvent → Kafka → verification email
   b. Index user in Elasticsearch (optional)
   c. Send WELCOME push notification
   d. Grant sign-up bonus credits (CreditBalanceService)
9. Return SignupData response (HTTP 201)
```

---

### `LoginUserImpl`
**Package:** `com.extractor.unraveldocs.auth.impl`  
**Implements:** `LoginUserService`

**Login sequence:**

```
1. Look up user by email (optional — used for block check)
2. Check if user is blocked (LoginAttemptsService)
3. Authenticate via Spring AuthenticationManager
   ├─ BadCredentialsException → record failed attempt → BadRequestException
   ├─ DisabledException → BadRequestException (email not verified)
   ├─ LockedException → ForbiddenException
   └─ Other AuthenticationException → record failed attempt → BadRequestException
4. Reset failed login attempts
5. Generate JWT access token + refresh token (JwtTokenProvider)
6. Store refresh token JTI (RefreshTokenService)
7. Restore soft-deleted account if applicable
8. Update lastLogin = now()
9. Assign default subscription if missing
10. Return LoginData response (HTTP 200)
```

---

### `EmailVerificationImpl`
**Package:** `com.extractor.unraveldocs.auth.impl`  
**Implements:** `EmailVerificationService`  
**Transactional:** Yes (`@Transactional`)

Handles two operations:

**`verifyEmail(String email, String token)`**
```
1. Find user by email → NotFoundException if not found
2. Check if already verified → BadRequestException
3. Validate token matches stored token → BadRequestException if mismatch
4. Check token expiry → set status to EXPIRED and throw BadRequestException if expired
5. Clear token, set emailVerified = true, status = VERIFIED
6. Set user.isVerified = true, user.isActive = true
7. Persist changes
8. After transaction commit: publish WelcomeEvent → Kafka → welcome email
```

**`resendEmailVerification(ResendEmailVerificationDto)`**
```
1. Find user by email → NotFoundException if not found
2. Check if already verified → BadRequestException
3. If a valid (non-expired) token exists → BadRequestException with time-remaining message
4. Generate new token with 3-hour TTL, reset status to PENDING
5. Persist changes
6. After transaction commit: publish UserRegisteredEvent → Kafka → verification email
```

---

### `GeneratePasswordImpl`
**Package:** `com.extractor.unraveldocs.auth.impl`  
**Implements:** `GeneratePasswordService`

**Logic:**
```
1. Parse passwordLength (String → int)
2. Validate length ≥ 8 → BadRequestException if not
3. If excludedChars provided → call UserLibrary.generateStrongPassword(length, excludedCharsArray)
4. Otherwise → call UserLibrary.generateStrongPassword(length)
5. Return GeneratePasswordResponse (HTTP 200)
```

---

### `RefreshTokenImpl`
**Package:** `com.extractor.unraveldocs.auth.impl`  
**Implements:** `RefreshTokenService` (auth.interfaces)

**`refreshToken(RefreshTokenRequest)`**
```
1. Extract JTI from refresh token
2. Validate: JTI not null + token signature valid + JTI present in store → UnauthorizedException
3. Validate token type == "REFRESH" → UnauthorizedException
4. Resolve userId from store → load User entity
5. Check user is verified and active → UnauthorizedException
6. Generate new access token
7. Invalidate old refresh JTI, generate new refresh token, store new JTI (rolling refresh)
8. Return RefreshLoginData (HTTP 200)
```

**`logout(HttpServletRequest)`**
```
1. Extract Bearer token from Authorization header
2. Validate token signature
3. Extract JTI and expiry
4. Blacklist JTI for remaining TTL (TokenBlacklistService)
5. Clear Spring SecurityContext
6. Return success (HTTP 200)
```

---

### `CustomUserDetailsService`
**Package:** `com.extractor.unraveldocs.auth.service`  
**Implements:** `UserDetailsService`, `UserEntityById`

| Method                              | Description                                                                                              |
|-------------------------------------|----------------------------------------------------------------------------------------------------------|
| `loadUserByUsername(String email)`  | Used by Spring Security's `AuthenticationManager`; throws `UsernameNotFoundException` if email not found |
| `loadUserEntityById(String userId)` | Loads the full `User` entity by ID; used by `RefreshTokenImpl`; throws `NotFoundException` if not found  |

---

## Events & Async Processing

The auth package publishes events to Kafka after successful database commits using `TransactionSynchronizationManager.registerSynchronization`. This guarantees that events are only emitted when the database state has been committed, preventing ghost notifications on rollback.

### `UserRegisteredEvent`
**Package:** `com.extractor.unraveldocs.auth.events`  
**Published by:** `SignupUserImpl`, `EmailVerificationImpl` (resend flow)

| Field               | Type     | Description                                   |
|---------------------|----------|-----------------------------------------------|
| `email`             | `String` | Recipient email                               |
| `firstName`         | `String` | User's first name                             |
| `lastName`          | `String` | User's last name                              |
| `verificationToken` | `String` | Hex verification token                        |
| `expiration`        | `String` | Human-readable TTL string (e.g., `"3 hours"`) |

---

### `WelcomeEvent`
**Package:** `com.extractor.unraveldocs.auth.events`  
**Published by:** `EmailVerificationImpl` (after successful verification)

| Field       | Type     | Description       |
|-------------|----------|-------------------|
| `email`     | `String` | Recipient email   |
| `firstName` | `String` | User's first name |
| `lastName`  | `String` | User's last name  |

---

### `UserRegisteredEventHandler`
**Package:** `com.extractor.unraveldocs.auth.components`  
**Implements:** `EventHandler<UserRegisteredEvent>`  
**Event Type:** `EventTypes.USER_REGISTERED`

Consumes `UserRegisteredEvent` from Kafka and triggers the transactional email pipeline:

1. Builds template variables: `firstName`, `lastName`, `verificationToken`, `expiration`.
2. Calls `EmailMessageProducerService.queueEmail(...)` with template `"emailVerificationToken"`.
3. Logs success or failure with sanitized email (via `SanitizeLogging`).

---

## Security & Token Strategy

### JWT Tokens

The application uses **two JWT tokens** per session:

| Token         | Type Claim | Purpose                              | Storage                                        |
|---------------|------------|--------------------------------------|------------------------------------------------|
| Access Token  | `ACCESS`   | Authorizes API requests; short-lived | Client-side (in-memory / Authorization header) |
| Refresh Token | `REFRESH`  | Issues new access tokens; long-lived | Client-side + JTI stored server-side           |

- Tokens are generated and validated by `JwtTokenProvider`.
- Access token TTL is configurable; exposed via `getAccessExpirationInMs()`.
- On logout, the access token JTI is added to `TokenBlacklistService` (backed by Redis or equivalent) for its remaining lifetime.

### Rolling Refresh Tokens

Each call to `/refresh-token` invalidates the submitted refresh token and issues a fresh pair. This reduces the window for refresh token theft.

### Login Attempt Tracking

`LoginAttemptsService` tracks failed authentication attempts per user. Once a threshold is exceeded, the account is blocked and `loginAttemptsService.checkIfUserBlocked(user)` is called before every authentication attempt.

### Password Security

- Passwords are encoded using **BCrypt** (`PasswordEncoder`).
- Passwords must meet complexity requirements (uppercase + lowercase + digit + special character).
- Passwords may not equal the user's email address.
- Password confirmation is validated by the custom `@PasswordMatches` constraint, implemented in `PasswordMatchesValidator`.

---

## Validation Rules

| Rule                            | Applied To                            | Details                                      |
|---------------------------------|---------------------------------------|----------------------------------------------|
| `@NotNull` / `@NotBlank`        | Most fields                           | Standard null/empty checks                   |
| `@Email`                        | Email fields                          | RFC-compliant email format                   |
| `@Size`                         | Name, email, organization, profession | Character bounds                             |
| `@Pattern`                      | Email, password                       | Regex enforcement                            |
| `@AssertTrue`                   | `acceptTerms`                         | Must be `true`                               |
| `@PasswordMatches`              | `SignupRequestDto`                    | Class-level; `password` == `confirmPassword` |
| Password ≥ 8 chars + complexity | `SignupRequestDto`                    | `@Pattern.List` checks                       |
| `passwordLength` ≥ 8            | `GeneratePasswordDto`                 | Validated in service layer                   |

---

## Error Reference

All errors follow the standard `UnravelDocsResponse` error envelope. HTTP status codes used within the auth package:

| Exception Class            | HTTP Status                 | Common Trigger                                                     |
|----------------------------|-----------------------------|--------------------------------------------------------------------|
| `BadRequestException`      | `400 Bad Request`           | Validation failures, invalid credentials, duplicate password/email |
| `ConflictException`        | `409 Conflict`              | Email already registered                                           |
| `NotFoundException`        | `404 Not Found`             | User not found by email or ID                                      |
| `ForbiddenException`       | `403 Forbidden`             | Locked account                                                     |
| `UnauthorizedException`    | `401 Unauthorized`          | Invalid/expired tokens, unverified account                         |
| `TokenProcessingException` | `500 Internal Server Error` | JTI generation failure during login                                |

---

## Flow Diagrams

### Registration Flow

```
Client
  │
  ├─ POST /api/v1/auth/signup
  │
  ▼
AuthController
  │
  ▼
AuthService → SignupUserImpl
  │
  ├─ [Duplicate email?] ──► 409 Conflict
  ├─ [Password == email?] ─► 400 Bad Request
  │
  ├─ Create User + UserVerification (PENDING) + LoginAttempts
  ├─ Assign default subscription
  ├─ Save to DB (transaction)
  │
  └─ After commit:
       ├─ Publish UserRegisteredEvent → Kafka
       │     └─ UserRegisteredEventHandler → EmailMessageProducerService → Verification email sent
       ├─ Index user in Elasticsearch
       ├─ Send WELCOME push notification
       └─ Grant sign-up bonus credits
  │
  ▼
201 Created + SignupData
```

---

### Login Flow

```
Client
  │
  ├─ POST /api/v1/auth/login
  │
  ▼
AuthController → AuthService → LoginUserImpl
  │
  ├─ Look up user by email
  ├─ [User blocked?] ──────────────────────────► 403 Forbidden
  │
  ├─ AuthenticationManager.authenticate()
  │    ├─ BadCredentials → record attempt ──────► 400 Bad Request
  │    ├─ DisabledException ──────────────────── ► 400 Bad Request
  │    └─ LockedException ───────────────────── ► 403 Forbidden
  │
  ├─ Reset login attempts
  ├─ Generate access token + refresh token
  ├─ Store refresh token JTI
  ├─ Update lastLogin
  │
  ▼
200 OK + LoginData (accessToken + refreshToken)
```

---

### Email Verification Flow

```
Client
  │
  ├─ POST /api/v1/auth/verify-email { email, token }
  │
  ▼
AuthController → AuthService → EmailVerificationImpl
  │
  ├─ Find user by email ──► 404 Not Found
  ├─ [Already verified?] ─► 400 Bad Request
  ├─ [Invalid token?] ────► 400 Bad Request
  ├─ [Token expired?] ────► set EXPIRED, 400 Bad Request
  │
  ├─ Set user active + verified, clear token, set VERIFIED
  ├─ Save to DB (transaction)
  │
  └─ After commit:
       └─ Publish WelcomeEvent → Kafka → Welcome email sent
  │
  ▼
200 OK
```

---

### Token Refresh Flow

```
Client
  │
  ├─ POST /api/v1/auth/refresh-token { refreshToken }
  │
  ▼
AuthController → AuthService → RefreshTokenImpl
  │
  ├─ Validate JTI + signature + store presence ──► 401 Unauthorized
  ├─ Validate type == "REFRESH" ─────────────── ► 401 Unauthorized
  ├─ Load user, check verified + active ──────── ► 401 Unauthorized
  │
  ├─ Issue new access token
  ├─ Delete old refresh JTI
  ├─ Issue new refresh token, store new JTI   ← rolling refresh
  │
  ▼
200 OK + RefreshLoginData
```

---

### Logout Flow

```
Client
  │
  ├─ POST /api/v1/auth/logout
  │   Authorization: Bearer <accessToken>
  │
  ▼
AuthController → AuthService → RefreshTokenImpl.logout()
  │
  ├─ Extract + validate access token from header
  ├─ Extract JTI + expiry
  ├─ Blacklist JTI for remaining TTL
  ├─ Clear SecurityContext
  │
  ▼
200 OK
```

