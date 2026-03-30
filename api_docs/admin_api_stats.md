# Admin Dashboard — API Stats & Management Specification

## 1. Overview

This document defines **every stat, metric, and management action** needed for the UnravelDocs admin dashboard. It is derived directly from the codebase entities, services, and business logic. Each section maps to a dashboard module with its required API endpoints, data points, and admin actions.

> [!NOTE]
> All admin endpoints live under `/api/v1/admin/` and require `ADMIN`, `MODERATOR`, or `SUPER_ADMIN` role. Existing endpoints are marked with ✅. New endpoints to build are marked with 🆕.

---

## 2. Dashboard Home — Key Performance Indicators (KPIs)

The landing page should display at-a-glance KPIs. Each card should show the **current value**, **trend** (vs. previous period), and a **sparkline chart** (last 30 days).

### 2.1 User KPIs

| KPI                                        | Description                                           | Source                 |
|--------------------------------------------|-------------------------------------------------------|------------------------|
| Total Users                                | Count of all registered users                         | `users` table          |
| Active Users                               | Users where `is_active = true AND is_verified = true` | `users` table          |
| New Users (Today / This Week / This Month) | Users created within the time window                  | `users.created_at`     |
| Verified vs Unverified                     | Ratio of `is_verified = true` to `false`              | `users` table          |
| Users Currently Blocked                    | Users with `login_attempts.is_blocked = true`         | `login_attempts` table |
| Daily Active Users (DAU)                   | Users who logged in today                             | `users.last_login`     |
| Weekly Active Users (WAU)                  | Users who logged in within last 7 days                | `users.last_login`     |
| Monthly Active Users (MAU)                 | Users who logged in within last 30 days               | `users.last_login`     |

### 2.2 Revenue KPIs

| KPI                                      | Description                             | Source                         |
|------------------------------------------|-----------------------------------------|--------------------------------|
| Total Revenue (All Time)                 | Sum of all receipt amounts              | `receipts.amount`              |
| Revenue (Today / This Week / This Month) | Filtered by `receipts.paid_at`          | `receipts` table               |
| Revenue by Gateway                       | Breakdown by Stripe / PayPal / Paystack | `receipts.payment_provider`    |
| Revenue by Currency                      | Breakdown by USD / NGN / etc.           | `receipts.currency`            |
| Average Revenue Per User (ARPU)          | Total revenue ÷ total paying users      | Computed                       |
| Monthly Recurring Revenue (MRR)          | Sum of active subscription prices       | `user_subscriptions` + `teams` |

### 2.3 Document KPIs

| KPI                                                 | Description                                     | Source                                   |
|-----------------------------------------------------|-------------------------------------------------|------------------------------------------|
| Total Documents Uploaded                            | Count of all `FileEntry` records                | `document_file_entries` table            |
| Documents Uploaded (Today / This Week / This Month) | Filtered by `created_at`                        | `document_file_entries`                  |
| Total Storage Used                                  | Sum of all `file_size` across all documents     | `document_file_entries.file_size`        |
| Average Document Size                               | Mean `file_size`                                | Computed                                 |
| Documents by Type                                   | Breakdown by `file_type` (PDF, DOCX, PNG, etc.) | `document_file_entries.file_type`        |
| Documents by Status                                 | Count per `DocumentStatus` enum                 | `document_collections.collection_status` |
| Failed Uploads                                      | Count where `upload_status` indicates failure   | `document_file_entries.upload_status`    |
| Encrypted Documents                                 | Count where `is_encrypted = true`               | `document_file_entries.is_encrypted`     |

### 2.4 Subscription KPIs

| KPI                     | Description                                                                               | Source                                         |
|-------------------------|-------------------------------------------------------------------------------------------|------------------------------------------------|
| Users by Plan           | Count per plan: FREE, STARTER_MONTHLY/YEARLY, PRO_MONTHLY/YEARLY, BUSINESS_MONTHLY/YEARLY | `user_subscriptions` JOIN `subscription_plans` |
| Active Subscriptions    | Where `status = 'Active'`                                                                 | `user_subscriptions.status`                    |
| Trial Subscriptions     | Where `status = 'Trial'`                                                                  | `user_subscriptions.status`                    |
| Cancelled Subscriptions | Where `status = 'Cancelled'`                                                              | `user_subscriptions.status`                    |
| Expired Subscriptions   | Where `status = 'Expired'`                                                                | `user_subscriptions.status`                    |
| Churn Rate              | (Cancelled + Expired in period) ÷ Active at start of period                               | Computed                                       |
| Upgrade Rate            | Subscription upgrades per period                                                          | `user_subscriptions.previous_plan_id`          |
| Downgrade Rate          | Subscription downgrades per period                                                        | `user_subscriptions.previous_plan_id`          |

---

## 3. User Management Module

### 3.1 User List & Filtering

✅ **Existing:** `GET /api/v1/admin/users` with `UserFilterDto`

**Required Filters:**

| Filter                               | Type     | Description                               |
|--------------------------------------|----------|-------------------------------------------|
| `role`                               | Enum     | USER, MODERATOR, ADMIN, SUPER_ADMIN       |
| `isActive`                           | Boolean  | Active status                             |
| `isVerified`                         | Boolean  | Email verification status                 |
| `isPlatformAdmin`                    | Boolean  | Platform admin flag                       |
| `isOrganizationAdmin`                | Boolean  | Organization admin flag                   |
| `country`                            | String   | Filter by country                         |
| `profession`                         | String   | Filter by profession                      |
| `organization`                       | String   | Filter by organization                    |
| `subscriptionPlan`                   | Enum     | Filter by current subscription tier       |
| `createdAfter` / `createdBefore`     | DateTime | Registration date range                   |
| `lastLoginAfter` / `lastLoginBefore` | DateTime | Last login date range                     |
| `hasTeam`                            | Boolean  | Whether user belongs to a team            |
| `isBlocked`                          | Boolean  | Whether login is currently blocked        |
| `search`                             | String   | Full-text search on name / email          |
| `sortBy`                             | String   | `createdAt`, `lastLogin`, `name`, `email` |
| `sortDirection`                      | String   | `ASC` / `DESC`                            |

