# User Package — API Documentation

> **Base URL:** `/api/v1/user`  
> **Package:** `com.extractor.unraveldocs.user`  
> **Last Updated:** 2026-02-26

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Data Models](#data-models)
   - [User Entity](#user-entity)
   - [Request DTOs](#request-dtos)
   - [Response DTOs](#response-dtos)
4. [Endpoints](#endpoints)
   - [Get Current User Profile](#1-get-current-user-profile)
   - [Update User Profile](#2-update-user-profile)
   - [Upload Profile Picture](#3-upload-profile-picture)
   - [Delete Profile Picture](#4-delete-profile-picture)
   - [Forgot Password](#5-forgot-password)
   - [Reset Password](#6-reset-password)
   - [Change Password](#7-change-password)
   - [Delete User Account](#8-delete-user-account)
5. [Service Layer](#service-layer)
   - [UserService (Facade)](#userservice-facade)
   - [GetUserProfileImpl](#getuserprofileimpl)
   - [ProfileUpdateImpl](#profileupdateimpl)
   - [ProfilePictureImpl](#profilepictureimpl)
   - [PasswordResetImpl](#passwordresetimpl)
   - [ChangePasswordImpl](#changepasswordimpl)
   - [DeleteUserImpl](#deleteuserimpl)
6. [Repository](#repository)
7. [Events & Async Processing](#events--async-processing)
   - [Published Events](#published-events)
   - [Event Handlers (Components)](#event-handlers-components)
   - [Kafka Consumer — UserEventListener](#kafka-consumer--usereventlistener)
8. [Rate Limiting](#rate-limiting)
9. [Caching Strategy](#caching-strategy)
10. [Scheduled Jobs](#scheduled-jobs)
11. [Validation Rules](#validation-rules)
12. [Error Reference](#error-reference)
13. [Flow Diagrams](#flow-diagrams)

---

## Overview

The **User** package manages everything related to an authenticated user's own profile and account settings. It sits above the auth package and assumes the user already has a valid session.

| Feature               | Description                                                                           |
|-----------------------|---------------------------------------------------------------------------------------|
| Profile Retrieval     | Fetch the authenticated user's own profile (cached)                                   |
| Profile Update        | Patch name, country, profession, and organization fields                              |
| Profile Picture       | Upload to AWS S3 or delete the current picture                                        |
| Forgot Password       | Initiates token-based password reset; rate-limited (5/hour per email)                 |
| Reset Password        | Consumes the reset token to set a new password; rate-limited (10/hour per email)      |
| Change Password       | Authenticated in-session password change; blacklists current access token             |
| Account Deletion      | Immediate hard delete or scheduled soft-delete with a 10-day grace period             |
| Inactive User Cleanup | Nightly job marks users inactive after 12 months without login and schedules deletion |

---

## Package Structure

```
user/
├── components/
│   ├── PasswordChangedEventHandler.java          # Kafka handler — sends password-changed email
│   ├── PasswordResetEventHandler.java            # Kafka handler — sends password reset token email
│   ├── PasswordResetSuccessfulEventHandler.java  # Kafka handler — sends password reset success email
│   ├── UserDeletedEventHandler.java              # Kafka handler — deletes S3 picture + sends deletion email
│   ├── UserDeletionScheduledEventHandler.java    # Kafka handler — sends scheduled-deletion warning email
│   └── WelcomeEmailHandler.java                  # Kafka handler — sends welcome email after verification
├── controller/
│   └── UserController.java                       # REST controller — maps HTTP requests to UserService
├── dto/
│   ├── GeneratedPassword.java                    # Inner DTO wrapping the generated password string
│   ├── UserData.java                             # User profile response payload
│   ├── request/
│   │   ├── ChangePasswordDto.java                # Change password request
│   │   ├── ForgotPasswordDto.java                # Forgot password request
│   │   ├── ProfileUpdateRequestDto.java          # Profile update request (JSON)
│   │   └── ResetPasswordDto.java                 # Reset password (token-based) request
│   └── response/
│       └── GeneratePasswordResponse.java         # Response wrapper for password generation
├── eventlistener/
│   └── UserEventListener.java                    # Kafka consumer — routes user events to registered handlers
├── events/
│   ├── PasswordChangedEvent.java                 # Event payload — password was changed
│   ├── PasswordResetEvent.java                   # Event payload — password reset requested
│   ├── PasswordResetSuccessfulEvent.java         # Event payload — password reset completed
│   ├── UserDeletedEvent.java                     # Event payload — user account deleted
│   └── UserDeletionScheduledEvent.java           # Event payload — account deletion scheduled
├── impl/
│   ├── ChangePasswordImpl.java                   # Change password business logic
│   ├── DeleteUserImpl.java                       # Account deletion + scheduled cleanup logic
│   ├── GetUserProfileImpl.java                   # Profile retrieval (with caching)
│   ├── PasswordResetImpl.java                    # Forgot + reset password logic
│   ├── ProfilePictureImpl.java                   # Profile picture upload/delete via AWS S3
│   └── ProfileUpdateImpl.java                    # Profile field update logic
├── interfaces/
│   ├── passwordreset/
│   │   ├── IPasswordReset.java                   # Contract: getEmail() + getToken()
│   │   └── PasswordResetParams.java              # Immutable record implementing IPasswordReset
│   └── userimpl/
│       ├── ChangePasswordService.java            # Contract for change password
│       ├── DeleteUserService.java                # Contract for user deletion
│       ├── GetUserProfileService.java            # Contract for profile retrieval
│       ├── PasswordResetService.java             # Contract for forgot/reset password
│       ├── ProfilePictureService.java            # Contract for profile picture operations
│       └── ProfileUpdateService.java             # Contract for profile updates
├── model/
│   └── User.java                                 # JPA entity + Spring Security UserDetails
├── repository/
│   └── UserRepository.java                       # JPA repository with custom JPQL queries
├── service/
│   └── UserService.java                          # Facade — delegates to individual implementations
└── documentation/
    └── api_docs.md                               # This file
```

---

## Data Models

### User Entity
**Package:** `com.extractor.unraveldocs.user.model`  
**Table:** `users`  
**Implements:** `UserDetails` (Spring Security)

| Column                  | Type             | Nullable | Default        | Description                                                               |
|-------------------------|------------------|----------|----------------|---------------------------------------------------------------------------|
| `id`                    | `String` (UUID)  | No       | Auto-generated | Primary key                                                               |
| `image_url`             | `String`         | Yes      | `null`         | URL of the profile picture stored on AWS S3                               |
| `first_name`            | `String`         | No       | —              | User's first name (capitalized on write)                                  |
| `last_name`             | `String`         | No       | —              | User's last name (capitalized on write)                                   |
| `email`                 | `String`         | No       | —              | Unique; lowercase; used as the Spring Security username                   |
| `password`              | `String`         | No       | —              | BCrypt-encoded password                                                   |
| `last_login`            | `OffsetDateTime` | Yes      | `null`         | Timestamp of the most recent successful login                             |
| `is_active`             | `boolean`        | No       | `false`        | Set to `true` after email verification; `false` for inactive/soft-deleted |
| `is_verified`           | `boolean`        | No       | `false`        | Set to `true` after email is verified                                     |
| `is_platform_admin`     | `boolean`        | No       | `false`        | Platform-level admin flag                                                 |
| `is_organization_admin` | `boolean`        | No       | `false`        | Organization-level admin flag                                             |
| `role`                  | `Role` (enum)    | No       | `USER`         | Authorization role                                                        |
| `deleted_at`            | `OffsetDateTime` | Yes      | `null`         | Soft-delete timestamp; non-null means deletion is scheduled               |
| `terms_accepted`        | `boolean`        | No       | `false`        | Whether user accepted terms at registration                               |
| `marketing_opt_in`      | `boolean`        | No       | `false`        | Whether user opted in to marketing emails                                 |
| `country`               | `String`         | No       | —              | User's country                                                            |
| `profession`            | `String`         | Yes      | `null`         | User's profession                                                         |
| `organization`          | `String`         | Yes      | `null`         | User's organization                                                       |
| `created_at`            | `OffsetDateTime` | No       | Auto           | Immutable creation timestamp                                              |
| `updated_at`            | `OffsetDateTime` | No       | Auto           | Updated on every save                                                     |

**Indexes:** `email` (unique), `is_active`, `is_verified`, `is_platform_admin`, `is_organization_admin`, `role`

**Relationships:**

| Association        | Type         | Target Entity        | Cascade             |
|--------------------|--------------|----------------------|---------------------|
| `loginAttempts`    | `@OneToOne`  | `LoginAttempts`      | ALL                 |
| `userVerification` | `@OneToOne`  | `UserVerification`   | ALL                 |
| `documents`        | `@OneToMany` | `DocumentCollection` | ALL + orphanRemoval |
| `subscription`     | `@OneToOne`  | `UserSubscription`   | ALL + orphanRemoval |

**Spring Security Integration:**

| Method                      | Returns                  | Note                                                      |
|-----------------------------|--------------------------|-----------------------------------------------------------|
| `getUsername()`             | `email`                  | Used by `AuthenticationManager`                           |
| `getAuthorities()`          | `[ROLE_<ROLE_NAME>]`     | Single authority derived from the `role` field            |
| `isEnabled()`               | `isActive && isVerified` | Both flags must be `true` for the account to authenticate |
| `isAccountNonExpired()`     | `true`                   | Always non-expired                                        |
| `isAccountNonLocked()`      | `true`                   | Lock logic handled externally via `LoginAttempts`         |
| `isCredentialsNonExpired()` | `true`                   | Always non-expired                                        |

---

### Request DTOs

#### `ForgotPasswordDto`
**Package:** `com.extractor.unraveldocs.user.dto.request`

| Field   | Type     | Required | Constraints            | Example                 |
|---------|----------|----------|------------------------|-------------------------|
| `email` | `String` | ✅        | Valid email; not blank | `"johndoe@example.com"` |

---

#### `ResetPasswordDto`
**Package:** `com.extractor.unraveldocs.user.dto.request`

| Field                | Type     | Required | Constraints                                               | Example                 |
|----------------------|----------|----------|-----------------------------------------------------------|-------------------------|
| `email`              | `String` | ✅        | Valid email; not blank                                    | `"johndoe@example.com"` |
| `token`              | `String` | ✅        | Not blank                                                 | `"a3f9b1c2d4..."`       |
| `newPassword`        | `String` | ✅        | Min 8 chars; uppercase + lowercase + digit + special char | `"NewP@ss123"`          |
| `confirmNewPassword` | `String` | ✅        | Must match `newPassword` (`@PasswordMatches`)             | `"NewP@ss123"`          |

---

#### `ChangePasswordDto`
**Package:** `com.extractor.unraveldocs.user.dto.request`

| Field                | Type     | Required | Constraints                                                           | Example         |
|----------------------|----------|----------|-----------------------------------------------------------------------|-----------------|
| `oldPassword`        | `String` | ✅        | Not blank                                                             | `"OldP@ss123"`  |
| `newPassword`        | `String` | ✅        | Min 8 chars; uppercase + lowercase + digit + special char             | `"NewP@ss456!"` |
| `confirmNewPassword` | `String` | ✅        | Must match `newPassword` (`@PasswordMatches` with custom field names) | `"NewP@ss456!"` |

---

#### `ProfileUpdateRequestDto`
**Package:** `com.extractor.unraveldocs.user.dto.request`

All fields are **optional** — only provided fields that differ from the current value are applied.

| Field          | Type     | Required | Constraints        | Example            |
|----------------|----------|----------|--------------------|--------------------|
| `firstName`    | `String` | ❌        | 2–80 characters    | `"Jane"`           |
| `lastName`     | `String` | ❌        | 2–80 characters    | `"Smith"`          |
| `country`      | `String` | ❌        | Max 100 characters | `"Canada"`         |
| `profession`   | `String` | ❌        | Max 150 characters | `"Data Scientist"` |
| `organization` | `String` | ❌        | Max 200 characters | `"Acme Inc."`      |

> ⚠️ Profile picture updates are handled separately via `POST /profile/{userId}/upload`.

---

### Response DTOs

#### `UserData`
**Package:** `com.extractor.unraveldocs.user.dto`  
Returned inside `UnravelDocsResponse<UserData>` for profile retrieval and profile update responses.

| Field            | Type             | Description                              |
|------------------|------------------|------------------------------------------|
| `id`             | `String`         | User UUID                                |
| `profilePicture` | `String`         | S3 URL of the profile picture (nullable) |
| `firstName`      | `String`         | User first name                          |
| `lastName`       | `String`         | User last name                           |
| `email`          | `String`         | User email                               |
| `role`           | `Role`           | User authorization role                  |
| `lastLogin`      | `OffsetDateTime` | Last successful login timestamp          |
| `isVerified`     | `boolean`        | Email verification status                |
| `country`        | `String`         | User country                             |
| `profession`     | `String`         | User profession (nullable)               |
| `organization`   | `String`         | User organization (nullable)             |
| `createdAt`      | `OffsetDateTime` | Account creation timestamp               |
| `updatedAt`      | `OffsetDateTime` | Last update timestamp                    |

---

## Endpoints

All endpoints are prefixed with `/api/v1/user`. All endpoints except `forgot-password` and `reset-password` require a valid `Authorization: Bearer <accessToken>` header.

---

### 1. Get Current User Profile

| Property          | Value             |
|-------------------|-------------------|
| **Method**        | `GET`             |
| **Path**          | `/api/v1/user/me` |
| **Auth Required** | Yes               |

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "User profile retrieved successfully",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "profilePicture": "https://bucket.s3.amazonaws.com/profile-pictures/avatar.jpg",
    "firstName": "John",
    "lastName": "Doe",
    "email": "johndoe@example.com",
    "role": "user",
    "lastLogin": "2026-02-26T10:05:00Z",
    "isVerified": true,
    "country": "USA",
    "profession": "Software Engineer",
    "organization": "Tech Company",
    "createdAt": "2026-01-01T08:00:00Z",
    "updatedAt": "2026-02-26T10:05:00Z"
  }
}
```

> **Caching:** The result is cached in `userProfileData` keyed by `userId`. Subsequent requests are served from cache until the cache is evicted.

**Error Responses**

| Status          | Condition                                         |
|-----------------|---------------------------------------------------|
| `403 Forbidden` | No authenticated user in security context         |
| `404 Not Found` | User record not found for the authenticated email |

---

### 2. Update User Profile

| Property          | Value                           |
|-------------------|---------------------------------|
| **Method**        | `PUT`                           |
| **Path**          | `/api/v1/user/profile/{userId}` |
| **Auth Required** | Yes                             |
| **Content-Type**  | `application/json`              |
| **Rate Limit**    | 20 requests / minute per user   |

**Path Parameters**

| Parameter | Type     | Description                                                    |
|-----------|----------|----------------------------------------------------------------|
| `userId`  | `String` | UUID of the user to update — must match the authenticated user |

**Request Body:** `ProfileUpdateRequestDto`

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "country": "Canada",
  "profession": "Data Scientist",
  "organization": "Acme Inc."
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Profile updated successfully",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "johndoe@example.com",
    "country": "Canada",
    "profession": "Data Scientist",
    "organization": "Acme Inc."
  }
}
```

**Side Effects**
- Only fields that are non-null, non-blank, and different from the current value are applied.
- Updated user is re-indexed in Elasticsearch if changes were made.
- Cache entries `getProfileByUser` and `getProfileByAdmin` are updated (`@CachePut`).

**Error Responses**

| Status                  | Condition                                |
|-------------------------|------------------------------------------|
| `400 Bad Request`       | Request body is null or validation fails |
| `403 Forbidden`         | Not authenticated                        |
| `404 Not Found`         | `userId` does not match any user         |
| `429 Too Many Requests` | Rate limit exceeded                      |

---

### 3. Upload Profile Picture

| Property          | Value                                  |
|-------------------|----------------------------------------|
| **Method**        | `POST`                                 |
| **Path**          | `/api/v1/user/profile/{userId}/upload` |
| **Auth Required** | Yes                                    |
| **Content-Type**  | `multipart/form-data`                  |
| **Rate Limit**    | 20 requests / minute per user          |

**Path Parameters**

| Parameter | Type     | Description                                       |
|-----------|----------|---------------------------------------------------|
| `userId`  | `String` | UUID of the user — used for rate-limit bucket key |

**Form Parameters**

| Parameter | Type            | Required | Description                                                 |
|-----------|-----------------|----------|-------------------------------------------------------------|
| `file`    | `MultipartFile` | ✅        | Image file to upload (validated against allowed MIME types) |

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Profile picture uploaded successfully.",
  "data": "https://bucket.s3.amazonaws.com/profile-pictures/uuid_filename.jpg"
}
```

**Side Effects**
- File is uploaded to the AWS S3 `profile-pictures/` folder via `AwsS3Service`.
- `user.profilePicture` is updated with the new S3 URL and persisted.

**Error Responses**

| Status                  | Condition                      |
|-------------------------|--------------------------------|
| `400 Bad Request`       | File is empty                  |
| `400 Bad Request`       | File MIME type is not an image |
| `403 Forbidden`         | Not authenticated              |
| `429 Too Many Requests` | Rate limit exceeded            |

---

### 4. Delete Profile Picture

| Property          | Value                                  |
|-------------------|----------------------------------------|
| **Method**        | `DELETE`                               |
| **Path**          | `/api/v1/user/profile/{userId}/delete` |
| **Auth Required** | Yes                                    |
| **Rate Limit**    | 20 requests / minute per user          |

**Path Parameters**

| Parameter | Type     | Description      |
|-----------|----------|------------------|
| `userId`  | `String` | UUID of the user |

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Profile picture deleted successfully.",
  "data": null
}
```

**Side Effects**
- Existing profile picture is deleted from AWS S3 via `AwsS3Service`.
- `user.profilePicture` is set to `null` and persisted.

**Error Responses**

| Status                  | Condition                             |
|-------------------------|---------------------------------------|
| `400 Bad Request`       | User has no profile picture to delete |
| `403 Forbidden`         | Not authenticated                     |
| `429 Too Many Requests` | Rate limit exceeded                   |

---

### 5. Forgot Password

| Property          | Value                               |
|-------------------|-------------------------------------|
| **Method**        | `POST`                              |
| **Path**          | `/api/v1/user/forgot-password`      |
| **Auth Required** | No                                  |
| **Content-Type**  | `application/json`                  |
| **Rate Limit**    | 5 requests / hour per email address |

**Request Body:** `ForgotPasswordDto`

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
  "message": "Password reset link sent to your email.",
  "data": null
}
```

**Side Effects**
- A hex password reset token is generated with a **1-hour TTL**.
- Token and expiry are stored in `UserVerification.passwordResetToken` / `passwordResetTokenExpiry`.
- A `PasswordResetEvent` is published to Kafka **after transaction commit** → triggers a password-reset email.

**Error Responses**

| Status                  | Condition                                                                           |
|-------------------------|-------------------------------------------------------------------------------------|
| `400 Bad Request`       | Account is not yet email-verified                                                   |
| `400 Bad Request`       | A valid (non-expired) reset token already exists — response includes remaining time |
| `404 Not Found`         | No user found for the given email                                                   |
| `429 Too Many Requests` | Rate limit exceeded (5 requests/hour per email)                                     |

---

### 6. Reset Password

| Property          | Value                                |
|-------------------|--------------------------------------|
| **Method**        | `POST`                               |
| **Path**          | `/api/v1/user/reset-password`        |
| **Auth Required** | No                                   |
| **Content-Type**  | `application/json`                   |
| **Rate Limit**    | 10 requests / hour per email address |

**Request Body:** `ResetPasswordDto`

```json
{
  "email": "johndoe@example.com",
  "token": "a3f9b1c2d4e5f6...",
  "newPassword": "NewP@ss123",
  "confirmNewPassword": "NewP@ss123"
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Password reset successfully.",
  "data": null
}
```

**Side Effects**
- New password is BCrypt-encoded and saved.
- `UserVerification.passwordResetToken` and `passwordResetTokenExpiry` are cleared.
- `UserVerification.status` is reset to `VERIFIED`.
- A `PasswordResetSuccessfulEvent` is published to Kafka **after transaction commit** → triggers a confirmation email.

**Error Responses**

| Status                  | Condition                                                       |
|-------------------------|-----------------------------------------------------------------|
| `400 Bad Request`       | Token is invalid or does not match stored token                 |
| `400 Bad Request`       | Token has expired (`VerifiedStatus` set to `EXPIRED`)           |
| `400 Bad Request`       | New password is the same as the current password                |
| `400 Bad Request`       | Validation failure (password complexity, passwords don't match) |
| `403 Forbidden`         | Account is not email-verified                                   |
| `404 Not Found`         | No user found for the given email                               |
| `429 Too Many Requests` | Rate limit exceeded                                             |

---

### 7. Change Password

| Property          | Value                          |
|-------------------|--------------------------------|
| **Method**        | `POST`                         |
| **Path**          | `/api/v1/user/change-password` |
| **Auth Required** | Yes                            |
| **Content-Type**  | `application/json`             |
| **Rate Limit**    | 20 requests / minute per user  |

**Request Body:** `ChangePasswordDto`

```json
{
  "oldPassword": "OldP@ss123",
  "newPassword": "NewP@ss456!",
  "confirmNewPassword": "NewP@ss456!"
}
```

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Password changed successfully.",
  "data": null
}
```

**Side Effects**
- New password is BCrypt-encoded and saved.
- The **current access token is immediately blacklisted** (JTI added to `TokenBlacklistService`) — the user must log in again.
- A `PasswordChangedEvent` is published to Kafka **after transaction commit** → triggers a security-alert email.

**Error Responses**

| Status                  | Condition                                              |
|-------------------------|--------------------------------------------------------|
| `400 Bad Request`       | Old password is incorrect                              |
| `400 Bad Request`       | New password is the same as the old password           |
| `400 Bad Request`       | Validation failure (complexity, passwords don't match) |
| `403 Forbidden`         | Account is not email-verified                          |
| `404 Not Found`         | Authenticated user not found (edge case)               |
| `429 Too Many Requests` | Rate limit exceeded                                    |

---

### 8. Delete User Account

| Property          | Value                           |
|-------------------|---------------------------------|
| **Method**        | `DELETE`                        |
| **Path**          | `/api/v1/user/profile/{userId}` |
| **Auth Required** | Yes                             |
| **Rate Limit**    | 20 requests / minute per user   |

**Path Parameters**

| Parameter | Type     | Description                        |
|-----------|----------|------------------------------------|
| `userId`  | `String` | UUID of the user account to delete |

**Success Response — `200 OK`**

```json
"User profile deleted successfully"
```

**Side Effects**
- `UserVerification` record is deleted first (FK constraint).
- User record is hard-deleted from the database immediately.
- A `UserDeletedEvent` is published to Kafka → handler deletes the S3 profile picture and sends a deletion confirmation email.
- Cache entries `getAllUsers`, `getProfileByAdmin`, and `getProfileByUser` are evicted (`@CacheEvict`).

**Error Responses**

| Status                  | Condition                        |
|-------------------------|----------------------------------|
| `403 Forbidden`         | Not authenticated                |
| `404 Not Found`         | `userId` does not match any user |
| `429 Too Many Requests` | Rate limit exceeded              |

---

## Service Layer

### `UserService` (Facade)
**Package:** `com.extractor.unraveldocs.user.service`

Thin orchestration layer. Controllers depend only on `UserService`, keeping them decoupled from implementation details.

| Method                                            | Delegates To            | Description                |
|---------------------------------------------------|-------------------------|----------------------------|
| `getUserProfileByOwner(String userId)`            | `GetUserProfileService` | Retrieve own profile       |
| `forgotPassword(ForgotPasswordDto)`               | `PasswordResetService`  | Initiate password reset    |
| `resetPassword(IPasswordReset, ResetPasswordDto)` | `PasswordResetService`  | Complete password reset    |
| `changePassword(ChangePasswordDto)`               | `ChangePasswordService` | In-session password change |
| `updateProfile(ProfileUpdateRequestDto, String)`  | `ProfileUpdateService`  | Update profile fields      |
| `deleteUser(String userId)`                       | `DeleteUserService`     | Delete user account        |
| `uploadProfilePicture(User, MultipartFile)`       | `ProfilePictureService` | Upload profile picture     |
| `deleteProfilePicture(User)`                      | `ProfilePictureService` | Delete profile picture     |

---

### `GetUserProfileImpl`
**Package:** `com.extractor.unraveldocs.user.impl`  
**Implements:** `GetUserProfileService`

**Logic:**
```
1. Delegate to getCachedUserData(userId)
   ├─ Cache HIT  → return cached UserData from "userProfileData" cache
   └─ Cache MISS → findById(userId) → NotFoundException if missing
                   → map User → UserData via ResponseData.getResponseData()
                   → store in cache
2. Wrap UserData in UnravelDocsResponse (HTTP 200)
```

---

### `ProfileUpdateImpl`
**Package:** `com.extractor.unraveldocs.user.impl`  
**Implements:** `ProfileUpdateService`  
**Transactional:** Yes (`@Transactional`)  
**Cache:** `@CachePut` on `getProfileByUser` and `getProfileByAdmin` caches, keyed by `userId`

**Logic:**
```
1. findById(userId) → NotFoundException if missing
2. For each updateable field (firstName, lastName, country, profession, organization):
   └─ If field is non-null, non-blank, and different from current value → apply change, set hasChanges = true
3. If hasChanges:
   ├─ save(user)
   └─ Re-index user in Elasticsearch (IndexAction.UPDATE)
4. Map updated User → UserData
5. Return UserData (HTTP 200)
```

---

### `ProfilePictureImpl`
**Package:** `com.extractor.unraveldocs.user.impl`  
**Implements:** `ProfilePictureService`  
**Transactional:** Yes (`@Transactional`)

**`uploadProfilePicture(User, MultipartFile)`**
```
1. Validate file MIME type via FileType.IMAGE.isValid() → BadRequestException if invalid
2. Generate S3 file name (AwsS3Service.generateFileName)
3. Upload file to S3 "profile-pictures/" folder
4. Update user.profilePicture with the returned URL
5. Save user
6. Return S3 URL (HTTP 200)
```

**`deleteProfilePicture(User)`**
```
1. If user.profilePicture is null/empty → return HTTP 400 (no picture to delete)
2. Delete file from S3 (AwsS3Service.deleteFile)
3. Set user.profilePicture = null
4. Save user
5. Return HTTP 200
```

---

### `PasswordResetImpl`
**Package:** `com.extractor.unraveldocs.user.impl`  
**Implements:** `PasswordResetService`  
**Transactional:** Yes (`@Transactional`)

**`forgotPassword(ForgotPasswordDto)`**
```
1. findByEmail(email) → NotFoundException if not found
2. [Account not verified?] → BadRequestException
3. [Valid reset token already exists?] → BadRequestException with time-remaining message
4. Generate hex token with 1-hour TTL
5. Store token + expiry in UserVerification
6. save(user)
7. After commit: publish PasswordResetEvent → Kafka → password-reset email
8. Return HTTP 200
```

**`resetPassword(IPasswordReset, ResetPasswordDto)`**
```
1. findByEmail(email) → NotFoundException if not found
2. [Account not verified?] → ForbiddenException
3. [Token mismatch or null?] → BadRequestException
4. [Token expired?] → set EXPIRED status, save, BadRequestException
5. [New password == old password?] → BadRequestException
6. Encode and save new password
7. Clear passwordResetToken + expiry, set status = VERIFIED
8. save(user)
9. After commit: publish PasswordResetSuccessfulEvent → Kafka → success email
10. Return HTTP 200
```

---

### `ChangePasswordImpl`
**Package:** `com.extractor.unraveldocs.user.impl`  
**Implements:** `ChangePasswordService`  
**Transactional:** Yes (`@Transactional`)

**Logic:**
```
1. Get authenticated user email from SecurityContextHolder
2. findByEmail(email) → NotFoundException if not found
3. [Account not verified?] → ForbiddenException
4. [oldPassword incorrect?] → BadRequestException
5. [newPassword == oldPassword?] → BadRequestException
6. Encode and save new password
7. Blacklist current access token JTI in TokenBlacklistService
8. After commit: publish PasswordChangedEvent → Kafka → security-alert email
9. Return HTTP 200
```

---

### `DeleteUserImpl`
**Package:** `com.extractor.unraveldocs.user.impl`  
**Implements:** `DeleteUserService`  
**Transactional:** Yes (all methods)

| Method                                | Trigger                               | Description                                                              |
|---------------------------------------|---------------------------------------|--------------------------------------------------------------------------|
| `deleteUser(String userId)`           | API call (`DELETE /profile/{userId}`) | Immediate hard delete; publishes `UserDeletedEvent`                      |
| `scheduleUserDeletion(String userId)` | Programmatic / admin                  | Sets `deletedAt = now + 10 days`; publishes `UserDeletionScheduledEvent` |
| `checkAndScheduleInactiveUsers()`     | Cron: `0 0 0 * * ?` (midnight)        | Marks users inactive if `lastLogin < 12 months ago`; schedules deletion  |
| `processScheduledDeletions()`         | Cron: `0 0 1 * * ?` (01:00)           | Hard-deletes all users whose `deletedAt` is in the past                  |

**Deletion sequence (immediate):**
```
1. findById(userId) → NotFoundException if not found
2. Publish UserDeletedEvent → Kafka
   └─ Handler: delete S3 profile picture + send deletion confirmation email
3. Delete UserVerification (FK constraint)
4. delete(user)
5. Evict caches: getAllUsers, getProfileByAdmin, getProfileByUser
```

**Scheduled deletion sequence:**
```
Midnight cron:
  → Find all users where lastLogin < (now - 12 months)
  → Set isActive = false, deletedAt = now + 10 days
  → Publish UserDeletionScheduledEvent per user → warning email sent

01:00 cron:
  → Find all users where deletedAt < now (past the grace period)
  → For each: publish UserDeletedEvent, delete UserVerification, delete User
  → Process in batches of 100
```

---

## Repository

### `UserRepository`
**Package:** `com.extractor.unraveldocs.user.repository`  
**Extends:** `JpaRepository<User, String>`

| Method                                                                                   | Description                                                                                                                                         |
|------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `findByEmail(String email)`                                                              | Look up a user by email (used by login, password reset, etc.)                                                                                       |
| `findUserById(String id)`                                                                | Look up a user by primary key (used by token refresh)                                                                                               |
| `existsByEmail(String email)`                                                            | Check for email uniqueness during registration                                                                                                      |
| `findBySubscriptionIsNull()`                                                             | Find users missing a subscription (maintenance use)                                                                                                 |
| `findAllUsers(search, firstName, lastName, email, role, isActive, isVerified, Pageable)` | Full-featured filtered + paginated user list for admin panel; all filter parameters are optional; excludes soft-deleted users (`deletedAt IS NULL`) |
| `findAllByLastLoginDateBefore(threshold, Pageable)`                                      | Finds active users whose last login is before the given threshold; used by inactive-user cron                                                       |
| `findAllByDeletedAtBefore(threshold, Pageable)`                                          | Finds users whose scheduled deletion date has passed; used by deletion-processing cron                                                              |
| `findByCreatedAtAfterAndDeletedAtIsNull(createdAfter)`                                   | Finds new non-deleted users created after a date; used by coupon targeting                                                                          |

---

## Events & Async Processing

All events are published **after database transaction commit** using `TransactionSynchronizationManager.registerSynchronization`. This prevents ghost events on rollback.

### Published Events

| Event Class                    | Event Type Constant         | Published By         | Trigger                                 |
|--------------------------------|-----------------------------|----------------------|-----------------------------------------|
| `PasswordResetEvent`           | `PASSWORD_RESET_REQUESTED`  | `PasswordResetImpl`  | Forgot password request                 |
| `PasswordResetSuccessfulEvent` | `PASSWORD_RESET_SUCCESSFUL` | `PasswordResetImpl`  | Password successfully reset             |
| `PasswordChangedEvent`         | `PASSWORD_CHANGED`          | `ChangePasswordImpl` | In-session password change              |
| `UserDeletedEvent`             | `USER_DELETED`              | `DeleteUserImpl`     | Immediate or scheduled account deletion |
| `UserDeletionScheduledEvent`   | `USER_DELETION_SCHEDULED`   | `DeleteUserImpl`     | Account flagged for future deletion     |

#### `PasswordResetEvent`
| Field        | Type     | Description                           |
|--------------|----------|---------------------------------------|
| `email`      | `String` | Recipient email                       |
| `firstName`  | `String` | User first name                       |
| `lastName`   | `String` | User last name                        |
| `token`      | `String` | Hex reset token                       |
| `expiration` | `String` | Human-readable TTL (e.g., `"1 hour"`) |

#### `PasswordResetSuccessfulEvent`
| Field       | Type     | Description     |
|-------------|----------|-----------------|
| `email`     | `String` | Recipient email |
| `firstName` | `String` | User first name |
| `lastName`  | `String` | User last name  |

#### `PasswordChangedEvent`
| Field       | Type     | Description     |
|-------------|----------|-----------------|
| `email`     | `String` | Recipient email |
| `firstName` | `String` | User first name |
| `lastName`  | `String` | User last name  |

#### `UserDeletedEvent`
| Field               | Type     | Description                                          |
|---------------------|----------|------------------------------------------------------|
| `email`             | `String` | Deleted user's email                                 |
| `profilePictureUrl` | `String` | S3 URL of the profile picture to clean up (nullable) |

#### `UserDeletionScheduledEvent`
| Field          | Type             | Description                                            |
|----------------|------------------|--------------------------------------------------------|
| `email`        | `String`         | Recipient email                                        |
| `firstName`    | `String`         | User first name                                        |
| `lastName`     | `String`         | User last name                                         |
| `deletionDate` | `OffsetDateTime` | ISO 8601 timestamp of when the account will be deleted |
| `reason`       | `String`         | Optional reason for scheduled deletion                 |

---

### Event Handlers (Components)

All handlers implement `EventHandler<T>` and are registered by event type in the Spring context.

| Handler                               | Event Type                  | Action                                                                                                       |
|---------------------------------------|-----------------------------|--------------------------------------------------------------------------------------------------------------|
| `PasswordResetEventHandler`           | `PASSWORD_RESET_REQUESTED`  | Calls `UserEmailTemplateService.sendPasswordResetToken(...)`                                                 |
| `PasswordResetSuccessfulEventHandler` | `PASSWORD_RESET_SUCCESSFUL` | Calls `UserEmailTemplateService.sendSuccessfulPasswordReset(...)`                                            |
| `PasswordChangedEventHandler`         | `PASSWORD_CHANGED`          | Calls `UserEmailTemplateService.sendSuccessfulPasswordChange(...)`                                           |
| `UserDeletedEventHandler`             | `USER_DELETED`              | Deletes S3 profile picture via `AwsS3Service`; calls `UserEmailTemplateService.sendDeletedAccountEmail(...)` |
| `UserDeletionScheduledEventHandler`   | `USER_DELETION_SCHEDULED`   | Calls `UserEmailTemplateService.scheduleUserDeletion(...)`                                                   |
| `WelcomeEmailHandler`                 | `WELCOME_EVENT`             | Calls `UserEmailTemplateService.sendWelcomeEmail(...)` (published by the auth package, handled here)         |

---

### Kafka Consumer — `UserEventListener`
**Package:** `com.extractor.unraveldocs.user.eventlistener`  
**Topic:** `KafkaTopicConfig.TOPIC_USERS`  
**Consumer Group:** `user-events-group`  
**Conditional:** Only active when `spring.kafka.bootstrap-servers` is configured

**Routing logic:**
1. Extracts `event-type` from the Kafka message header (falls back to the record key).
2. Normalizes the event type to a simple class name (strips package prefix).
3. If the payload is a `Map` (raw JSON deserialization), converts it to the appropriate event class using `ObjectMapper`.
4. Looks up the matching `EventHandler` from the Spring-injected `Map<String, EventHandler<?>>`.
5. Calls `handler.handleEvent(payload)` and acknowledges the message.
6. On error, re-throws the exception to trigger Kafka retry / Dead Letter Queue (DLQ) handling.

**Supported event types:**

| Simple Name                    | Class                                                                |
|--------------------------------|----------------------------------------------------------------------|
| `UserRegisteredEvent`          | `com.extractor.unraveldocs.auth.events.UserRegisteredEvent`          |
| `WelcomeEvent`                 | `com.extractor.unraveldocs.auth.events.WelcomeEvent`                 |
| `PasswordChangedEvent`         | `com.extractor.unraveldocs.user.events.PasswordChangedEvent`         |
| `PasswordResetEvent`           | `com.extractor.unraveldocs.user.events.PasswordResetEvent`           |
| `PasswordResetSuccessfulEvent` | `com.extractor.unraveldocs.user.events.PasswordResetSuccessfulEvent` |
| `UserDeletedEvent`             | `com.extractor.unraveldocs.user.events.UserDeletedEvent`             |
| `UserDeletionScheduledEvent`   | `com.extractor.unraveldocs.user.events.UserDeletionScheduledEvent`   |

---

## Rate Limiting

Rate limiting is implemented in-process using [Bucket4j](https://github.com/bucket4j/bucket4j) with per-key token buckets stored in `ConcurrentHashMap`. Buckets are lazily created on first request for each key.

| Endpoint                          | Key           | Limit     | Window     | Exception on Breach        |
|-----------------------------------|---------------|-----------|------------|----------------------------|
| `POST /forgot-password`           | Email address | 5 tokens  | Per hour   | `TooManyRequestsException` |
| `POST /reset-password`            | Email address | 10 tokens | Per hour   | `TooManyRequestsException` |
| `POST /change-password`           | User ID       | 20 tokens | Per minute | `TooManyRequestsException` |
| `PUT /profile/{userId}`           | User ID       | 20 tokens | Per minute | `TooManyRequestsException` |
| `DELETE /profile/{userId}`        | User ID       | 20 tokens | Per minute | `TooManyRequestsException` |
| `POST /profile/{userId}/upload`   | User ID       | 20 tokens | Per minute | `TooManyRequestsException` |
| `DELETE /profile/{userId}/delete` | User ID       | 20 tokens | Per minute | `TooManyRequestsException` |

> ⚠️ Buckets use the **greedy** refill strategy — tokens are refilled continuously rather than in bulk at the end of the window.

---

## Caching Strategy

| Cache Name          | Key      | Populated By                                            | Evicted By                                                                                         |
|---------------------|----------|---------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `userProfileData`   | `userId` | `GetUserProfileImpl.getCachedUserData()` (`@Cacheable`) | Not explicitly evicted (TTL-based or restart)                                                      |
| `getProfileByUser`  | `userId` | `ProfileUpdateImpl.updateProfile()` (`@CachePut`)       | `DeleteUserImpl.deleteUser()` + `processScheduledDeletions()` (`@CacheEvict`, `allEntries = true`) |
| `getProfileByAdmin` | `userId` | `ProfileUpdateImpl.updateProfile()` (`@CachePut`)       | `DeleteUserImpl.deleteUser()` + `processScheduledDeletions()` (`@CacheEvict`, `allEntries = true`) |
| `getAllUsers`       | —        | Admin list endpoint (separate package)                  | `DeleteUserImpl.deleteUser()` + `processScheduledDeletions()` (`@CacheEvict`, `allEntries = true`) |

---

## Scheduled Jobs

Both jobs are defined in `DeleteUserImpl` and run on the application server's default scheduler.

### `checkAndScheduleInactiveUsers`
- **Schedule:** `0 0 0 * * ?` (every day at midnight)
- **Batch size:** 100 users per page
- **Logic:** Finds all non-deleted users whose `lastLogin` is before `now - 12 months`. Marks each as inactive (`isActive = false`) and schedules them for deletion 10 days from now. Publishes a `UserDeletionScheduledEvent` per user (→ warning email).

### `processScheduledDeletions`
- **Schedule:** `0 0 1 * * ?` (every day at 01:00)
- **Batch size:** 100 users per page
- **Logic:** Finds all users whose `deletedAt < now` (grace period elapsed). Hard-deletes each user and their `UserVerification` record. Publishes a `UserDeletedEvent` per user (→ deletion confirmation email + S3 cleanup). Evicts all user-related caches.

---

## Validation Rules

| Rule                        | Applied To                                           | Details                                                              |
|-----------------------------|------------------------------------------------------|----------------------------------------------------------------------|
| `@NotBlank` / `@NotNull`    | Email, password fields                               | Standard null/empty checks                                           |
| `@Email`                    | `ForgotPasswordDto.email`, `ResetPasswordDto.email`  | RFC-compliant email format                                           |
| `@Size(min=2, max=80)`      | `firstName`, `lastName` in `ProfileUpdateRequestDto` | Character bounds                                                     |
| `@Size(max=100)`            | `country`                                            | Character bound                                                      |
| `@Size(max=150)`            | `profession`                                         | Character bound                                                      |
| `@Size(max=200)`            | `organization`                                       | Character bound                                                      |
| `@Size(min=8)`              | `newPassword` in change/reset DTOs                   | Minimum length                                                       |
| `@Pattern` (× 4)            | `newPassword`                                        | Requires uppercase + lowercase + digit + special char (`@$!%*?&`)    |
| `@PasswordMatches`          | `ChangePasswordDto`, `ResetPasswordDto`              | `newPassword` == `confirmNewPassword` (custom field names specified) |
| Old password ≠ new password | `ChangePasswordImpl`, `PasswordResetImpl`            | Enforced in service layer via BCrypt                                 |
| File not empty              | `ProfilePictureImpl`                                 | Checked in controller before service call                            |
| File MIME type is image     | `ProfilePictureImpl`                                 | `FileType.IMAGE.isValid(contentType)`                                |

---

## Error Reference

| Exception Class            | HTTP Status             | Common Trigger                                                                                 |
|----------------------------|-------------------------|------------------------------------------------------------------------------------------------|
| `BadRequestException`      | `400 Bad Request`       | Validation failures, wrong password, expired/invalid token, same-as-old password, invalid file |
| `ForbiddenException`       | `403 Forbidden`         | No authenticated user, account not verified                                                    |
| `NotFoundException`        | `404 Not Found`         | User not found by ID or email                                                                  |
| `TooManyRequestsException` | `429 Too Many Requests` | Rate limit bucket exhausted                                                                    |

---

## Flow Diagrams

### Get Profile Flow

```
Client
  │
  ├─ GET /api/v1/user/me
  │   Authorization: Bearer <token>
  │
  ▼
UserController
  │
  ├─ Extract User from @AuthenticationPrincipal
  │
  ▼
UserService → GetUserProfileImpl
  │
  ├─ Cache HIT  ─────────────────────────────────► Return UserData (HTTP 200)
  │
  └─ Cache MISS → findById(userId)
                    └─ NotFoundException if not found
                  → Map User → UserData → Store in cache
                  → Return UserData (HTTP 200)
```

---

### Forgot Password Flow

```
Client
  │
  ├─ POST /api/v1/user/forgot-password { email }
  │
  ▼
UserController
  │
  ├─ Rate limit check (5/hour per email) ───────► 429 Too Many Requests
  │
  ▼
UserService → PasswordResetImpl.forgotPassword()
  │
  ├─ findByEmail(email) ────────────────────────► 404 Not Found
  ├─ [Not verified?] ───────────────────────────► 400 Bad Request
  ├─ [Valid token already exists?] ─────────────► 400 Bad Request (+ time remaining)
  │
  ├─ Generate hex token, set 1-hour TTL
  ├─ save(user)
  │
  └─ After commit:
       └─ Publish PasswordResetEvent → Kafka
             └─ PasswordResetEventHandler → sendPasswordResetToken() → Email sent
  │
  ▼
200 OK
```

---

### Reset Password Flow

```
Client
  │
  ├─ POST /api/v1/user/reset-password { email, token, newPassword, confirmNewPassword }
  │
  ▼
UserController
  │
  ├─ Rate limit check (10/hour per email) ──────► 429 Too Many Requests
  │
  ▼
UserService → PasswordResetImpl.resetPassword()
  │
  ├─ findByEmail(email) ────────────────────────► 404 Not Found
  ├─ [Not verified?] ───────────────────────────► 403 Forbidden
  ├─ [Token invalid?] ──────────────────────────► 400 Bad Request
  ├─ [Token expired?] ──────────────────────────► set EXPIRED + 400 Bad Request
  ├─ [New == old password?] ────────────────────► 400 Bad Request
  │
  ├─ Encode and save new password
  ├─ Clear token fields, status = VERIFIED
  │
  └─ After commit:
       └─ Publish PasswordResetSuccessfulEvent → Kafka
             └─ PasswordResetSuccessfulEventHandler → sendSuccessfulPasswordReset() → Email sent
  │
  ▼
200 OK
```

---

### Change Password Flow

```
Client
  │
  ├─ POST /api/v1/user/change-password { oldPassword, newPassword, confirmNewPassword }
  │   Authorization: Bearer <token>
  │
  ▼
UserController
  │
  ├─ Rate limit check (20/min per user) ────────► 429 Too Many Requests
  │
  ▼
UserService → ChangePasswordImpl.changePassword()
  │
  ├─ Get email from SecurityContextHolder
  ├─ findByEmail(email) ────────────────────────► 404 Not Found
  ├─ [Not verified?] ───────────────────────────► 403 Forbidden
  ├─ [oldPassword wrong?] ──────────────────────► 400 Bad Request
  ├─ [newPassword == old?] ─────────────────────► 400 Bad Request
  │
  ├─ Encode and save new password
  ├─ Blacklist current access token JTI ← user must log in again
  │
  └─ After commit:
       └─ Publish PasswordChangedEvent → Kafka
             └─ PasswordChangedEventHandler → sendSuccessfulPasswordChange() → Security email
  │
  ▼
200 OK
```

---

### Account Deletion Flow (Immediate)

```
Client
  │
  ├─ DELETE /api/v1/user/profile/{userId}
  │   Authorization: Bearer <token>
  │
  ▼
UserController
  │
  ├─ Rate limit check (20/min per user) ────────► 429 Too Many Requests
  │
  ▼
UserService → DeleteUserImpl.deleteUser()
  │
  ├─ findById(userId) ──────────────────────────► 404 Not Found
  ├─ Publish UserDeletedEvent → Kafka
  │     └─ UserDeletedEventHandler:
  │           ├─ Delete S3 profile picture
  │           └─ Send deletion confirmation email
  ├─ Delete UserVerification record
  ├─ delete(user)
  └─ Evict caches: getAllUsers, getProfileByAdmin, getProfileByUser
  │
  ▼
200 OK
```

---

### Scheduled Deletion Flow

```
[Midnight Cron — checkAndScheduleInactiveUsers]
  │
  ├─ Find users where lastLogin < (now - 12 months)
  ├─ Per user:
  │   ├─ isActive = false
  │   ├─ deletedAt = now + 10 days
  │   └─ Publish UserDeletionScheduledEvent → warning email
  └─ saveAll(batch of 100)

[01:00 Cron — processScheduledDeletions]
  │
  ├─ Find users where deletedAt < now
  ├─ Per user:
  │   ├─ Publish UserDeletedEvent → S3 cleanup + deletion email
  │   └─ Delete UserVerification + User record
  └─ Evict all user caches
```

---

### Profile Picture Upload Flow

```
Client
  │
  ├─ POST /api/v1/user/profile/{userId}/upload
  │   Authorization: Bearer <token>
  │   Content-Type: multipart/form-data
  │   file: <image file>
  │
  ▼
UserController
  │
  ├─ [File empty?] ─────────────────────────────► 400 Bad Request
  ├─ Rate limit check (20/min per user) ────────► 429 Too Many Requests
  │
  ▼
UserService → ProfilePictureImpl.uploadProfilePicture()
  │
  ├─ [Invalid MIME type?] ──────────────────────► 400 Bad Request
  ├─ Generate S3 file name
  ├─ Upload to S3 "profile-pictures/" folder
  ├─ user.profilePicture = returned S3 URL
  └─ save(user)
  │
  ▼
200 OK + S3 URL string
```

