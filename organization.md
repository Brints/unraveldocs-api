# Organization Feature Documentation

## Overview

The Organization feature enables Premium and Enterprise subscribers to create and manage organizations with team collaboration capabilities. Organizations provide a shared workspace for document processing with role-based access control.

---

## Subscription Tiers

| Feature | Premium | Enterprise |
|---------|---------|------------|
| Monthly Document Limit | 200 | Unlimited |
| Maximum Members | 10 | 10 |
| Admin Promotion | ❌ | ✅ |
| Email Invitations | ❌ | ✅ |
| Free Trial | 10 days | 10 days |

---

## Roles & Permissions

| Permission | Owner | Admin | Member |
|------------|-------|-------|--------|
| View organization | ✅ | ✅ | ✅ |
| View all member emails | ✅ | ❌ | ❌ |
| Add members | ✅ | ✅ | ❌ |
| Remove members | ✅ | ✅ | ❌ |
| Promote to admin | ✅ | ❌ | ❌ |
| Send invitations | ✅ | ✅ | ❌ |
| Close organization | ✅ | ❌ | ❌ |
| Reactivate organization | ✅ | ❌ | ❌ |

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/organizations/initiate` | Start organization creation (sends OTP) |
| `POST` | `/api/v1/organizations/verify` | Verify OTP and create organization |
| `GET` | `/api/v1/organizations/{orgId}` | Get organization details |
| `GET` | `/api/v1/organizations/my` | Get user's organizations |
| `GET` | `/api/v1/organizations/{orgId}/members` | List members (emails masked for non-owners) |
| `POST` | `/api/v1/organizations/{orgId}/members` | Add member by user ID |
| `DELETE` | `/api/v1/organizations/{orgId}/members/{id}` | Remove single member |
| `DELETE` | `/api/v1/organizations/{orgId}/members/batch` | Batch remove members |
| `POST` | `/api/v1/organizations/{orgId}/members/{id}/promote` | Promote to admin (Enterprise) |
| `POST` | `/api/v1/organizations/{orgId}/invitations` | Send email invitation (Enterprise) |
| `POST` | `/api/v1/organizations/invitations/{token}/accept` | Accept invitation |
| `DELETE` | `/api/v1/organizations/{orgId}` | Close organization |
| `POST` | `/api/v1/organizations/{orgId}/reactivate` | Reactivate organization |

---

## Data Models

### Organization Entity

```java
Organization {
    String id;                           // UUID primary key
    String name;                         // Organization name
    String description;                  // Optional description
    String orgCode;                      // Unique 8-character code
    OrganizationSubscriptionType subscriptionType;  // PREMIUM or ENTERPRISE
    String paymentGateway;               // "stripe" or "paystack"
    OffsetDateTime trialEndsAt;          // Trial period end date
    boolean hasUsedTrial;                // Whether trial was used
    boolean isActive;                    // Active status
    boolean isVerified;                  // Verified via OTP
    boolean isClosed;                    // Closed status
    Integer maxMembers;                  // Default 10
    Integer monthlyDocumentLimit;        // null for Enterprise (unlimited)
    Integer monthlyDocumentUploadCount;  // Current month's upload count
}
```

### OrganizationMember Entity

```java
OrganizationMember {
    String id;                    // UUID primary key
    Organization organization;   // Parent organization
    User user;                   // Member user
    OrganizationMemberRole role; // OWNER, ADMIN, or MEMBER
    User invitedBy;              // Who invited this member
    OffsetDateTime joinedAt;     // Join timestamp
}
```

---

## Database Tables

The following tables are created by Flyway migration `V30__create_organization_tables.sql`:

- `organizations` - Main organization data
- `organization_members` - Member relationships
- `organization_invitations` - Email invitations (Enterprise)
- `organization_otp_verifications` - OTP verification records

### Indexes

- `idx_organizations_org_code` - Fast org code lookups
- `idx_organizations_created_by` - Creator queries
- `idx_org_members_organization` - Member by org queries
- `idx_org_invitations_token` - Token lookup

---

## Email Templates

| Template | Purpose |
|----------|---------|
| `organization-otp.html` | OTP verification during creation |
| `organization-created.html` | Confirmation after successful creation |
| `organization-invitation.html` | Email invitations (Enterprise) |

---

## Security Features

- **OTP Verification**: 6-digit OTP with 15-minute expiry
- **Email Masking**: Non-owners see masked emails (`j***e@e***e.com`)
- **Role-Based Access**: Permission checks on all operations
- **Redis Caching**: Organization and member data cached
- **Input Validation**: All DTOs have Jakarta validation annotations

---

## Implementation Files

### Controllers
- `OrganizationController.java` - REST endpoints

### Services
- `OrganizationService.java` - Interface
- `OrganizationServiceImpl.java` - Delegating implementation
- `InitiateOrgCreationImpl.java` - OTP generation flow
- `VerifyOtpAndCreateOrgImpl.java` - Organization creation
- `MemberManagementImpl.java` - Add/remove members
- `AdminPromotionImpl.java` - Promote to admin
- `InvitationServiceImpl.java` - Email invitations
- `CloseOrganizationImpl.java` - Close/reactivate
- `OrganizationViewImpl.java` - Read operations

### Repositories
- `OrganizationRepository.java`
- `OrganizationMemberRepository.java`
- `OrganizationInvitationRepository.java`
- `OrganizationOtpRepository.java`

### DTOs
- **Request**: `CreateOrganizationRequest`, `VerifyOrgOtpRequest`, `AddMemberRequest`, `RemoveMembersRequest`, `InviteMemberRequest`, `PromoteToAdminRequest`
- **Response**: `OrganizationResponse`, `OrganizationMemberResponse`