### 3.2 User Detail View

✅ **Existing:** `GET /api/v1/admin/{userId}`

**Data Points to Display:**

| Section            | Fields                                                                                                                                       |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| **Profile**        | ID, name, email, profile picture, country, profession, organization, role, created at, last login                                            |
| **Status Flags**   | isActive, isVerified, isPlatformAdmin, isOrganizationAdmin, termsAccepted, marketingOptIn                                                    |
| **Subscription**   | Current plan, status, billing period dates, auto-renew, trial status, subscription source (INDIVIDUAL/TEAM), payment gateway subscription ID |
| **Usage Quotas**   | Storage used / limit, OCR pages used / limit, AI operations used / limit, monthly documents uploaded / limit, quota reset date               |
| **Credit Balance** | Current balance, total purchased, total used                                                                                                 |
| **Documents**      | Total collections, total files, total storage consumed, recent uploads                                                                       |
| **Teams**          | Teams owned, teams member of, team roles                                                                                                     |
| **Login Security** | Failed login attempts, is blocked, blocked until                                                                                             |
| **Notifications**  | Total notifications, unread count, notification preferences                                                                                  |
| **Receipts**       | Total payments, total amount paid, recent receipts                                                                                           |

### 3.3 User Admin Actions

| Action                      | Endpoint                                  | Method | Description                                                 |
|-----------------------------|-------------------------------------------|--------|-------------------------------------------------------------|
| ✅ Change Role               | `/api/v1/admin/change-role`               | PUT    | Change user role to ADMIN / MODERATOR                       |
| 🆕 Activate/Deactivate User | `/api/v1/admin/users/{id}/status`         | PATCH  | Toggle `isActive`                                           |
| 🆕 Force Verify Email       | `/api/v1/admin/users/{id}/verify`         | PATCH  | Set `isVerified = true`                                     |
| 🆕 Unlock Blocked User      | `/api/v1/admin/users/{id}/unlock`         | PATCH  | Reset login attempts, unblock                               |
| 🆕 Reset Password           | `/api/v1/admin/users/{id}/reset-password` | POST   | Trigger password reset email                                |
| 🆕 Delete/Soft-Delete User  | `/api/v1/admin/users/{id}`                | DELETE | Set `deleted_at` timestamp                                  |
| 🆕 Impersonate User         | `/api/v1/admin/users/{id}/impersonate`    | POST   | Generate a temporary auth token for the user (audit-logged) |
| ✅ Allocate Credits          | `/api/v1/admin/credits/allocate`          | POST   | Grant credits to a user                                     |
| 🆕 Adjust Subscription      | `/api/v1/admin/users/{id}/subscription`   | PUT    | Manually upgrade/downgrade plan                             |
| 🆕 Reset Usage Quotas       | `/api/v1/admin/users/{id}/reset-quotas`   | POST   | Reset monthly OCR / AI / document counters                  |

### 3.4 User Statistics & Charts

| Chart                     | Type                        | Description                              |
|---------------------------|-----------------------------|------------------------------------------|
| User Growth Over Time     | Line chart                  | New registrations per day / week / month |
| Users by Country          | Choropleth map or bar chart | Geographic distribution                  |
| Users by Profession       | Pie/donut chart             | Profession breakdown                     |
| Users by Organization     | Bar chart                   | Top organizations                        |
| Users by Role             | Pie chart                   | Role distribution                        |
| Active vs Inactive        | Gauge or donut              | Active ratio                             |
| Verified vs Unverified    | Gauge or donut              | Verification ratio                       |
| Login Activity Heatmap    | Heatmap                     | Login frequency by day/hour              |
| Retention Cohort Analysis | Cohort table                | Monthly user retention                   |
| Marketing Opt-In Rate     | Gauge                       | % opted in to marketing                  |

---

## 4. Subscription & Billing Module

### 4.1 Subscription Stats

🆕 **Endpoint:** `GET /api/v1/admin/subscriptions/stats`

```json
{
  "totalSubscriptions": 1500,
  "byPlan": {
    "FREE": 800,
    "STARTER_MONTHLY": 200,
    "STARTER_YEARLY": 100,
    "PRO_MONTHLY": 150,
    "PRO_YEARLY": 80,
    "BUSINESS_MONTHLY": 50,
    "BUSINESS_YEARLY": 120
  },
  "byStatus": {
    "ACTIVE": 700,
    "TRIAL": 200,
    "CANCELLED": 100,
    "EXPIRED": 500
  },
  "bySource": {
    "INDIVIDUAL": 1200,
    "TEAM": 300
  },
  "trialConversionRate": 45.2,
  "churnRate": 5.3,
  "mrr": 12500.00,
  "averageRevenuePerUser": 8.33,
  "usersNearingQuotaLimits": {
    "storage": 45,
    "ocrPages": 23,
    "aiOperations": 12,
    "documentUploads": 67
  }
}
```

### 4.2 Subscription Plan Management

🆕 **Endpoints:**

| Action             | Endpoint                                             | Method | Description                           |
|--------------------|------------------------------------------------------|--------|---------------------------------------|
| List Plans         | `/api/v1/admin/subscriptions/plans`                  | GET    | All plans with limits and pricing     |
| Update Plan Limits | `/api/v1/admin/subscriptions/plans/{id}`             | PUT    | Modify document/OCR/storage/AI limits |
| Toggle Plan Active | `/api/v1/admin/subscriptions/plans/{id}/status`      | PATCH  | Activate / deactivate a plan          |
| Plan Subscribers   | `/api/v1/admin/subscriptions/plans/{id}/subscribers` | GET    | List users on a specific plan         |

