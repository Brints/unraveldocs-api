# Dashboard Metrics Package — API Documentation

> **Base URL:** `/api/v1/admin/dashboard`  
> **Package:** `com.extractor.unraveldocs.metrics.dashboard`  
> **Last Updated:** 2026-03-24

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Data Models](#data-models)
   - [Response DTOs](#response-dtos)
4. [Endpoints](#endpoints)
   - [Get Dashboard KPI Stats](#1-get-dashboard-kpi-stats)
5. [Service Layer](#service-layer)
   - [DashboardMetricsService](#dashboardmetricsservice)
   - [DashboardMetricsServiceImpl](#dashboardmetricsserviceimpl)

---

## Overview

The **Dashboard Metrics** package provides aggregate statistics and Key Performance Indicators (KPIs) necessary for the admin dashboard. This module is restricted to high-level administrators and aggregates data across multiple modules including Users, Subscriptions, Login Attempts, and Storage.

| Feature        | Description                                                                                                             |
|----------------|-------------------------------------------------------------------------------------------------------------------------|
| KPI Statistics | Fetch aggregated counts for users (active, verified, new), subscriptions by status and plan, and overall storage usage. |

---

## Package Structure

```
metrics/dashboard/
├── api_docs/
│   └── api_docs.md                               # This file
├── controller/
│   └── DashboardMetricsController.java           # REST controller for dashboard endpoints
├── dto/
│   └── DashboardKpiStatsDto.java                 # Aggregated KPI response payload
└── service/
    ├── DashboardMetricsService.java              # Service interface
    └── impl/
        └── DashboardMetricsServiceImpl.java      # Service implementation collecting cross-module metrics
```

---

## Data Models

### Response DTOs

#### `DashboardKpiStatsDto`
**Package:** `com.extractor.unraveldocs.metrics.dashboard.dto`  
Returned inside `UnravelDocsResponse<DashboardKpiStatsDto>` when fetching stats.

| Section               | Field                    | Type                | Description                                                |
|-----------------------|--------------------------|---------------------|------------------------------------------------------------|
| **User KPIs**         | `totalUsers`             | `long`              | Count of all non-deleted users                             |
|                       | `activeUsers`            | `long`              | Users where `isActive = true AND isVerified = true`        |
|                       | `newUsersToday`          | `long`              | Users registered today                                     |
|                       | `newUsersThisWeek`       | `long`              | Users registered in the last 7 days                        |
|                       | `newUsersThisMonth`      | `long`              | Users registered in the last 30 days                       |
|                       | `verifiedUsers`          | `long`              | Count of `isVerified = true`                               |
|                       | `unverifiedUsers`        | `long`              | Count of `isVerified = false`                              |
|                       | `blockedUsers`           | `long`              | Count of users currently blocked due to login attempts     |
|                       | `dailyActiveUsers`       | `long`              | Users with `lastLogin` today                               |
|                       | `weeklyActiveUsers`      | `long`              | Users with `lastLogin` in the last 7 days                  |
|                       | `monthlyActiveUsers`     | `long`              | Users with `lastLogin` in the last 30 days                 |
| **Subscription KPIs** | `usersByPlan`            | `Map<String, Long>` | Breakdown of subscription counts by plan name              |
|                       | `usersByStatus`          | `Map<String, Long>` | Breakdown of subscriptions by status (ACTIVE, TRIAL, etc.) |
|                       | `activeSubscriptions`    | `long`              | Count of active subscriptions                              |
|                       | `trialSubscriptions`     | `long`              | Count of trial subscriptions                               |
|                       | `cancelledSubscriptions` | `long`              | Count of cancelled subscriptions                           |
|                       | `expiredSubscriptions`   | `long`              | Count of expired subscriptions                             |
| **Storage KPIs**      | `totalStorageUsed`       | `long`              | Sum of storage used across all subscriptions (in bytes)    |
|                       | `averageStorageUsed`     | `double`            | Average storage used per user (in bytes)                   |

---

## Endpoints

All endpoints are prefixed with `/api/v1/admin/dashboard`. Role-based access control requires `ADMIN`, `MODERATOR`, or `SUPER_ADMIN` privileges.

---

### 1. Get Dashboard KPI Stats

| Property          | Value                                 |
|-------------------|---------------------------------------|
| **Method**        | `GET`                                 |
| **Path**          | `/api/v1/admin/dashboard/stats`       |
| **Auth Required** | Yes (`ADMIN, MODERATOR, SUPER_ADMIN`) |

**Success Response — `200 OK`**

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Successfully fetched dashboard KPI stats",
  "data": {
    "totalUsers": 1500,
    "activeUsers": 1200,
    "newUsersToday": 10,
    "newUsersThisWeek": 85,
    "newUsersThisMonth": 350,
    "verifiedUsers": 1300,
    "unverifiedUsers": 200,
    "blockedUsers": 5,
    "dailyActiveUsers": 400,
    "weeklyActiveUsers": 900,
    "monthlyActiveUsers": 1350,
    "usersByPlan": {
      "FREE": 800,
      "PRO_MONTHLY": 400,
      "BUSINESS_YEARLY": 300
    },
    "usersByStatus": {
      "ACTIVE": 1400,
      "TRIAL": 50,
      "CANCELLED": 20,
      "EXPIRED": 30
    },
    "activeSubscriptions": 1400,
    "trialSubscriptions": 50,
    "cancelledSubscriptions": 20,
    "expiredSubscriptions": 30,
    "totalStorageUsed": 5000000000,
    "averageStorageUsed": 3333333.33
  }
}
```

> **Caching:** The result is cached in `dashboardKpiStats` to prevent repeated heavy aggregations on the database. 

**Error Responses**

| Status          | Condition                                         |
|-----------------|---------------------------------------------------|
| `403 Forbidden` | Access token missing, invalid, or inadequate role |

---

## Service Layer

### `DashboardMetricsService`
**Package:** `com.extractor.unraveldocs.metrics.dashboard.service`

Contract for fetching dashboard metrics. 

### `DashboardMetricsServiceImpl`
**Package:** `com.extractor.unraveldocs.metrics.dashboard.service.impl`  
**Implements:** `DashboardMetricsService`

**Logic:**
```
1. Compute relative Date boundaries (start of today, week, month in UTC).
2. Query UserRepository for aggregate user counts and time-based metrics.
3. Query LoginAttemptsRepository for blocked status counts.
4. Query UserSubscriptionRepository for active/trial status counts and plan grouping.
5. Query UserSubscriptionRepository for total summed storage usage.
6. Assemble collected metrics into DashboardKpiStatsDto.
7. Wrap and return in UnravelDocsResponse (HTTP 200).
8. Results are annotated with @Cacheable to avoid recalculation under heavy API load.
```
