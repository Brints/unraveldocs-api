# 📊 UnravelDocs Statistics Curation

A comprehensive list of all trackable statistics for the UnravelDocs platform, organized by category.

---

## Table of Contents

- [Tools & Technologies Needed](#-tools--technologies-needed)
- [Database Views vs Other Approaches](#-database-views-vs-other-approaches)
- [Core Statistics](#-core-statistics-you-mentioned)
- [User Statistics](#-user-statistics)
- [Document Statistics](#-document-statistics)
- [OCR Statistics](#-ocr-statistics)
- [Subscription Statistics](#-subscription-statistics)
- [Team Statistics](#-team-statistics)
- [Organization Statistics](#-organization-statistics)
- [Payment Statistics](#-payment-statistics)
- [Receipt Statistics](#-receipt-statistics)
- [Authentication & Security Statistics](#-authentication--security-statistics)
- [System Statistics](#-system-statistics)
- [Geographic & Demographic Stats](#-geographic--demographic-stats)
- [Business Intelligence Stats](#-business-intelligence-stats)
- [Notification Stats](#-notification-stats)
- [Recommended Dashboard Views](#-recommended-dashboard-views)
- [Implementation Notes](#-implementation-notes)

---

## 🛠 Tools & Technologies Needed

### Recommended Tool Stack

| Category | Tool | Purpose | Already in Use? |
|----------|------|---------|-----------------|
| **Metrics Collection** | Micrometer | Application metrics (counters, gauges, timers) | ✅ Yes (in pom.xml) |
| **Time-Series DB** | Prometheus | Store and query metrics | ❌ Recommended |
| **Visualization** | Grafana | Dashboards and alerting | ❌ Recommended |
| **Log Analytics** | Elasticsearch + Kibana | Full-text search, log analysis | ✅ Yes |
| **APM (Optional)** | Datadog / New Relic | Full-stack observability | ❌ Optional |
| **Database** | PostgreSQL | Primary data store | ✅ Yes |
| **Caching** | Redis | Real-time counters, caching | ✅ Yes |
| **Message Broker** | RabbitMQ / Kafka | Event-driven stats updates | ✅ Yes |
| **Structured Logging** | Logstash Logback | JSON logs for analysis | ✅ Yes |

---

### Detailed Tool Breakdown

#### 1. **Micrometer + Prometheus + Grafana** (Recommended Core Stack)

```
┌─────────────────┐     ┌─────────────┐     ┌──────────────┐
│  Spring Boot    │────▶│  Prometheus │────▶│   Grafana    │
│  + Micrometer   │     │  (scrapes)  │     │ (visualize)  │
└─────────────────┘     └─────────────┘     └──────────────┘
```

**What it provides:**
- Real-time system metrics (JVM, HTTP, DB connections)
- Custom business metrics (user signups, uploads, payments)
- Alerting on thresholds
- Historical data retention
- **Cost: FREE** (self-hosted)

**Spring Boot Integration:**
```xml
<!-- Already should be in pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

#### 2. **Elasticsearch + Kibana** (Already in Use)

**What it provides:**
- Log aggregation and search
- Document/User/Payment search indexes
- Visualization dashboards
- Anomaly detection (with X-Pack)

> [!TIP]
> You already have Elasticsearch configured. Kibana can create dashboards from your existing search indexes (`UserSearchIndex`, `DocumentSearchIndex`, `PaymentSearchIndex`).

---

#### 3. **Redis** (Already in Use)

**What it provides:**
- Real-time counters (e.g., daily active users, uploads today)
- Rate limiting
- Session storage
- Cached aggregations

**Example use cases:**
- `INCR stats:uploads:2026-01-03` - Daily upload counter
- `PFADD dau:2026-01-03 userId` - HyperLogLog for unique users

---

#### 4. **PostgreSQL Database Views & Materialized Views**

**What it provides:**
- Pre-computed aggregations
- Complex queries simplified
- Scheduled refreshes for "snapshot" stats

---

### Do You Need Datadog?

| Factor | Without Datadog | With Datadog |
|--------|-----------------|--------------|
| **Cost** | Free (Prometheus/Grafana) | $15-31/host/month |
| **Setup Complexity** | Medium (self-hosted) | Low (SaaS) |
| **APM (traces)** | Requires Jaeger/Zipkin | Built-in |
| **Log Management** | Elasticsearch/Kibana | Built-in |
| **Infrastructure Monitoring** | Prometheus node_exporter | Built-in |
| **Alerting** | Grafana Alerting | Built-in |
| **Maintenance** | You manage it | Managed SaaS |

> [!IMPORTANT]
> **Recommendation**: Datadog is **NOT required** for your use case. The Micrometer + Prometheus + Grafana stack provides everything you need for free. However, Datadog is worth considering if:
> - You want unified APM, logs, and metrics in one place
> - You prefer managed SaaS over self-hosting
> - You need advanced features like Real User Monitoring (RUM)
> - Budget allows ($15-31/host/month + data ingestion fees)

---

## 📊 Database Views vs Other Approaches

### Performance Comparison

| Approach | Use Case | Performance | Freshness | Complexity |
|----------|----------|-------------|-----------|------------|
| **Raw Queries** | Ad-hoc analysis | ❌ Slow for large tables | ✅ Real-time | Low |
| **Database Views** | Simplified queries | ❌ Same as raw (computed on read) | ✅ Real-time | Low |
| **Materialized Views** | Heavy aggregations | ✅ Fast (pre-computed) | ⚠️ Stale (until refresh) | Medium |
| **Stats Tables** | Historical snapshots | ✅ Fast | ⚠️ Stale (until job runs) | Medium |
| **Redis Counters** | Real-time counts | ✅✅ Fastest | ✅ Real-time | Medium |
| **Elasticsearch** | Search + aggregations | ✅ Fast for text search | ⚠️ Near real-time | High |

---

### When to Use Each Approach

#### Regular Views (CREATE VIEW)
```sql
-- Good for: Simplifying complex joins, not for performance
CREATE VIEW user_subscription_summary AS
SELECT 
    u.id, u.email, sp.name as plan_name, us.status
FROM users u
JOIN user_subscriptions us ON u.id = us.user_id
JOIN subscription_plans sp ON us.plan_id = sp.id;
```
> ⚠️ **Views are NOT more performant** - they execute the query every time. They're just syntactic sugar.

---

#### Materialized Views (Recommended for Heavy Stats)
```sql
-- Good for: Pre-computed aggregations, refreshed periodically
CREATE MATERIALIZED VIEW daily_stats AS
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_users,
    COUNT(*) FILTER (WHERE is_active) as active_users,
    COUNT(*) FILTER (WHERE NOT is_verified) as unverified_users
FROM users
GROUP BY DATE(created_at);

-- Refresh manually or via scheduled job
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_stats;
```
> ✅ **Materialized Views ARE more performant** - data is pre-computed and stored.

---

#### Stats Tables with Scheduled Jobs (Most Flexible)
```sql
-- Stats aggregation table
CREATE TABLE daily_statistics (
    id UUID PRIMARY KEY,
    stat_date DATE NOT NULL,
    stat_type VARCHAR(100) NOT NULL,
    stat_value BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(stat_date, stat_type)
);

-- Indexes for fast lookups
CREATE INDEX idx_daily_stats_date ON daily_statistics(stat_date);
CREATE INDEX idx_daily_stats_type ON daily_statistics(stat_type);
```

```java
// Scheduled job to compute stats daily
@Scheduled(cron = "0 0 1 * * *") // 1 AM daily
public void computeDailyStats() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    
    // Compute and store various stats
    long newUsers = userRepository.countByCreatedAtBetween(yesterday.atStartOfDay(), yesterday.plusDays(1).atStartOfDay());
    statsRepository.upsert(yesterday, "new_users", newUsers);
    
    long uploads = documentRepository.countByCreatedAtBetween(...);
    statsRepository.upsert(yesterday, "document_uploads", uploads);
    // ... more stats
}
```

---

#### Redis for Real-Time Counters
```java
// Real-time counter using Redis
@Service
public class RealTimeStatsService {
    private final StringRedisTemplate redis;
    
    public void incrementDailyUploads() {
        String key = "stats:uploads:" + LocalDate.now();
        redis.opsForValue().increment(key);
        redis.expire(key, Duration.ofDays(30)); // Auto-cleanup
    }
    
    public long getDailyUploads(LocalDate date) {
        String value = redis.opsForValue().get("stats:uploads:" + date);
        return value != null ? Long.parseLong(value) : 0;
    }
    
    // HyperLogLog for unique counts (memory efficient)
    public void trackDailyActiveUser(String userId) {
        String key = "stats:dau:" + LocalDate.now();
        redis.opsForHyperLogLog().add(key, userId);
    }
    
    public long getDailyActiveUsers(LocalDate date) {
        return redis.opsForHyperLogLog().size("stats:dau:" + date);
    }
}
```

---

### Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         STATS ARCHITECTURE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │   Real-Time  │    │   Near-Time  │    │   Historical     │   │
│  │              │    │              │    │                  │   │
│  │  • Redis     │    │ • Materialized│   │ • Stats Tables   │   │
│  │    Counters  │    │   Views      │    │ • Daily Jobs     │   │
│  │  • HyperLog  │    │ • Refresh    │    │ • Monthly Rollups│   │
│  │              │    │   every 5min │    │                  │   │
│  └──────────────┘    └──────────────┘    └──────────────────┘   │
│         │                   │                     │              │
│         └───────────────────┴─────────────────────┘              │
│                             │                                    │
│                    ┌────────▼────────┐                          │
│                    │    Grafana /    │                          │
│                    │    Kibana       │                          │
│                    │   Dashboards    │                          │
│                    └─────────────────┘                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔑 Core Statistics You Mentioned

| Statistic | Description | Data Source |
|-----------|-------------|-------------|
| Total Users | Total registered users | `users` table |
| Unactivated Users | Users who haven't verified/activated | `users` where `is_active = false` OR `is_verified = false` |
| Total Subscriptions | All active subscriptions | `user_subscriptions` table |
| Subscriptions by Category | Count by plan (Free, Starter, Pro, Business) | `user_subscriptions` grouped by `plan_id` |
| Total Users in a Month | New user registrations per month | `users` by `created_at` |
| Total Documents Uploaded | All documents uploaded | `document_collections` table |
| Failed Uploads | Documents with failed upload status | `document_file_entries` where `upload_status = 'FAILED'` |
| Documents Uploaded in a Month | Monthly document uploads | `document_collections` by `created_at` |
| Total OCR Pages | All OCR pages processed | `ocr_data` table count |
| Total Failed OCR | Failed OCR operations | `ocr_data` where `status = 'FAILED'` |
| Team Subscriptions | Number of team subscriptions | `teams` table |

---

## 👥 User Statistics

### Account Stats
| Statistic | Description |
|-----------|-------------|
| Total registered users | All users in the system |
| Active users | Users with `is_active = true` AND `is_verified = true` |
| Inactive users | Users with `is_active = false` |
| Unverified users | Users with `is_verified = false` |
| Deleted users (soft) | Users with `deleted_at` not null |
| Platform admins | Users with `is_platform_admin = true` |
| Organization admins | Users with `is_organization_admin = true` |
| Users by role | Breakdown by USER, ADMIN, etc. |
| Users by country | Geographic distribution |
| Users by profession | Professional breakdown |
| Users who opted in for marketing | `marketing_opt_in = true` |

### User Growth & Engagement
| Statistic | Description |
|-----------|-------------|
| New users per day/week/month/year | Time-series user registrations |
| Daily active users (DAU) | Users who logged in today |
| Weekly active users (WAU) | Users who logged in this week |
| Monthly active users (MAU) | Users who logged in this month |
| User retention rate | % of users returning after X days |
| Churn rate | % of users who stopped using the platform |
| Average session duration | Average time per login session |
| Last login distribution | Breakdown of last login times |
| Users never logged in | Users with `last_login = null` |
| Login frequency | Average logins per user |

### User Registration Funnel
| Statistic | Description |
|-----------|-------------|
| Sign-up to verification rate | % completing email verification |
| Verification to first login rate | % who log in after verification |
| First login to first document rate | % who upload after first login |
| Onboarding completion rate | % completing onboarding steps |

---

## 📄 Document Statistics

### Upload Stats
| Statistic | Description |
|-----------|-------------|
| Total document collections | All document collections |
| Total individual files | All files across all collections |
| Documents by status | UPLOADED, COMPLETED, PARTIALLY_COMPLETED, FAILED_UPLOAD, PROCESSING, PROCESSED, FAILED_OCR, DELETED |
| Documents by file type | PDF, PNG, JPG, TIFF, etc. |
| Total storage used | Sum of all `file_size` values |
| Average file size | Average `file_size` |
| Largest file uploaded | Maximum `file_size` |
| Documents per user (average) | Total docs / total users |
| Documents per user (distribution) | Distribution of doc counts |
| Empty collections | Collections with no files |

### Time-Based Document Stats
| Statistic | Description |
|-----------|-------------|
| Documents uploaded per day/week/month/year | Time-series uploads |
| Peak upload hours | Busiest hours for uploads |
| Peak upload days | Busiest days for uploads |
| Upload trends | Growth/decline over time |
| Documents deleted per period | Soft-deleted documents |

### Document Processing Stats
| Statistic | Description |
|-----------|-------------|
| Average processing time | Time from upload to processing complete |
| Documents pending processing | Documents awaiting processing |
| Documents in processing | Currently being processed |
| Processing success rate | % of documents successfully processed |
| Processing failure rate | % of documents failed processing |

---

## 🔍 OCR Statistics

### Processing Volume
| Statistic | Description |
|-----------|-------------|
| Total OCR operations | All OCR processes initiated |
| OCR by status | PENDING, PROCESSING, COMPLETED, FAILED |
| OCR success rate | % completed successfully |
| OCR failure rate | % failed |
| Average OCR processing time | Time from initiation to completion |
| OCR queue length | Currently pending |
| OCR retries | Number of retry attempts |

### OCR Quality & Performance
| Statistic | Description |
|-----------|-------------|
| OCR provider usage | Tesseract vs Google Vision breakdown |
| Average extracted text length | Average characters extracted |
| Empty OCR results | Documents with no text extracted |
| OCR error types | Categorized error messages |
| Pages processed per hour/day | Throughput metrics |

### OCR by Plan
| Statistic | Description |
|-----------|-------------|
| OCR pages by subscription tier | Usage per Free, Starter, Pro, Business |
| OCR quota utilization | % of quota used per plan |
| Users at OCR limit | Users who hit their limit |
| OCR overages | Attempts beyond quota |

---

## 💳 Subscription Statistics

### Individual Subscriptions
| Statistic | Description |
|-----------|-------------|
| Total subscriptions | All user subscriptions |
| Subscriptions by plan | Free, Starter (Monthly/Yearly), Pro (Monthly/Yearly), Business (Monthly/Yearly) |
| Subscriptions by billing interval | Monthly vs Yearly |
| Subscriptions by status | Active, Cancelled, Trial, Expired |
| Active subscriptions | Currently active |
| Trial subscriptions | Users in trial period |
| Subscriptions with auto-renew | `auto_renew = true` |
| Users who used trial | `has_used_trial = true` |

### Subscription Economics
| Statistic | Description |
|-----------|-------------|
| Monthly Recurring Revenue (MRR) | Sum of monthly subscription values |
| Annual Recurring Revenue (ARR) | MRR × 12 |
| Average Revenue Per User (ARPU) | Total revenue / active users |
| Customer Lifetime Value (CLV) | Average subscription duration × ARPU |
| Plan upgrade rate | % upgrading to higher tier |
| Plan downgrade rate | % downgrading to lower tier |
| Yearly vs monthly preference | Ratio of yearly to monthly |

### Subscription Lifecycle
| Statistic | Description |
|-----------|-------------|
| New subscriptions per period | Time-series new subscriptions |
| Subscription cancellations per period | Time-series cancellations |
| Subscription renewals per period | Time-series renewals |
| Subscription expiring soon | Subscriptions ending within 7 days |
| Average subscription duration | Mean time before cancellation |
| Trial to paid conversion rate | % converting from trial |

---

## 👥 Team Statistics

### Team Overview
| Statistic | Description |
|-----------|-------------|
| Total teams | All teams created |
| Active teams | Teams with `is_active = true` |
| Verified teams | Teams with `is_verified = true` |
| Closed teams | Teams with `is_closed = true` |
| Teams by subscription type | TEAM_PREMIUM vs TEAM_ENTERPRISE |
| Teams by subscription status | TRIAL, ACTIVE, CANCELLED, EXPIRED, PAST_DUE |
| Teams by billing cycle | MONTHLY vs YEARLY |
| Teams with auto-renew enabled | `auto_renew = true` |

### Team Membership
| Statistic | Description |
|-----------|-------------|
| Total team members | All members across all teams |
| Average members per team | Mean membership count |
| Members by role | OWNER, ADMIN, MEMBER breakdown |
| Teams at max capacity | Teams with `members.count = max_members` |
| Teams below 50% capacity | Teams significantly underutilized |
| Solo teams | Teams with only the owner |

### Team Trial & Subscription
| Statistic | Description |
|-----------|-------------|
| Teams in trial | `subscription_status = TRIAL` |
| Teams with trial reminder sent | `trial_reminder_sent = true` |
| Trial expiring within 3 days | Teams needing warning |
| Teams that used trial | `has_used_trial = true` |
| Trial to paid conversion rate | % of trials converting |

### Team Invitations
| Statistic | Description |
|-----------|-------------|
| Total invitations sent | All `TeamInvitation` records |
| Pending invitations | Invitations awaiting response |
| Accepted invitations | Successfully joined members |
| Declined/expired invitations | Failed invitation attempts |
| Invitation acceptance rate | % of invitations accepted |
| Average time to accept | Mean time from sent to accepted |

### Team Documents
| Statistic | Description |
|-----------|-------------|
| Team document uploads | Total across all teams |
| Average documents per team | Mean doc count per team |
| Teams at document limit | Teams at `monthly_document_limit` |
| Team document utilization | % of limit used |

---

## 🏢 Organization Statistics

### Organization Overview
| Statistic | Description |
|-----------|-------------|
| Total organizations | All organizations created |
| Active organizations | `is_active = true` |
| Verified organizations | `is_verified = true` |
| Closed organizations | `is_closed = true` |
| Organizations by type | PREMIUM vs ENTERPRISE |
| Organizations in trial | Currently in trial period |

### Organization Membership
| Statistic | Description |
|-----------|-------------|
| Total organization members | All members across organizations |
| Average members per org | Mean membership count |
| Members by role | OWNER, ADMIN, MEMBER breakdown |
| Organizations at capacity | At `max_members` limit |
| Organization invitation stats | Sent, pending, accepted, declined |

### Organization Documents
| Statistic | Description |
|-----------|-------------|
| Org document uploads | Monthly upload counts |
| Orgs at document limit | At `monthly_document_limit` |
| Document utilization by org | % of limit used |

---

## 💰 Payment Statistics

### Payment Volume
| Statistic | Description |
|-----------|-------------|
| Total payments | All payment transactions |
| Total revenue | Sum of successful payment amounts |
| Payments by status | PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELED, REFUNDED, etc. |
| Payments by type | ONE_TIME, SUBSCRIPTION |
| Payments by currency | USD, EUR, NGN, etc. distribution |
| Average payment amount | Mean transaction value |
| Largest payment | Maximum transaction value |

### Payment Gateway Stats
| Statistic | Description |
|-----------|-------------|
| Payments by gateway | Stripe vs Paystack vs others |
| Gateway success rate | Per-gateway success rates |
| Gateway failure rate | Per-gateway failure rates |
| Gateway preference by region | Which gateway used where |

### Payment Issues
| Statistic | Description |
|-----------|-------------|
| Failed payments | Payments with `status = FAILED` |
| Payment failure reasons | Categorized `failure_message` |
| Refunds | Total refunded amount |
| Partial refunds | Partially refunded transactions |
| Disputed transactions | Transactions in dispute |
| Chargebacks | Chargeback count and amount |

### Revenue Metrics
| Statistic | Description |
|-----------|-------------|
| Daily/Weekly/Monthly revenue | Time-series revenue |
| Revenue by plan | Revenue contribution by plan type |
| Revenue by billing cycle | Monthly vs Yearly revenue |
| Revenue by payment method | Card, bank, etc. |
| Year-over-year growth | Revenue growth comparison |

---

## 🧾 Receipt Statistics

| Statistic | Description |
|-----------|-------------|
| Total receipts generated | All `Receipt` records |
| Receipts by provider | Stripe, Paystack breakdown |
| Receipts emailed | `email_sent = true` |
| Receipts pending email | `email_sent = false` |
| Average receipt amount | Mean receipt value |
| Receipts per day/month | Time-series generation |

---

## 🔐 Authentication & Security Statistics

### Login Stats
| Statistic | Description |
|-----------|-------------|
| Total login attempts | All login attempts |
| Successful logins | Successful authentications |
| Failed logins | Failed login attempts |
| Login success rate | % of attempts successful |
| Unique users logging in per day | DAU metric |

### Login Attempts & Blocking
| Statistic | Description |
|-----------|-------------|
| Users currently blocked | `is_blocked = true` |
| Users blocked in last 24h | Recently blocked accounts |
| Average failed attempts before block | Mean failures before lockout |
| Blocked users by reason | Categorized block reasons |
| Unblock rate | % of blocked accounts recovered |

### Token & Session Stats
| Statistic | Description |
|-----------|-------------|
| Active sessions | Currently active JWT tokens |
| Token refresh rate | How often tokens are refreshed |
| Session duration distribution | How long sessions last |
| Concurrent sessions per user | Multiple device logins |

### OAuth Statistics
| Statistic | Description |
|-----------|-------------|
| OAuth logins | Social login count |
| OAuth by provider | Google, GitHub breakdown |
| OAuth vs password ratio | Social vs traditional login |

### Email Verification
| Statistic | Description |
|-----------|-------------|
| Verification emails sent | Total sent |
| Verification completion rate | % who complete verification |
| Average time to verify | Mean time from signup to verify |
| Expired verification tokens | Tokens that timed out |
| Resent verifications | Re-requested verification emails |

### Password Reset
| Statistic | Description |
|-----------|-------------|
| Password reset requests | Total reset requests |
| Password reset completions | Successfully reset |
| Reset completion rate | % completing reset |
| Average reset time | Time from request to completion |

---

## 📡 System Statistics

### Server Performance
| Statistic | Description |
|-----------|-------------|
| Request count per second/minute | API throughput |
| Response time (avg, p50, p95, p99) | Latency distribution |
| Error rate | 4xx and 5xx response rate |
| Request count by endpoint | Top API endpoints |
| Active connections | Current open connections |
| Thread pool utilization | Thread usage |

### API Usage
| Statistic | Description |
|-----------|-------------|
| Total API calls | All requests |
| API calls by endpoint | Per-endpoint breakdown |
| API calls by user | Per-user API usage |
| API errors by type | Error categorization |
| Rate limit hits | Users hitting rate limits |
| API response time by endpoint | Per-endpoint latency |

### Database Metrics
| Statistic | Description |
|-----------|-------------|
| Database connections (active/idle) | Connection pool stats |
| Query execution time (avg, p95) | Query performance |
| Slow queries | Queries exceeding threshold |
| Database size | Total storage used |
| Table sizes | Per-table storage |
| Row counts per table | Data volume |
| Index usage | Index hit rates |

### Redis/Cache Stats
| Statistic | Description |
|-----------|-------------|
| Cache hit rate | % of requests served from cache |
| Cache miss rate | % requiring database lookup |
| Cache memory usage | Redis memory consumption |
| Cache key count | Number of cached items |
| Cache eviction rate | Items evicted |
| Average cache TTL | Mean time to live |

### Elasticsearch Stats
| Statistic | Description |
|-----------|-------------|
| Index sizes | Per-index storage |
| Document counts | Documents indexed |
| Search query count | Search requests |
| Search latency | Query response time |
| Indexing rate | Documents indexed per second |
| Index health | Cluster health status |

### Message Broker Stats (RabbitMQ/Kafka)
| Statistic | Description |
|-----------|-------------|
| Messages published | Total messages sent |
| Messages consumed | Total messages processed |
| Queue lengths | Per-queue message count |
| Consumer lag | Kafka consumer lag |
| Message processing time | Time to process messages |
| Dead letter messages | Failed message count |
| Message retry rate | Reprocessing frequency |

### Storage Stats
| Statistic | Description |
|-----------|-------------|
| S3 storage used | Total S3 storage |
| S3 objects count | Number of objects |
| S3 upload/download bandwidth | Transfer rates |
| Cloudinary storage used | Image storage |
| Cloudinary bandwidth | CDN bandwidth |

### Email Service Stats
| Statistic | Description |
|-----------|-------------|
| Emails sent | Total emails sent |
| Emails by type | Verification, welcome, receipt, etc. |
| Email delivery rate | % successfully delivered |
| Email bounce rate | % bounced |
| Email open rate | % opened (if tracked) |
| Email click rate | % clicked links (if tracked) |

### SMS Stats
| Statistic | Description |
|-----------|-------------|
| SMS sent | Total SMS messages |
| SMS delivery rate | % successfully delivered |
| SMS by type | OTP, notifications, etc. |
| SMS cost | Total SMS spending |

### Error & Exception Stats
| Statistic | Description |
|-----------|-------------|
| Total exceptions | Exception count |
| Exceptions by type | Categorized exceptions |
| Exceptions by endpoint | Where errors occur |
| Exception trends | Error rate over time |
| Unhandled exceptions | Critical errors |

### Background Job Stats
| Statistic | Description |
|-----------|-------------|
| Scheduled jobs | All scheduled tasks |
| Job execution count | How often jobs run |
| Job success/failure rate | Job reliability |
| Average job duration | Processing time |
| Failed job retries | Retry attempts |

---

## 🌍 Geographic & Demographic Stats

| Statistic | Description |
|-----------|-------------|
| Users by country | Geographic distribution |
| Revenue by country | Revenue per region |
| Subscriptions by country | Plan popularity by region |
| Preferred currency by region | Currency preferences |
| Language preferences | If i18n is tracked |
| Timezone distribution | User timezone spread |

---

## 📈 Business Intelligence Stats

### Conversion Funnel
| Statistic | Description |
|-----------|-------------|
| Visitor to signup rate | Website conversion |
| Signup to verified rate | Activation rate |
| Verified to first upload rate | Engagement rate |
| Free to paid conversion rate | Monetization rate |
| Trial to paid conversion rate | Trial success |

### Cohort Analysis
| Statistic | Description |
|-----------|-------------|
| Retention by signup cohort | User retention curves |
| Revenue by signup cohort | Cohort value |
| Feature adoption by cohort | Feature usage patterns |

### Feature Usage
| Statistic | Description |
|-----------|-------------|
| OCR feature usage | % using OCR |
| Word export usage | % using export |
| Team feature adoption | % using teams |
| Search usage | Search feature usage |
| Mobile vs desktop usage | Device breakdown |

---

## 🔔 Notification Stats

| Statistic | Description |
|-----------|-------------|
| Push notifications sent | Total push notifications |
| Notification types | By notification category |
| Notification click-through rate | Engagement with notifications |
| Users with notifications enabled | Opt-in rate |

---

## 📊 Recommended Dashboard Views

### Executive Dashboard
- Total users, MRR, ARR, user growth
- Conversion rates, churn rate
- Revenue trends, top plans

### Operations Dashboard
- System health, error rates
- Queue lengths, processing times
- Database/cache performance

### User Analytics Dashboard
- DAU/WAU/MAU trends
- User segments, engagement
- Geographic distribution

### Financial Dashboard
- Revenue breakdown, payment success
- Refunds, chargebacks
- Revenue by plan/region

### Document Processing Dashboard
- Upload volume, processing status
- OCR success rates, queue length
- Storage utilization

---

## 💡 Implementation Notes

> [!TIP]
> **Recommended Implementation Priority:**
> 1. **Phase 1**: Materialized views for core business stats
> 2. **Phase 2**: Redis counters for real-time metrics
> 3. **Phase 3**: Micrometer metrics for system observability
> 4. **Phase 4**: Grafana/Kibana dashboards

> [!IMPORTANT]
> **Key Decisions:**
> - Use **Materialized Views** for stats that don't need real-time freshness (daily stats)
> - Use **Redis counters** for real-time counts (current uploads, active users)
> - Use **Stats tables with scheduled jobs** for historical data and complex aggregations
> - Use **Micrometer + Prometheus + Grafana** for system metrics (NOT Datadog - it's overkill for your needs)

> [!NOTE]
> For time-series statistics, consider:
> - Storing aggregated stats in separate tables
> - Using scheduled jobs to compute daily/weekly/monthly rollups
> - Implementing data retention policies for raw data

---

## 📝 Sample Materialized Views

```sql
-- User Stats Materialized View
CREATE MATERIALIZED VIEW mv_user_stats AS
SELECT 
    COUNT(*) as total_users,
    COUNT(*) FILTER (WHERE is_active AND is_verified) as active_users,
    COUNT(*) FILTER (WHERE NOT is_active) as inactive_users,
    COUNT(*) FILTER (WHERE NOT is_verified) as unverified_users,
    COUNT(*) FILTER (WHERE is_platform_admin) as platform_admins,
    COUNT(*) FILTER (WHERE marketing_opt_in) as marketing_opted_in
FROM users
WHERE deleted_at IS NULL;

-- Subscription Stats Materialized View
CREATE MATERIALIZED VIEW mv_subscription_stats AS
SELECT 
    sp.name as plan_name,
    COUNT(*) as subscription_count,
    COUNT(*) FILTER (WHERE us.status = 'active') as active_count,
    SUM(sp.price) FILTER (WHERE us.status = 'active') as mrr
FROM user_subscriptions us
JOIN subscription_plans sp ON us.plan_id = sp.id
GROUP BY sp.name;

-- OCR Stats Materialized View
CREATE MATERIALIZED VIEW mv_ocr_stats AS
SELECT 
    status,
    COUNT(*) as count,
    DATE(created_at) as date
FROM ocr_data
GROUP BY status, DATE(created_at);

-- Refresh views (schedule this)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_user_stats;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_subscription_stats;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_ocr_stats;
```

---

*Last Updated: 2026-01-03*