### 4.3 Subscription Charts

| Chart                          | Type               | Description                                                    |
|--------------------------------|--------------------|----------------------------------------------------------------|
| Plan Distribution              | Pie chart          | Users per plan                                                 |
| Subscription Trend             | Stacked area chart | Active/trial/cancelled over time                               |
| Revenue Trend                  | Line chart         | MRR over months                                                |
| Trial → Paid Conversion Funnel | Funnel chart       | Trial starts → Active conversions                              |
| Churn Trend                    | Line chart         | Monthly churn rate                                             |
| Upgrade/Downgrade Flow         | Sankey diagram     | Plan migration paths                                           |
| Quota Utilization Distribution | Histogram          | How many users are at 0-25%, 25-50%, 50-75%, 75-100% of quotas |

---

## 5. Payment & Revenue Module

### 5.1 Payment Stats

🆕 **Endpoint:** `GET /api/v1/admin/payments/stats`

**Required Data Points:**

| Stat                      | Description                                    |
|---------------------------|------------------------------------------------|
| Total Transactions        | Count of all receipts                          |
| Total Revenue             | Sum of all `receipts.amount`                   |
| Revenue by Provider       | Stripe / PayPal / Paystack breakdown           |
| Revenue by Currency       | USD / NGN / etc.                               |
| Revenue by Payment Method | Card / bank transfer / etc.                    |
| Average Transaction Value | Mean receipt amount                            |
| Failed Payment Rate       | From `PaymentMetrics` — failed ÷ total         |
| Webhook Processing Stats  | Success / failure counts from `PaymentMetrics` |
| Refund Count & Total      | Count + sum of refund transactions             |
| Pending Receipts          | Receipts where `email_sent = false`            |

### 5.2 Payment List & Filtering

🆕 **Endpoint:** `GET /api/v1/admin/payments`

**Filters:** `userId`, `provider`, `currency`, `dateRange`, `minAmount`, `maxAmount`, `sortBy`

### 5.3 Receipt Management

🆕 **Endpoints:**

| Action               | Endpoint                               | Method | Description              |
|----------------------|----------------------------------------|--------|--------------------------|
| List All Receipts    | `/api/v1/admin/receipts`               | GET    | Paginated with filters   |
| View Receipt         | `/api/v1/admin/receipts/{id}`          | GET    | Full receipt detail      |
| Resend Receipt Email | `/api/v1/admin/receipts/{id}/resend`   | POST   | Re-trigger receipt email |
| Download Receipt PDF | `/api/v1/admin/receipts/{id}/download` | GET    | Generate + return PDF    |

### 5.4 Revenue Charts

| Chart                           | Type              | Description                           |
|---------------------------------|-------------------|---------------------------------------|
| Daily/Weekly/Monthly Revenue    | Line chart        | Revenue over time                     |
| Revenue by Gateway              | Stacked bar chart | Stripe vs PayPal vs Paystack          |
| Revenue by Plan                 | Bar chart         | Revenue contributed by each plan tier |
| Transaction Volume              | Line chart        | Number of transactions over time      |
| Average Transaction Value Trend | Line chart        | ATV over time                         |
| Revenue by Country              | Map / bar         | Top revenue-generating countries      |
| Payment Failure Rate            | Line chart        | % failures over time                  |

---

## 6. Team Management Module

### 6.1 Team Stats

🆕 **Endpoint:** `GET /api/v1/admin/teams/stats`

| Stat                                     | Description                                            |
|------------------------------------------|--------------------------------------------------------|
| Total Teams                              | Count of all teams                                     |
| Active Teams                             | `is_active = true AND is_closed = false`               |
| Closed Teams                             | `is_closed = true`                                     |
| Teams by Subscription Type               | TEAM_PREMIUM / TEAM_ENTERPRISE                         |
| Teams by Subscription Status             | TRIAL / ACTIVE / CANCELLED / EXPIRED / PAST_DUE        |
| Teams by Billing Cycle                   | MONTHLY / YEARLY                                       |
| Teams by Payment Gateway                 | Stripe / Paystack                                      |
| Average Team Size                        | Mean member count                                      |
| Total Team Members                       | Sum of all team members                                |
| Pending Invitations                      | Count of `team_invitations` where `status = 'PENDING'` |
| Expired Invitations                      | Count where status = EXPIRED or past `expires_at`      |
| Teams in Trial                           | Active trial teams + days remaining                    |
| Past Due Teams                           | Teams with `past_due_since` set                        |
| Total Team Storage Used                  | Sum of all `teams.storage_used`                        |
| Average Monthly Document Uploads (Teams) | Mean of `teams.monthly_document_upload_count`          |

### 6.2 Team List & Detail

🆕 **Endpoints:**

| Action           | Endpoint                               | Method | Description                            |
|------------------|----------------------------------------|--------|----------------------------------------|
| List Teams       | `/api/v1/admin/teams`                  | GET    | Paginated with filters                 |
| Team Detail      | `/api/v1/admin/teams/{id}`             | GET    | Full team info + members + invitations |
| Team Members     | `/api/v1/admin/teams/{id}/members`     | GET    | Members with roles                     |
| Team Invitations | `/api/v1/admin/teams/{id}/invitations` | GET    | All invitations with status            |

**Team Detail Data:**

