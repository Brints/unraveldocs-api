# Team Package — API Documentation

> **Base URL:** `/api/v1/teams`
> **Package:** `com.extractor.unraveldocs.team`
> **Last Updated:** 2026-03-07

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
   - [Team Creation](#team-creation)
   - [Team View](#team-view)
   - [Member Management](#member-management)
   - [Invitations](#invitations)
   - [Subscription Management](#subscription-management)
   - [Team Lifecycle](#team-lifecycle)

---

## Overview

The **Team** package handles operations relating to organizations and workspaces within UnravelDocs. It covers team creation, role-based member management (Owner, Admin, Member), invitations, subscription lifecycle management (trial, active, past due, cancelled, expired), and limits on members/documents based on subscription tiers (Premium, Enterprise). It enables multi-tenant collaboration on documents.

---

## Package Structure

```
team/
├── controller/
│   └── TeamController.java                  # REST controller — endpoints for team ops
├── datamodel/
│   ├── TeamBillingCycle.java                # Enum: MONTHLY | YEARLY
│   ├── TeamSubscriptionStatus.java          # Enum: TRIAL | ACTIVE | CANCELLED | EXPIRED | PAST_DUE
│   └── TeamSubscriptionType.java            # Enum: TEAM_PREMIUM | TEAM_ENTERPRISE
├── documentation/
│   └── api_docs.md                          # This file
├── dto/
│   ├── request/
│   │   ├── AddTeamMemberRequest.java
│   │   ├── CreateTeamRequest.java
│   │   ├── InviteTeamMemberRequest.java
│   │   ├── PromoteToAdminRequest.java
│   │   ├── RemoveTeamMembersRequest.java
│   │   └── VerifyTeamOtpRequest.java
│   └── response/
│       ├── TeamMemberResponse.java
│       └── TeamResponse.java
├── events/                                  # Kafka / Application events
├── impl/                                    # Business logic implementations
├── interfaces/
│   └── TeamService.java                     # Contract for team operations
├── jobs/                                    # Scheduled tasks (e.g. billing, trial expiry)
├── model/
│   ├── Team.java                            # JPA entity — Core aggregate root
│   ├── TeamInvitation.java                  # JPA entity — Represents a pending invite
│   ├── TeamMember.java                      # JPA entity — Associates User + Team + Role
│   ├── TeamOtpVerification.java             # JPA entity — Stores OTPs for team creation
│   └── TeamSubscriptionPlan.java            # JPA entity — Pricing and limits config
├── repository/                              # Spring Data JPA repositories
└── service/                                 # General internal services
```

---

## Data Models

### Enums

#### `TeamSubscriptionType`
Identifiers for the team's tier. Price and features change based on this.
* `TEAM_PREMIUM`: Standard team tier
* `TEAM_ENTERPRISE`: Advanced tier (unlimited document loads, advanced admin features)

#### `TeamBillingCycle`
* `MONTHLY`: Billed every 1 month
* `YEARLY`: Billed every 12 months

#### `TeamSubscriptionStatus`
* `TRIAL`: 10-day free trial period.
* `ACTIVE`: Subscription is active and paid.
* `CANCELLED`: Cancelled, but active until billing period ends.
* `EXPIRED`: Trial or subscription period ended.
* `PAST_DUE`: Payment failed, but access may be allowed via grace period.

### Entities

#### `Team`
The core aggregate. Holds subscription metadata, payment integration refs (Stripe/Paystack), team roles, and usage limits.
* **Fields**: `id`, `name`, `description`, `teamCode`, `subscriptionType`, `billingCycle`, `subscriptionStatus`, `hasUsedTrial`, `trialEndsAt`, `maxMembers`, `monthlyDocumentLimit`, `storageUsed`. Contains boolean flags like `isClosed`, `isActive`, `isVerified`.

#### `TeamMember`
An association between a `User` and `Team`.
* **Fields**: `id`, `team`, `user`, `role` (OWNER, ADMIN, MEMBER), `invitedBy`, `joinedAt`.

#### `TeamInvitation`
Tracks invitations sent via email to join a team.
* **Fields**: `id`, `team`, `email`, `invitationToken`, `status` (PENDING, ACCEPTED, EXPIRED, CANCELLED), `expiresAt`, `acceptedAt`.

### Request DTOs

#### `CreateTeamRequest`
| Field              | Required | Type                   | Note |
|--------------------|----------|------------------------|------|
| `name`             | ✅        | String                 | 2-100 characters |
| `description`      | ❌        | String                 | Max 500 characters |
| `subscriptionType` | ✅        | `TeamSubscriptionType` | PREMIUM or ENTERPRISE |
| `billingCycle`     | ✅        | `TeamBillingCycle`     | MONTHLY or YEARLY |
| `paymentGateway`   | ✅        | String                 | Gateway name (e.g., Stripe, Paystack) |
| `paymentToken`     | ❌        | String                 | Optional payment token at signup |

#### `VerifyTeamOtpRequest`
| Field | Required | Type   | Note |
|-------|----------|--------|------|
| `otp` | ✅        | String | 6 digits |

#### `AddTeamMemberRequest` & `InviteTeamMemberRequest`
| Field   | Required | Type   | Note |
|---------|----------|--------|------|
| `email` | ✅        | String | Valid email address |

#### `RemoveTeamMembersRequest`
| Field       | Required | Type         | Note |
|-------------|----------|--------------|------|
| `memberIds` | ✅        | List<String> | At least one ID required |

### Response DTOs

#### `TeamResponse`
Contains all key details of a team, structured for the frontend. Includes subscription data (`type`, `cycle`, `status`, `price`, `currency`, `flags`), billing dates, trial details, and current/max member counts. Emits context-specific `isOwner` boolean.

#### `TeamMemberResponse`
Details of a user belonging to the team (`userId`, `firstName`, `lastName`, `email`, `role`, `joinedAt`). Notably, `email` is masked for basic members viewing others for privacy.

---

## Endpoints

All endpoints require authentication (Bearer token) except where explicitly noted. Accessing or managing team resources often requires verifying the caller's role (`OWNER`, `ADMIN`, etc.).

### Team Creation

#### 1. Initiate Team Creation
Sends an OTP to the user's email to verify intent before creating the team.
* **POST** `/initiate`
* **Body:** `CreateTeamRequest`
* **Response:** `200 OK` — `UnravelDocsResponse<Void>`

#### 2. Verify OTP and Create Team
Validates the OTP and finalizes team and subscription setup. The caller becomes the team `OWNER`.
* **POST** `/verify`
* **Body:** `VerifyTeamOtpRequest`
* **Response:** `201 Created` — `UnravelDocsResponse<TeamResponse>`

### Team View

#### 3. Get Team Details
Returns detailed information about a team. Caller must be an active member.
* **GET** `/{teamId}`
* **Response:** `200 OK` — `UnravelDocsResponse<TeamResponse>`

#### 4. Get My Teams
Returns a list of all teams the caller holds membership in.
* **GET** `/my`
* **Response:** `200 OK` — `UnravelDocsResponse<List<TeamResponse>>`

#### 5. Get Team Members
Returns a list of members in the team. Email addresses of other members are masked unless the caller is the `OWNER`.
* **GET** `/{teamId}/members`
* **Response:** `200 OK` — `UnravelDocsResponse<List<TeamMemberResponse>>`

### Member Management

Requires `ADMIN` or `OWNER` role.

#### 6. Add Member
Directly adds an existing Platform User to the team. Limits apply based on subscription (`maxMembers`).
* **POST** `/{teamId}/members`
* **Body:** `AddTeamMemberRequest`
* **Response:** `201 Created` — `UnravelDocsResponse<TeamMemberResponse>`

#### 7. Remove Member
Removes a specific member. An `OWNER` cannot be removed.
* **DELETE** `/{teamId}/members/{memberId}`
* **Response:** `200 OK` — `UnravelDocsResponse<Void>`

#### 8. Batch Remove Members
Removes multiple members in one bulk action.
* **DELETE** `/{teamId}/members/batch`
* **Body:** `RemoveTeamMembersRequest`
* **Response:** `200 OK` — `UnravelDocsResponse<Void>`

#### 9. Promote to Admin
Promotes a `MEMBER` to `ADMIN`. Usually restricted to `OWNER`.
* **POST** `/{teamId}/members/{memberId}/promote`
* **Response:** `200 OK` — `UnravelDocsResponse<TeamMemberResponse>`

### Invitations

#### 10. Send Invitation
Sends an email invitation containing a unique link/token. Often restricted to ENTERPRISE tiers.
* **POST** `/{teamId}/invitations`
* **Body:** `InviteTeamMemberRequest`
* **Response:** `201 Created` — `UnravelDocsResponse<String>` (typically contains success message)

#### 11. Accept Invitation
Validates an invitation token and adds the authenticated user to the team. Sets invitation status to `ACCEPTED`.
* **POST** `/invitations/{token}/accept`
* **Response:** `200 OK` — `UnravelDocsResponse<TeamMemberResponse>`

### Subscription Management

#### 12. Cancel Subscription
Cancels auto-renewal. The team retains full features until the current billing period (`subscriptionEndsAt`) is reached, at which point status drops to `CANCELLED` or `EXPIRED`. Requires `OWNER`.
* **POST** `/{teamId}/cancel`
* **Response:** `200 OK` — `UnravelDocsResponse<TeamResponse>`

### Team Lifecycle

Requires `OWNER` role.

#### 13. Close Team
Closes the team. Sets `isClosed = true` and `closedAt`. Active subscriptions should generally be cancelled first. Members remain associated but lose access to team resources until reactivated.
* **DELETE** `/{teamId}`
* **Response:** `200 OK` — `UnravelDocsResponse<Void>`

#### 14. Reactivate Team
Reopens a previously closed team if they are within reactivation bounds. 
* **POST** `/{teamId}/reactivate`
* **Response:** `200 OK` — `UnravelDocsResponse<TeamResponse>`