| Section            | Fields                                                                                                            |
|--------------------|-------------------------------------------------------------------------------------------------------------------|
| **Info**           | Name, description, team code, created at, created by                                                              |
| **Subscription**   | Type, status, billing cycle, payment gateway, price, currency, plan reference                                     |
| **Trial**          | Trial ends at, has used trial, trial reminder sent, days remaining                                                |
| **Billing**        | Next billing date, last billing date, auto renew, cancellation requested at, subscription ends at, past due since |
| **Limits & Usage** | Max members, current member count, monthly document limit, monthly uploads, storage limit, storage used           |
| **Members**        | List with user name, email, role (OWNER/ADMIN/MEMBER), joined at                                                  |
| **Invitations**    | Email, status (PENDING/ACCEPTED/EXPIRED/CANCELLED), created at, expires at                                        |

### 6.3 Team Admin Actions

| Action                       | Endpoint                                        | Method | Description                   |
|------------------------------|-------------------------------------------------|--------|-------------------------------|
| 🆕 Activate/Deactivate Team  | `/api/v1/admin/teams/{id}/status`               | PATCH  | Toggle `is_active`            |
| 🆕 Close Team                | `/api/v1/admin/teams/{id}/close`                | POST   | Set `is_closed` + `closed_at` |
| 🆕 Extend Trial              | `/api/v1/admin/teams/{id}/extend-trial`         | POST   | Push `trial_ends_at` forward  |
| 🆕 Change Team Subscription  | `/api/v1/admin/teams/{id}/subscription`         | PUT    | Manually change plan/status   |
| 🆕 Remove Team Member        | `/api/v1/admin/teams/{id}/members/{memberId}`   | DELETE | Force-remove a member         |
| 🆕 Cancel Invitation         | `/api/v1/admin/teams/{id}/invitations/{invId}`  | DELETE | Cancel pending invitation     |
| 🆕 Reset Team Document Count | `/api/v1/admin/teams/{id}/reset-document-count` | POST   | Reset monthly upload counter  |

### 6.4 Team Subscription Plan Management

🆕 **Endpoints:**

| Action           | Endpoint                               | Method | Description                      |
|------------------|----------------------------------------|--------|----------------------------------|
| List Team Plans  | `/api/v1/admin/team-plans`             | GET    | All team subscription plans      |
| Update Team Plan | `/api/v1/admin/team-plans/{id}`        | PUT    | Modify pricing, limits, features |
| Toggle Team Plan | `/api/v1/admin/team-plans/{id}/status` | PATCH  | Activate / deactivate            |

---

## 7. Coupon Management Module

### 7.1 Coupon Stats

🆕 **Endpoint:** `GET /api/v1/admin/coupons/stats`

| Stat                                    | Description                                        |
|-----------------------------------------|----------------------------------------------------|
| Total Coupons Created                   | Count of all coupons                               |
| Active Coupons                          | Where `is_active = true` and within validity dates |
| Expired Coupons                         | Past `valid_until`                                 |
| Total Coupon Usages                     | Sum of `current_usage_count`                       |
| Total Discount Given                    | Sum from `coupon_analytics.total_discount_amount`  |
| Total Original Amount (Before Discount) | Sum from `coupon_analytics.total_original_amount`  |
| Total Final Amount (After Discount)     | Sum from `coupon_analytics.total_final_amount`     |
| Unique Users Who Used Coupons           | Distinct user count from `coupon_usages`           |
| Average Discount Percentage             | Mean of `coupons.discount_percentage`              |
| Coupons Near Expiry                     | Active coupons expiring within 7 days              |
| Coupons at Usage Limit                  | Where `current_usage_count >= max_usage_count`     |
| Coupon Usage by Recipient Category      | ALL_PAID_USERS / specific categories               |
| Bulk Generation Jobs                    | Active / completed / failed bulk jobs              |

### 7.2 Existing Coupon Endpoints

| Action            | Endpoint                                      | Status |
|-------------------|-----------------------------------------------|--------|
| Create Coupon     | `POST /api/v1/admin/coupons`                  | ✅      |
| Update Coupon     | `PUT /api/v1/admin/coupons/{id}`              | ✅      |
| Deactivate Coupon | `DELETE /api/v1/admin/coupons/{id}`           | ✅      |
| Get Coupon        | `GET /api/v1/admin/coupons/{id}`              | ✅      |
| List Coupons      | `GET /api/v1/admin/coupons`                   | ✅      |
| Coupon Usage      | `GET /api/v1/admin/coupons/{id}/usage`        | ✅      |
| Coupon Analytics  | `GET /api/v1/admin/coupons/{id}/analytics`    | ✅      |
| Bulk Generate     | `POST /api/v1/admin/coupons/bulk-generate`    | ✅      |
| Bulk Job Status   | `GET /api/v1/admin/coupons/bulk-jobs/{jobId}` | ✅      |

### 7.3 Coupon Charts

| Chart                      | Type           | Description                        |
|----------------------------|----------------|------------------------------------|
| Coupon Usage Over Time     | Line chart     | Daily/weekly usage trend           |
| Discount Revenue Impact    | Bar chart      | Original vs discounted revenue     |
| Top Coupons by Usage       | Horizontal bar | Most-used coupon codes             |
| Usage by Subscription Plan | Stacked bar    | Which plans use coupons most       |
| Coupon Lifecycle           | Gantt-style    | Validity periods of active coupons |

---

## 8. Credit System Module

### 8.1 Credit Stats

🆕 **Endpoint:** `GET /api/v1/admin/credits/stats`

| Stat                         | Description                                              |
|------------------------------|----------------------------------------------------------|
| Total Credits in Circulation | Sum of all `user_credit_balances.balance`                |
| Total Credits Purchased      | Sum of all `user_credit_balances.total_purchased`        |
| Total Credits Used           | Sum of all `user_credit_balances.total_used`             |
| Total Credit Transactions    | Count of `credit_transactions`                           |
| Transactions by Type         | PURCHASE / DEDUCTION / REFUND / BONUS / ADMIN_ALLOCATION |
| Revenue from Credit Packs    | Sum of credit pack purchase amounts                      |
| Active Credit Packs          | Packs where `is_active = true`                           |
| Top Credit Pack by Sales     | Most purchased pack                                      |
| Users with Zero Balance      | Users who have exhausted credits                         |
| Average Credit Balance       | Mean balance across users                                |

### 8.2 Existing Credit Endpoints

| Action             | Endpoint                                  | Status |
|--------------------|-------------------------------------------|--------|
| Create Credit Pack | `POST /api/v1/admin/credits/packs`        | ✅      |
| Update Credit Pack | `PUT /api/v1/admin/credits/packs/{id}`    | ✅      |
| Deactivate Pack    | `DELETE /api/v1/admin/credits/packs/{id}` | ✅      |
| List All Packs     | `GET /api/v1/admin/credits/packs`         | ✅      |
| Get Pack by ID     | `GET /api/v1/admin/credits/packs/{id}`    | ✅      |
| Allocate Credits   | `POST /api/v1/admin/credits/allocate`     | ✅      |

### 8.3 New Credit Endpoints

| Action                    | Endpoint                                            | Method | Description                               |
|---------------------------|-----------------------------------------------------|--------|-------------------------------------------|
| 🆕 Credit Transaction Log | `/api/v1/admin/credits/transactions`                | GET    | Paginated, filterable transaction history |
| 🆕 User Credit History    | `/api/v1/admin/credits/users/{userId}/transactions` | GET    | Transactions for a specific user          |
| 🆕 Bulk Allocate Credits  | `/api/v1/admin/credits/bulk-allocate`               | POST   | Allocate to multiple users at once        |
| 🆕 Revoke Credits         | `/api/v1/admin/credits/revoke`                      | POST   | Deduct credits from a user with reason    |

---

## 9. Document & OCR Operations Module

### 9.1 Document Stats

🆕 **Endpoint:** `GET /api/v1/admin/documents/stats`

| Stat                         | Description                                                                                         |
|------------------------------|-----------------------------------------------------------------------------------------------------|
| Total Document Collections   | Count of `document_collections`                                                                     |
| Total Files                  | Count of all `FileEntry` records                                                                    |
| Total Storage Consumed       | Sum of `file_size` (display in GB/TB)                                                               |
| Files by Type                | PDF, DOCX, PNG, JPG, TIFF, etc. breakdown                                                           |
| Files by Status              | UPLOADED, COMPLETED, PARTIALLY_COMPLETED, FAILED_UPLOAD, PROCESSING, PROCESSED, FAILED_OCR, DELETED |
| Encrypted Documents Count    | Where `is_encrypted = true`                                                                         |
| Average Files Per Collection | Mean file count per collection                                                                      |
| Upload Success Rate          | (Total - FAILED_UPLOAD) ÷ Total                                                                     |
| Largest Documents            | Top N files by `file_size`                                                                          |
| Most Active Uploaders        | Users with most document uploads                                                                    |

### 9.2 OCR Stats

🆕 **Endpoint:** `GET /api/v1/admin/ocr/stats`

| Stat                     | Description                         | Source                                        |
|--------------------------|-------------------------------------|-----------------------------------------------|
| Total OCR Requests       | All OCR operations                  | `OcrMetrics` → `ocr.requests.total`           |
| OCR Success Rate         | Successful ÷ Total                  | `ocr.requests.success` / `ocr.requests.total` |
| OCR Error Rate           | Failed ÷ Total                      | `ocr.requests.errors` / `ocr.requests.total`  |
| OCR by Provider          | TESSERACT vs GOOGLE_VISION          | Tag: `provider`                               |
| Average Processing Time  | P50 / P95 / P99 latency             | `ocr.requests.duration`                       |
| Average Confidence Score | Mean OCR confidence                 | `ocr.confidence`                              |
| Characters Extracted     | Total / average per request         | `ocr.characters.extracted`                    |
| Fallback Rate            | How often fallback provider is used | `ocr.fallbacks.total`                         |
| Quota Usage by Tier      | OCR usage per subscription tier     | `ocr.quota.usage`                             |
| Quota Exceeded Events    | Users hitting OCR limits            | `ocr.quota.exceeded`                          |
| Errors by Type           | Breakdown of error categories       | `ocr.errors.by.type`                          |

### 9.3 Document Admin Actions

| Action                  | Endpoint                                 | Method | Description                             |
|-------------------------|------------------------------------------|--------|-----------------------------------------|
| 🆕 List All Documents   | `/api/v1/admin/documents`                | GET    | Paginated with user/type/status filters |
| 🆕 View Document Detail | `/api/v1/admin/documents/{id}`           | GET    | Collection detail with all files        |
| 🆕 Delete Document      | `/api/v1/admin/documents/{id}`           | DELETE | Force-delete a document collection      |
| 🆕 Reprocess Document   | `/api/v1/admin/documents/{id}/reprocess` | POST   | Re-run OCR on a failed document         |
| ✅ Storage Migration     | `POST /api/v1/admin/storage/migrate`     | POST   | Recalculate storage for all users       |

---

## 10. AI Operations Module

### 10.1 AI Stats

🆕 **Endpoint:** `GET /api/v1/admin/ai/stats`

| Stat                       | Description                                                                           |
|----------------------------|---------------------------------------------------------------------------------------|
| Total AI Operations        | All AI operations (summaries + classifications)                                       |
| AI Operations by Type      | Short summary / detailed summary / classification                                     |
| AI Operations by Provider  | OPENAI / MISTRAL_AI                                                                   |
| AI Fallback Rate           | When primary provider fails and fallback is used                                      |
| Average AI Processing Time | Latency per operation type                                                            |
| AI Quota Usage by Plan     | Usage per subscription tier                                                           |
| Credit Cost Consumed       | Total credits spent on AI (1 for short summary, 2 for detailed, 1 for classification) |
| AI Error Rate              | Failed operations ÷ total                                                             |
| Users Approaching AI Quota | Users at >80% of `ai_operations_limit`                                                |

---

## 11. Storage Management Module

### 11.1 Storage Stats

🆕 **Endpoint:** `GET /api/v1/admin/storage/stats`

| Stat                          | Description                                                         |
|-------------------------------|---------------------------------------------------------------------|
| Total Storage Used (Platform) | Sum of all `user_subscriptions.storage_used` + `teams.storage_used` |
| Storage by Plan Tier          | Breakdown per subscription tier                                     |
| Users Near Storage Limit      | Users at >80% of their plan's `storage_limit`                       |
| Teams Near Storage Limit      | Teams at >80% of their `storage_limit`                              |
| Average Storage Per User      | Mean `storage_used`                                                 |
| Storage Growth Rate           | Storage delta per day/week/month                                    |
| S3 Bucket Stats               | Total objects, total size, bucket region                            |

### 11.2 Storage Charts

| Chart                        | Type        | Description                      |
|------------------------------|-------------|----------------------------------|
| Storage Growth Over Time     | Area chart  | Cumulative storage usage         |
| Storage Distribution by Plan | Stacked bar | Which plans consume most storage |
| Top Storage Consumers        | Bar chart   | Users/teams using most storage   |
| Storage Utilization Heatmap  | Heatmap     | % capacity used per tier         |

---

## 12. Notification Management Module

### 12.1 Notification Stats

🆕 **Endpoint:** `GET /api/v1/admin/notifications/stats`

| Stat                              | Description                                  |
|-----------------------------------|----------------------------------------------|
| Total Notifications Sent          | Count of `notifications`                     |
| Notifications by Type             | Per `NotificationType` enum                  |
| Read vs Unread                    | Count of `is_read = true` vs `false`         |
| Average Read Rate                 | % of notifications read                      |
| Registered Devices                | Count of `user_device_tokens`                |
| Devices by Type                   | iOS / Android / Web                          |
| Active Notification Provider      | FCM / OneSignal / AWS SNS                    |
| Storage Warning Notifications     | Sent via `StorageWarningNotificationJob`     |
| Subscription Expiry Notifications | Sent via `SubscriptionExpiryNotificationJob` |
| Stale Tokens Cleaned              | Count from `StaleTokenCleanupJob`            |

### 12.2 Notification Admin Actions

| Action                         | Endpoint                                | Method | Description                                    |
|--------------------------------|-----------------------------------------|--------|------------------------------------------------|
| 🆕 Send Broadcast Notification | `/api/v1/admin/notifications/broadcast` | POST   | Push notification to all users or a segment    |
| 🆕 Send Targeted Notification  | `/api/v1/admin/notifications/send`      | POST   | Push to specific user(s)                       |
| 🆕 Notification History        | `/api/v1/admin/notifications`           | GET    | Paginated list with filters                    |
| 🆕 Delete Old Notifications    | `/api/v1/admin/notifications/cleanup`   | POST   | Force cleanup (normally runs as scheduled job) |

---

## 13. Security & Access Module

### 13.1 Security Stats

🆕 **Endpoint:** `GET /api/v1/admin/security/stats`

| Stat                                 | Description                             |
|--------------------------------------|-----------------------------------------|
| Currently Blocked Users              | `login_attempts.is_blocked = true`      |
| Total Blocked Events (Today)         | Users blocked in the current day        |
| Failed Login Attempts (Today / Week) | Sum of `login_attempts.login_attempts`  |
| Users by Block Duration              | How long users remain blocked           |
| Rate Limit Violations                | From `rate-limit` metrics               |
| Rate Limit Violations by Tier        | FREE / STARTER / PRO / BUSINESS         |
| Active Sessions                      | Currently valid JWT tokens (if tracked) |
| OTPs Generated                       | From `AdminController.generateOtp`      |
| Active OTPs                          | From `AdminController.fetchActiveOtps`  |

### 13.2 Existing Security Endpoints

| Action            | Endpoint                          | Status |
|-------------------|-----------------------------------|--------|
| Generate OTP      | `POST /api/v1/admin/generate-otp` | ✅      |
| Fetch Active OTPs | `GET /api/v1/admin/active-otps`   | ✅      |

### 13.3 New Security Actions

| Action                      | Endpoint                                              | Method | Description                          |
|-----------------------------|-------------------------------------------------------|--------|--------------------------------------|
| 🆕 View Blocked Users       | `/api/v1/admin/security/blocked-users`                | GET    | List of currently blocked users      |
| 🆕 Unblock User             | `/api/v1/admin/security/unblock/{userId}`             | POST   | Reset block + attempts               |
| 🆕 View Login Attempt Logs  | `/api/v1/admin/security/login-attempts`               | GET    | Paginated log of all failed logins   |
| 🆕 Invalidate User Sessions | `/api/v1/admin/security/sessions/{userId}/invalidate` | POST   | Force logout everywhere              |
| 🆕 Rate Limit Override      | `/api/v1/admin/security/rate-limit/{userId}`          | PUT    | Temporarily adjust limits for a user |
| 🆕 Security Audit Log       | `/api/v1/admin/security/audit-log`                    | GET    | Admin action audit trail             |

---

## 14. Kafka & Messaging Module

### 14.1 Messaging Stats

🆕 **Endpoint:** `GET /api/v1/admin/messaging/stats`

| Stat                          | Description               | Source                                           |
|-------------------------------|---------------------------|--------------------------------------------------|
| Messages Sent                 | Total messages published  | `KafkaMetrics` → `kafka.messaging.messages.sent` |
| Messages Received             | Total messages consumed   | `kafka.messaging.messages.received`              |
| Messages Failed               | Failed sends              | `kafka.messaging.messages.failed`                |
| Messages to DLQ               | Dead letter queue entries | `kafka.messaging.messages.dlq`                   |
| Send Latency (P50/P95/P99)    | Publishing latency        | `kafka.messaging.send.latency`                   |
| Process Latency (P50/P95/P99) | Consumer processing time  | `kafka.messaging.process.latency`                |
| Consumer Lag                  | From kafka_exporter       | `kafka_consumergroup_lag_sum`                    |
| Active Topics                 | List of known topics      | Kafka Admin API                                  |
| Broker Status                 | UP / DOWN                 | `kafka_brokers` metric                           |

---

## 15. Elasticsearch & Search Module

### 15.1 Search Stats

🆕 **Endpoint:** `GET /api/v1/admin/search/stats`

| Stat                    | Description                  |
|-------------------------|------------------------------|
| Cluster Health          | GREEN / YELLOW / RED         |
| Total Indexed Documents | From ES `_count`             |
| Index Size              | Total shard size in GB       |
| Search Query Rate       | Queries per second / minute  |
| Average Search Latency  | Mean query response time     |
| Index Operations Rate   | Documents indexed per second |
| Failed Queries          | Error count                  |

### 15.2 Existing Search Endpoints

✅ **Admin search controller** already exists at `/api/v1/admin/search/`

---

## 16. System Health Module

### 16.1 System Health Dashboard

🆕 **Endpoint:** `GET /api/v1/admin/system/health`

This should aggregate data from the monitoring stack (see `system_metrics.md`):

| Component           | Health Check                                    | Source                           |
|---------------------|-------------------------------------------------|----------------------------------|
| API Server          | UP / DOWN, uptime, version                      | Actuator `/health`               |
| PostgreSQL          | Connected, active connections, pool utilization | HikariCP metrics + `pg_up`       |
| Redis               | Connected, memory usage, hit rate               | `redis_up` + redis_exporter      |
| Kafka               | Broker status, consumer lag                     | `kafka_brokers` + kafka_exporter |
| Elasticsearch       | Cluster health, index count                     | ES `_cluster/health`             |
| S3 (LocalStack/AWS) | Bucket accessible                               | S3 health check                  |
| JVM                 | Heap usage, GC activity, thread count           | Micrometer JVM metrics           |
| Host                | CPU, memory, disk                               | Node Exporter metrics            |

### 16.2 System Admin Actions

| Action                   | Endpoint                              | Method | Description                              |
|--------------------------|---------------------------------------|--------|------------------------------------------|
| 🆕 Trigger Manual GC     | `/api/v1/admin/system/gc`             | POST   | Suggest JVM garbage collection           |
| 🆕 Clear Redis Cache     | `/api/v1/admin/system/cache/clear`    | POST   | Flush specific or all Redis keys         |
| 🆕 Reindex Elasticsearch | `/api/v1/admin/system/search/reindex` | POST   | Trigger full re-indexing                 |
| 🆕 Application Info      | `/api/v1/admin/system/info`           | GET    | Version, build time, uptime, environment |
| 🆕 Configuration Review  | `/api/v1/admin/system/config`         | GET    | Non-sensitive configuration values       |

---

## 17. Audit Log Module

### 17.1 Audit Trail

🆕 **Endpoint:** `GET /api/v1/admin/audit-log`

Every admin action should be audit-logged:

| Field         | Description                                                                 |
|---------------|-----------------------------------------------------------------------------|
| `id`          | Unique log entry ID                                                         |
| `adminUserId` | Admin who performed the action                                              |
| `adminEmail`  | Admin's email                                                               |
| `action`      | Action type (e.g., `USER_ROLE_CHANGED`, `USER_BLOCKED`, `CREDIT_ALLOCATED`) |
| `targetType`  | Entity type (USER, TEAM, COUPON, CREDIT_PACK, etc.)                         |
| `targetId`    | ID of the affected entity                                                   |
| `details`     | JSON payload with before/after values                                       |
| `ipAddress`   | Request IP address                                                          |
| `userAgent`   | Browser/client user agent                                                   |
| `timestamp`   | When the action occurred                                                    |

**Filters:** `adminId`, `action`, `targetType`, `targetId`, `dateRange`

---

## 18. Reporting & Export Module

### 18.1 Report Generation

🆕 **Endpoints:**

| Report              | Endpoint                              | Format    | Description                            |
|---------------------|---------------------------------------|-----------|----------------------------------------|
| User Report         | `/api/v1/admin/reports/users`         | CSV/Excel | All users with subscriptions and usage |
| Revenue Report      | `/api/v1/admin/reports/revenue`       | CSV/Excel | All transactions with breakdown        |
| Subscription Report | `/api/v1/admin/reports/subscriptions` | CSV/Excel | Plan distribution and status           |
| Team Report         | `/api/v1/admin/reports/teams`         | CSV/Excel | All teams with members and billing     |
| Coupon Report       | `/api/v1/admin/reports/coupons`       | CSV/Excel | Coupon usage and discount impact       |
| Credit Report       | `/api/v1/admin/reports/credits`       | CSV/Excel | Credit transactions and balances       |
| Document Report     | `/api/v1/admin/reports/documents`     | CSV/Excel | Upload volumes and storage             |

**Common Parameters:** `startDate`, `endDate`, `format` (CSV / XLSX)

---

## 19. Complete Endpoint Summary

### Existing Endpoints (✅)

| #  | Endpoint                                  | Method         |
|----|-------------------------------------------|----------------|
| 1  | `/api/v1/admin/signup`                    | POST           |
| 2  | `/api/v1/admin/change-role`               | PUT            |
| 3  | `/api/v1/admin/users`                     | GET            |
| 4  | `/api/v1/admin/{userId}`                  | GET            |
| 5  | `/api/v1/admin/generate-otp`              | POST           |
| 6  | `/api/v1/admin/active-otps`               | GET            |
| 7  | `/api/v1/admin/coupons`                   | GET/POST       |
| 8  | `/api/v1/admin/coupons/{id}`              | GET/PUT/DELETE |
| 9  | `/api/v1/admin/coupons/{id}/usage`        | GET            |
| 10 | `/api/v1/admin/coupons/{id}/analytics`    | GET            |
| 11 | `/api/v1/admin/coupons/bulk-generate`     | POST           |
| 12 | `/api/v1/admin/coupons/bulk-jobs/{jobId}` | GET            |
| 13 | `/api/v1/admin/credits/packs`             | GET/POST       |
| 14 | `/api/v1/admin/credits/packs/{id}`        | GET/PUT/DELETE |
| 15 | `/api/v1/admin/credits/allocate`          | POST           |
| 16 | `/api/v1/admin/storage/migrate`           | POST           |

### New Endpoints Required (🆕) — 50+ endpoints

Grouped by module, covering stats aggregation, CRUD management, bulk operations, reporting, and system administration as defined in sections 2–18 above.

---

## 20. Coupon Admin API (Phase 6A)

### 20.1 Get Coupon Statistics
**Endpoint**: `GET /api/v1/admin/coupons/stats`  
**Description**: Fetches aggregated coupon metrics including total usage and discount figures.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Coupon stats retrieved successfully",
  "data": {
    "totalCoupons": 150,
    "activeCoupons": 120,
    "expiredCoupons": 20,
    "totalUsages": 1850,
    "totalDiscountGiven": 8500.00,
    "uniqueUsersWhoUsedCoupons": 950,
    "averageDiscountPercentage": 15.5,
    "couponsNearExpiry": 5,
    "couponsAtUsageLimit": 2
  }
}
```

---

## 21. Credit Admin API (Phase 6B)

### 21.1 Get Credit Statistics
**Endpoint**: `GET /api/v1/admin/credits/stats`  
**Description**: Fetches aggregated credit metrics including usage, circulation, and transactions.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Credit stats retrieved successfully",
  "data": {
    "totalCreditsInCirculation": 50000,
    "totalCreditsPurchased": 120000,
    "totalCreditsUsed": 70000,
    "totalCreditTransactions": 1450,
    "transactionsByType": {
      "PURCHASE": 500,
      "DEDUCTION": 800,
      "BONUS": 100,
      "REFUND": 50
    },
    "activeCreditPacks": 3,
    "usersWithZeroBalance": 1250,
    "averageCreditBalance": 52.5
  }
}
```

---

## 22. Document Admin API (Phase 6C)

### 22.1 Get Document Statistics
**Endpoint**: `GET /api/v1/admin/documents/stats`  
**Description**: Fetches aggregated document, file and storage metrics.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document stats retrieved successfully",
  "data": {
    "totalCollections": 100,
    "totalFiles": 500,
    "totalStorageBytes": 1024000,
    "filesByType": {
      "application/pdf": 400,
      "image/jpeg": 100
    },
    "filesByStatus": {
      "COMPLETED": 450,
      "FAILED": 50
    },
    "encryptedDocuments": 50,
    "averageFilesPerCollection": 5.0,
    "uploadSuccessRate": 90.0
}
```

---

## 23. Notification Admin API (Phase 6D)

### 23.1 Get Notification Statistics
**Endpoint**: `GET /api/v1/admin/notifications/stats`  
**Description**: Fetches aggregated notification engagement and device metrics.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Notification stats retrieved successfully",
  "data": {
    "totalNotifications": 1000,
    "notificationsByType": {
      "OCR_COMPLETE": 700,
      "ACCOUNT_ALERT": 300
    },
    "readCount": 600,
    "unreadCount": 400,
    "readRate": 60.0,
    "registeredDevices": 250,
    "devicesByType": {
      "ios": 150,
      "android": 100
    }
}
```

---

## 24. Security Admin API (Phase 6E)

### 24.1 Get Security Statistics
**Endpoint**: `GET /api/v1/admin/security/stats`  
**Description**: Fetches aggregated security metrics including bans, failing logins, and roles.  
**Roles Required**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`

**Response (`200 OK`)**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Security stats retrieved successfully",
  "data": {
    "activeBans": 15,
    "twoFactorEnabledUsers": 0,
    "recentFailedLogins": 45,
    "userRoles": {
      "USER": 1000,
      "ADMIN": 5,
      "SUPER_ADMIN": 2
    }
  }
}
```

---

## 25. Data Refresh Strategy

| Data Category   | Refresh Method        | Frequency                    |
|-----------------|-----------------------|------------------------------|
| KPI Cards       | API call with caching | Real-time with 30s cache     |
| Charts & Trends | Scheduled aggregation | Every 5 minutes              |
| User Lists      | Paginated API         | On demand                    |
| System Health   | Prometheus + Actuator | Every 15 seconds             |
| Audit Log       | Database query        | On demand                    |
| Reports         | Background job        | On demand (async generation) |

> [!TIP]
> For expensive aggregate queries (e.g., total revenue, user counts by plan), implement a `DashboardStatsService` that pre-computes and caches these values in Redis with a 30-second TTL. This avoids hitting the database on every dashboard page load.

> [!IMPORTANT]
> All new admin endpoints must:
> 1. Require `ADMIN` or `SUPER_ADMIN` role via `@PreAuthorize`
> 2. Log the action via the audit log system
> 3. Sanitize all logged parameters via `SanitizeLogging`
> 4. Return the standard `UnravelDocsResponse<T>` wrapper
> 5. Support pagination via `page` and `size` query parameters for list endpoints
