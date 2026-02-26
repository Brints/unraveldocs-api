# Elasticsearch Package — Documentation

> **Package:** `com.extractor.unraveldocs.elasticsearch`  
> **Base URLs:**
> - User Search: `/api/v1/search`
> - Admin Search: `/api/v1/admin/search`
> - Admin Sync: `/api/v1/admin/elasticsearch`  
> **Last Updated:** February 26, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Architecture](#architecture)
4. [Configuration](#configuration)
5. [Index Documents (Mappings)](#index-documents-mappings)
   - [DocumentSearchIndex](#documentsearchindex)
   - [UserSearchIndex](#usersearchindex)
   - [PaymentSearchIndex](#paymentsearchindex)
6. [Events & Enums](#events--enums)
   - [ElasticsearchIndexEvent](#elasticsearchindexevent)
   - [IndexAction](#indexaction)
   - [IndexType](#indextype)
7. [Publisher](#publisher)
   - [ElasticsearchEventPublisher](#elasticsearcheventpublisher)
8. [Consumer](#consumer)
   - [ElasticsearchIndexConsumer](#elasticsearchindexconsumer)
9. [Services](#services)
   - [ElasticsearchIndexingService](#elasticsearchindexingservice)
   - [ElasticsearchSyncService](#elasticsearchsyncservice)
   - [DocumentSearchService](#documentsearchservice)
   - [UserSearchService](#usersearchservice)
   - [PaymentSearchService](#paymentsearchservice)
10. [Repositories](#repositories)
    - [DocumentSearchRepository](#documentsearchrepository)
    - [UserSearchRepository](#usersearchrepository)
    - [PaymentSearchRepository](#paymentsearchrepository)
11. [DTOs](#dtos)
    - [SearchRequest](#searchrequest)
    - [SearchResponse](#searchresponse)
    - [DocumentSearchResult](#documentsearchresult)
12. [Endpoints](#endpoints)
    - [Document Search (User)](#document-search-user)
    - [Admin User Search](#admin-user-search)
    - [Admin Payment Search](#admin-payment-search)
    - [Admin Sync Endpoints](#admin-sync-endpoints)
13. [Elasticsearch Queries Reference](#elasticsearch-queries-reference)
14. [Data Mapping Reference](#data-mapping-reference)
15. [Configuration Reference](#configuration-reference)
16. [Flow Diagrams](#flow-diagrams)

---

## Overview

The **Elasticsearch** package provides the full-text search and indexing infrastructure for UnravelDocs. It spans three concerns:

| Concern | Description |
|---|---|
| **Indexing (Write Path)** | Domain services call `ElasticsearchIndexingService` which publishes `ElasticsearchIndexEvent`s to the `unraveldocs-elasticsearch` Kafka topic. The `ElasticsearchIndexConsumer` picks these up asynchronously and writes to the appropriate ES index. |
| **Searching (Read Path)** | Three search services (`DocumentSearchService`, `UserSearchService`, `PaymentSearchService`) delegate to Spring Data Elasticsearch repositories backed by custom `@Query` DSL queries. |
| **Bulk Sync** | `ElasticsearchSyncService` provides paginated batch re-indexing from PostgreSQL → Elasticsearch, triggered by admin REST endpoints. |

**Three Elasticsearch indexes:**

| Index Name | Document Type | Primary Use |
|---|---|---|
| `documents` | `DocumentSearchIndex` | User full-text search across OCR-extracted text and file metadata |
| `users` | `UserSearchIndex` | Admin user lookup and filtering |
| `payments` | `PaymentSearchIndex` | Admin payment oversight and receipt lookup |

> **Conditional Loading:** All beans carry `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`. If the property is absent, the entire Elasticsearch sub-system is silently disabled and the application boots without it.

---

## Package Structure

```
elasticsearch/
├── config/
│   └── ElasticsearchConfig.java              # Wires RestClient, ElasticsearchTransport, ElasticsearchClient, ElasticsearchOperations
├── consumer/
│   └── ElasticsearchIndexConsumer.java       # @KafkaListener — consumes unraveldocs-elasticsearch; routes to repository by IndexType + IndexAction
├── controller/
│   ├── AdminSearchController.java            # Admin: POST/GET /api/v1/admin/search/users|payments + receipt lookup
│   ├── DocumentSearchController.java         # User: POST/GET /api/v1/search/documents + content search
│   └── ElasticsearchSyncController.java      # Admin: POST /api/v1/admin/elasticsearch/sync/**
├── document/
│   ├── DocumentSearchIndex.java              # @Document(indexName = "documents") — file metadata + OCR extracted text
│   ├── PaymentSearchIndex.java               # @Document(indexName = "payments") — receipt + payment fields
│   └── UserSearchIndex.java                  # @Document(indexName = "users") — user profile + subscription snapshot
├── dto/
│   ├── DocumentSearchResult.java             # Rich search result with text preview + highlights list
│   ├── SearchRequest.java                    # Generic pageable search request with filters + date range
│   └── SearchResponse.java                   # Generic pageable search response with totalHits + facets
├── events/
│   ├── ElasticsearchIndexEvent.java          # Serializable event envelope: documentId + action + indexType + payload
│   ├── IndexAction.java                      # Enum: CREATE | UPDATE | DELETE
│   └── IndexType.java                        # Enum: DOCUMENT | USER | PAYMENT | SUBSCRIPTION
├── publisher/
│   └── ElasticsearchEventPublisher.java      # Wraps MessageBrokerFactory; publishes ElasticsearchIndexEvent to unraveldocs-elasticsearch
├── repository/
│   ├── DocumentSearchRepository.java         # ElasticsearchRepository<DocumentSearchIndex, String> + 5 custom @Query methods
│   ├── PaymentSearchRepository.java          # ElasticsearchRepository<PaymentSearchIndex, String> + 6 custom @Query methods
│   └── UserSearchRepository.java             # ElasticsearchRepository<UserSearchIndex, String> + 4 custom @Query methods
├── service/
│   ├── DocumentSearchService.java            # Full-text document search + async/sync index + delete operations
│   ├── ElasticsearchIndexingService.java     # Domain-facing facade: maps JPA entities → index docs + publishes events
│   ├── ElasticsearchSyncService.java         # Bulk @Async sync: reads PostgreSQL in 100-record pages → saves to ES repositories
│   ├── PaymentSearchService.java             # Payment search with provider/status/currency/date filters
│   └── UserSearchService.java                # User search with role/active/country filters
└── documentation/
    └── api_docs.md                           # This file
```

---

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   Domain Packages                          │
│  (user, documents, payment, auth …)                        │
│                                                            │
│  ──calls──► ElasticsearchIndexingService                   │
│               .indexUser(user, CREATE/UPDATE)              │
│               .indexDocument(collection, file, ocr, action)│
│               .indexPayment(receipt, CREATE)               │
│               .deleteUserFromIndex(userId)                 │
└────────────────────────┬───────────────────────────────────┘
                         │ maps JPA entity → index doc
                         │ serialises to JSON payload
                         ▼
              ElasticsearchEventPublisher
                  .publishUserIndexEvent()
                  .publishDocumentIndexEvent()
                  .publishPaymentIndexEvent()
                         │
                         │ Message.of(event, TOPIC_ELASTICSEARCH, documentId, headers)
                         ▼
           MessageBrokerFactory → KafkaMessageProducer
                         │
                         ▼
         Kafka Topic: unraveldocs-elasticsearch
          (6 partitions, 7-day retention, keyed by documentId)
                         │
                         ▼
         ElasticsearchIndexConsumer (@KafkaListener)
           group: elasticsearch-consumer-group
                         │
                  switch(IndexType)
                 /        |         \
         DOCUMENT        USER      PAYMENT
            │              │           │
            ▼              ▼           ▼
  DocumentSearchRepo  UserSearchRepo  PaymentSearchRepo
        .save() / .deleteById()
                         │
                         ▼
              Elasticsearch Cluster
            ┌──────────┬──────────┐
            │documents │  users   │payments│
            └──────────┴──────────┘
                         │
              ┌──────────┴──────────────────────────┐
              │                                     │
    DocumentSearchService            UserSearchService / PaymentSearchService
    .searchDocuments()               .searchUsers() / .searchPayments()
              │                                     │
    DocumentSearchController         AdminSearchController
    /api/v1/search/documents         /api/v1/admin/search/users|payments
```

---

## Configuration

### `ElasticsearchConfig`
**Package:** `com.extractor.unraveldocs.elasticsearch.config`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`  
**Enables:** `@EnableElasticsearchRepositories(basePackages = "com.extractor.unraveldocs.elasticsearch.repository")`

Manually constructs the Elasticsearch Java client stack (bypassing Spring Boot autoconfiguration) to allow full control over connection parameters.

**Bean chain:**

```
@Value("${spring.elasticsearch.uris:http://localhost:9200}")
        │
        ▼ extractHost / extractPort / extractScheme
RestClient (destroyMethod = "close")
        │
        ▼
RestClientTransport(restClient, JacksonJsonpMapper)
        │
        ▼
ElasticsearchClient(transport)
        │
        ▼
ElasticsearchTemplate(client)  ← implements ElasticsearchOperations
```

| Bean | Type | Description |
|---|---|---|
| `restClient` | `RestClient` | Low-level Apache HTTP client; destroyed gracefully on shutdown |
| `elasticsearchTransport` | `ElasticsearchTransport` | Transport layer with Jackson JSON mapping |
| `elasticsearchClient` | `ElasticsearchClient` | High-level ES Java API client |
| `elasticsearchTemplate` | `ElasticsearchOperations` | Spring Data template for programmatic operations |

**URI parsing:** Host, port, and scheme are extracted from the URI string. Defaults: host = *(from URI)*, port = `9200`, scheme = `"http"`. Throws `IllegalArgumentException` if the URI is malformed.

---

## Index Documents (Mappings)

### `DocumentSearchIndex`
**Index name:** `documents`  
**Settings:** `/elasticsearch/document-settings.json`  
**Package:** `com.extractor.unraveldocs.elasticsearch.document`

Combines `FileEntry` metadata with OCR-extracted text from `OcrData` into a single flat Elasticsearch document.

| Field | ES Type | Mapping Detail | Source |
|---|---|---|---|
| `id` | `@Id` | Document ID | `FileEntry.documentId` |
| `userId` | `Keyword` | Exact match filter | `DocumentCollection.user.id` |
| `collectionId` | `Keyword` | Exact match filter | `DocumentCollection.id` |
| `fileName` | `Text` (standard) + `Keyword` suffix | Full-text searchable; `.keyword` for exact/sort | `FileEntry.originalFileName` |
| `fileType` | `Keyword` | Filter by extension (pdf, docx, png…) | `FileEntry.fileType` |
| `fileSize` | `Long` | Size in bytes | `FileEntry.fileSize` |
| `status` | `Keyword` | Collection status (PENDING, COMPLETED…) | `DocumentCollection.collectionStatus.name()` |
| `ocrStatus` | `Keyword` | OCR processing status | `OcrData.status.name()` (null if not processed) |
| `extractedText` | `Text` (standard) | **Primary full-text search field** — boosted ×2 in queries | `OcrData.editedContent` or `OcrData.extractedText` |
| `fileUrl` | `Keyword` | Not indexed (`index = false`) — stored but not searchable | `FileEntry.fileUrl` |
| `uploadTimestamp` | `Date` | ISO 8601 optional time | `DocumentCollection.uploadTimestamp` |
| `createdAt` | `Date` | ISO 8601 optional time | `FileEntry.createdAt` |
| `updatedAt` | `Date` | ISO 8601 optional time | `FileEntry.updatedAt` |
| `tags` | `Keyword` (list) | Reserved for future categorisation; defaults to empty list | *(not yet populated)* |

> **Note:** `editedContent` takes precedence over `extractedText` when mapping, ensuring users search the most up-to-date version of OCR output.

---

### `UserSearchIndex`
**Index name:** `users`  
**Package:** `com.extractor.unraveldocs.elasticsearch.document`

Snapshot of a user's profile, role, subscription, and activity. Used exclusively by admin search.

| Field | ES Type | Mapping Detail | Source |
|---|---|---|---|
| `id` | `@Id` | User UUID | `User.id` |
| `firstName` | `Text` (standard) + `Keyword` | Boosted ×2 in search queries | `User.firstName` |
| `lastName` | `Text` (standard) + `Keyword` | Boosted ×2 in search queries | `User.lastName` |
| `email` | `Keyword` | Exact match + wildcard search | `User.email` |
| `role` | `Keyword` | Filter by role (USER, ADMIN…) | `User.role.name()` |
| `isActive` | `Boolean` | Active status filter | `User.isActive` |
| `isVerified` | `Boolean` | Verification status filter | `User.isVerified` |
| `isPlatformAdmin` | `Boolean` | Platform admin flag | `User.isPlatformAdmin` |
| `isOrganizationAdmin` | `Boolean` | Org admin flag | `User.isOrganizationAdmin` |
| `country` | `Keyword` | Country filter | `User.country` |
| `profession` | `Text` (standard) + `Keyword` | Searchable profession | `User.profession` |
| `organization` | `Text` (standard) + `Keyword` | Searchable organisation | `User.organization` |
| `profilePicture` | `Keyword` | Not indexed (`index = false`) | `User.profilePicture` |
| `subscriptionPlan` | `Keyword` | Current plan name | `User.subscription.plan.name.name()` |
| `subscriptionStatus` | `Keyword` | Subscription status | `User.subscription.status` |
| `lastLogin` | `Date` | Last login timestamp | `User.lastLogin` |
| `createdAt` | `Date` | Account creation | `User.createdAt` |
| `updatedAt` | `Date` | Last update | `User.updatedAt` |
| `documentCount` | `Integer` | Total documents uploaded | `User.documents.size()` |

---

### `PaymentSearchIndex`
**Index name:** `payments`  
**Package:** `com.extractor.unraveldocs.elasticsearch.document`

Snapshot of a `Receipt` entity enriched with user data. All receipts are indexed as `status = "COMPLETED"` since receipts are only created for successful payments.

| Field | ES Type | Mapping Detail | Source |
|---|---|---|---|
| `id` | `@Id` | Receipt UUID | `Receipt.id` |
| `userId` | `Keyword` | Filter by user | `Receipt.user.id` |
| `userEmail` | `Keyword` | Exact/wildcard search | `Receipt.user.email` |
| `userName` | `Text` (standard) + `Keyword` | Full name searchable | `user.firstName + " " + user.lastName` |
| `receiptNumber` | `Keyword` | Exact lookup | `Receipt.receiptNumber` |
| `paymentProvider` | `Keyword` | Filter: STRIPE, PAYSTACK, PAYPAL | `Receipt.paymentProvider.name()` |
| `externalPaymentId` | `Keyword` | Exact provider reference | `Receipt.externalPaymentId` |
| `paymentType` | `Keyword` | ONE_TIME, SUBSCRIPTION | `Receipt.paymentType` |
| `status` | `Keyword` | Always `"COMPLETED"` | Hardcoded |
| `amount` | `Scaled_Float` (scalingFactor = 100) | Stores 2 decimal places efficiently | `Receipt.amount.doubleValue()` |
| `currency` | `Keyword` | ISO currency code | `Receipt.currency` |
| `paymentMethod` | `Keyword` | Card, bank transfer, etc. | `Receipt.paymentMethod` |
| `paymentMethodDetails` | `Keyword` | Masked card number, etc. | `Receipt.paymentMethodDetails` |
| `description` | `Text` (standard) | Searchable payment description | `Receipt.description` |
| `subscriptionPlan` | `Keyword` | Associated plan (nullable) | `Receipt.subscriptionPlan` |
| `receiptUrl` | `Keyword` | Not indexed (`index = false`) | `Receipt.receiptUrl` |
| `emailSent` | `Boolean` | Whether receipt email was sent | `Receipt.emailSent` |
| `paidAt` | `Date` | Payment timestamp; used for date range queries | `Receipt.paidAt` |
| `createdAt` | `Date` | Record creation | `Receipt.createdAt` |
| `updatedAt` | `Date` | Last update | `Receipt.updatedAt` |

---

## Events & Enums

### `ElasticsearchIndexEvent`
**Package:** `com.extractor.unraveldocs.elasticsearch.events`

Serializable event envelope published to Kafka for async index processing.

```
ElasticsearchIndexEvent
├── documentId: String          ← UUID of the entity being indexed (used as Kafka message key)
├── action: IndexAction         ← CREATE | UPDATE | DELETE
├── indexType: IndexType        ← DOCUMENT | USER | PAYMENT | SUBSCRIPTION
├── timestamp: OffsetDateTime   ← When the event was created
└── payload: String             ← JSON-serialized index document (null for DELETE)
```

**Factory methods:**

| Method | Action | Payload |
|---|---|---|
| `ElasticsearchIndexEvent.createEvent(documentId, indexType, payload)` | `CREATE` | JSON string of index doc |
| `ElasticsearchIndexEvent.updateEvent(documentId, indexType, payload)` | `UPDATE` | JSON string of index doc |
| `ElasticsearchIndexEvent.deleteEvent(documentId, indexType)` | `DELETE` | `null` |

---

### `IndexAction`
**Package:** `com.extractor.unraveldocs.elasticsearch.events`

| Value | Description |
|---|---|
| `CREATE` | Create a new document in the ES index |
| `UPDATE` | Update an existing document in the ES index |
| `DELETE` | Remove a document from the ES index by ID |

---

### `IndexType`
**Package:** `com.extractor.unraveldocs.elasticsearch.events`

| Value | ES Index | Repository |
|---|---|---|
| `DOCUMENT` | `documents` | `DocumentSearchRepository` |
| `USER` | `users` | `UserSearchRepository` |
| `PAYMENT` | `payments` | `PaymentSearchRepository` |
| `SUBSCRIPTION` | *(not yet implemented)* | *(pending)* |

---

## Publisher

### `ElasticsearchEventPublisher`
**Package:** `com.extractor.unraveldocs.elasticsearch.publisher`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

Wraps `MessageBrokerFactory` to publish `ElasticsearchIndexEvent`s to the `unraveldocs-elasticsearch` Kafka topic. Uses `documentId` as the Kafka message key for partition ordering (all events for the same document land in the same partition, preserving causal order).

| Method | Event type header | Description |
|---|---|---|
| `publishDocumentIndexEvent(event)` | `"elasticsearch.index.document"` | Routes to document indexing |
| `publishUserIndexEvent(event)` | `"elasticsearch.index.user"` | Routes to user indexing |
| `publishPaymentIndexEvent(event)` | `"elasticsearch.index.payment"` | Routes to payment indexing |
| `publishSubscriptionIndexEvent(event)` | `"elasticsearch.index.subscription"` | Subscription indexing (not yet consumed) |
| `publishEvent(event)` | *(derived from IndexType)* | Auto-routes based on `event.getIndexType()` |
| `toJsonPayload(Object)` | — | Serialises any object to JSON string using `JsonMapper`; throws `RuntimeException` on failure |

**Internal send flow:**
```
event + eventType
    │
    ▼
Message.of(event, TOPIC_ELASTICSEARCH, event.documentId, {"event-type": eventType})
    │
    ▼
MessageBrokerFactory.getProducer(KAFKA).send(message)   ← async CompletableFuture
    │
    ├─ Success → log info
    └─ Failure → log error (does NOT throw — fire-and-forget for indexing events)
```

---

## Consumer

### `ElasticsearchIndexConsumer`
**Package:** `com.extractor.unraveldocs.elasticsearch.consumer`  
**Topic:** `unraveldocs-elasticsearch`  
**Group ID:** `elasticsearch-consumer-group`  
**Ack Mode:** `MANUAL_IMMEDIATE`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

The single Kafka listener that processes all indexing work. Routes to the correct repository based on `IndexType`, then performs the correct operation based on `IndexAction`.

**Dispatch logic:**
```
handleIndexEvent(ElasticsearchIndexEvent, Acknowledgment)
    │
    ├─ event == null → acknowledge + skip
    │
    └─ switch(event.indexType)
            ├─ DOCUMENT  → processDocumentEvent(event)
            ├─ USER      → processUserEvent(event)
            ├─ PAYMENT   → processPaymentEvent(event)
            └─ SUBSCRIPTION → log "not yet implemented"
    │
    └─ On success: acknowledgment.acknowledge()
       On failure: rethrow → DefaultErrorHandler → retry → unraveldocs-elasticsearch-dlq
```

**Per-type processing:**

| IndexType | DELETE action | CREATE / UPDATE action |
|---|---|---|
| `DOCUMENT` | `documentSearchRepository.deleteById(id)` | Deserialise JSON → `DocumentSearchIndex` → `documentSearchRepository.save(doc)` |
| `USER` | `userSearchRepository.deleteById(id)` | Deserialise JSON → `UserSearchIndex` → `userSearchRepository.save(user)` |
| `PAYMENT` | `paymentSearchRepository.deleteById(id)` | Deserialise JSON → `PaymentSearchIndex` → `paymentSearchRepository.save(payment)` |

**Deserialization:** `ObjectMapper.readValue(json, TargetClass.class)`. Throws `RuntimeException` (non-retryable path) if the JSON is malformed — routes directly to DLQ.

---

## Services

### `ElasticsearchIndexingService`
**Package:** `com.extractor.unraveldocs.elasticsearch.service`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

The **primary domain-facing facade** for indexing. Called by `SignupUserImpl`, `ChangePasswordImpl`, document upload services, payment services, etc. Maps JPA entities to index documents and delegates to `ElasticsearchEventPublisher`.

> **Key design principle:** All methods catch exceptions internally and log them without rethrowing. Indexing failures never propagate to the business operation caller — the system degrades gracefully (search may be stale but writes always succeed).

**Methods:**

| Method | Action | Description |
|---|---|---|
| `indexUser(User, IndexAction)` | `CREATE` or `UPDATE` | Maps `User` → `UserSearchIndex` → publishes `publishUserIndexEvent` |
| `indexDocument(DocumentCollection, FileEntry, OcrData, IndexAction)` | `CREATE` or `UPDATE` | Maps to `DocumentSearchIndex` (prefers `editedContent` over `extractedText`) → publishes `publishDocumentIndexEvent` |
| `indexPayment(Receipt, IndexAction)` | `CREATE` or `UPDATE` | Maps `Receipt` → `PaymentSearchIndex` → publishes `publishPaymentIndexEvent` |
| `deleteUserFromIndex(String userId)` | `DELETE` | Publishes `deleteEvent(userId, USER)` |
| `deleteDocumentFromIndex(String documentId)` | `DELETE` | Publishes `deleteEvent(documentId, DOCUMENT)` |

**Mapping — `User` → `UserSearchIndex`:**
- Reads subscription plan name and status if present; safely defaults to `null` if subscription or plan is missing.
- `documentCount` = `user.getDocuments().size()` or `0` if null.

**Mapping — `DocumentCollection + FileEntry + OcrData` → `DocumentSearchIndex`:**
- `extractedText` = `ocrData.editedContent` if non-null, else `ocrData.extractedText`, else `null`.
- `ocrStatus` = `ocrData.status.name()` if `ocrData` is non-null, else `null`.

**Mapping — `Receipt` → `PaymentSearchIndex`:**
- `status` is always hardcoded to `"COMPLETED"`.
- `userName` = `user.firstName + " " + user.lastName`.
- `amount` = `receipt.amount.doubleValue()` or `null` if amount is null.

---

### `ElasticsearchSyncService`
**Package:** `com.extractor.unraveldocs.elasticsearch.service`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

Handles **bulk re-indexing** of existing PostgreSQL data into Elasticsearch. Used for initial migration and recovery scenarios. Reads in pages of 100 records (`BATCH_SIZE = 100`) to avoid OOM.

**Methods:**

| Method | Transaction | Async | Description |
|---|---|---|---|
| `syncAll()` | No | `@Async` → returns `CompletableFuture<Map<String, Object>>` | Runs `syncAllUsers()` + `syncAllDocuments()` + `syncAllPayments()` sequentially; returns summary map |
| `syncAllUsers()` | `@Transactional(readOnly = true)` | No | Paginates all `User` records → maps each → `userSearchRepository.saveAll(batch)` |
| `syncAllDocuments()` | `@Transactional(readOnly = true)` | No | Paginates all `DocumentCollection` records; batch-fetches `OcrData` by `documentId` list; maps each `FileEntry` → `documentSearchRepository.saveAll(batch)` |
| `syncAllPayments()` | `@Transactional(readOnly = true)` | No | Paginates all `Receipt` records → maps each → `paymentSearchRepository.saveAll(batch)` |
| `indexUser(User)` | No | No | Single-document live update: `userSearchRepository.save(index)` |
| `indexDocument(DocumentCollection, FileEntry, OcrData)` | No | No | Single-document live update: `documentSearchRepository.save(index)` |
| `indexPayment(Receipt)` | No | No | Single-document live update: `paymentSearchRepository.save(index)` |

**`syncAll()` response map:**

| Key | Type | Description |
|---|---|---|
| `usersIndexed` | `Integer` | Number of users indexed |
| `documentsIndexed` | `Integer` | Number of documents indexed |
| `paymentsIndexed` | `Integer` | Number of payments indexed |
| `status` | `String` | `"SUCCESS"` or `"FAILED"` |
| `durationMs` | `Long` | Total execution time in milliseconds |
| `error` | `String` | Error message (only on `"FAILED"`) |

**Document sync optimisation:** Instead of fetching OCR data one-by-one per file, all `documentId`s in a page are collected first, then `ocrDataRepository.findByDocumentIdIn(ids)` is called once and the results are stored in a `Map<String, OcrData>` for O(1) lookup per file entry.

---

### `DocumentSearchService`
**Package:** `com.extractor.unraveldocs.elasticsearch.service`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

Handles document search and individual document index operations for authenticated users.

**Search methods:**

| Method | Description | Repository Method |
|---|---|---|
| `searchDocuments(userId, SearchRequest)` | Main search: with query → `searchDocuments(userId, query, pageable)`, without → `findByUserId(userId, pageable)` | `DocumentSearchRepository` |
| `searchByContent(userId, query, page, size)` | Targeted OCR text search | `searchByUserIdAndExtractedText()` |
| `getDocumentsByUserId(userId, Pageable)` | Bare listing without text filtering | `findByUserId()` |

**Index/delete methods:**

| Method | Mechanism | Description |
|---|---|---|
| `indexDocument(DocumentSearchIndex)` | Async via `ElasticsearchEventPublisher` | Publishes CREATE event to Kafka |
| `indexDocumentSync(DocumentSearchIndex)` | Direct repository save | For bulk/sync operations |
| `deleteDocument(String documentId)` | Async via `ElasticsearchEventPublisher` | Publishes DELETE event to Kafka |
| `deleteDocumentsByUserId(String userId)` | Direct `deleteByUserId()` | Removes all user's documents (e.g., on account deletion) |
| `deleteDocumentsByCollectionId(String collectionId)` | Direct `deleteByCollectionId()` | Removes a full collection from the index |

**Text preview:** The `toSearchResult()` mapper truncates `extractedText` to `200` characters with `"..."` suffix for the `textPreview` field in `DocumentSearchResult`.

**Pageable creation:** `sortBy` defaults to `"createdAt"`, `sortDirection` to `"desc"`, `page` to `0`, `size` to `10`.

---

### `UserSearchService`
**Package:** `com.extractor.unraveldocs.elasticsearch.service`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

Handles user search for admin dashboards. Supports query + filter combinations.

**`searchUsers(SearchRequest)` filter routing:**

| Has query? | Filter key | Repository method |
|---|---|---|
| Yes | `role` | `searchUsersByRole(query, role, pageable)` |
| Yes | `isActive` | `searchUsersByActiveStatus(query, isActive, pageable)` |
| Yes | *(none)* | `searchUsers(query, pageable)` |
| No | `role` | `findByRole(role, pageable)` |
| No | `isActive` | `findByIsActive(isActive, pageable)` |
| No | `country` | `findByCountry(country, pageable)` |
| No | *(none)* | `findAll(pageable)` |

**Additional methods:**

| Method | Description |
|---|---|
| `findByRole(role, Pageable)` | Direct repository delegation |
| `findByActiveStatus(isActive, Pageable)` | Direct repository delegation |
| `findByVerifiedStatus(isVerified, Pageable)` | Direct repository delegation |
| `indexUser(UserSearchIndex)` | Async event publish (CREATE) |
| `indexUserSync(UserSearchIndex)` | Direct save |
| `updateUser(UserSearchIndex)` | Async event publish (UPDATE) |
| `deleteUser(String userId)` | Async event publish (DELETE) |

---

### `PaymentSearchService`
**Package:** `com.extractor.unraveldocs.elasticsearch.service`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

Handles payment search for admin oversight.

**`searchPayments(SearchRequest)` filter routing:**

| Has query? | Filter key | Repository method |
|---|---|---|
| Yes | `paymentProvider` | `searchPaymentsByProvider(query, provider, pageable)` |
| Yes | `status` | `searchPaymentsByStatus(query, status, pageable)` |
| Yes | *(none)* | `searchPayments(query, pageable)` |
| No | `paymentProvider` | `findByPaymentProvider(provider, pageable)` |
| No | `status` | `findByStatus(status, pageable)` |
| No | `currency` | `findByCurrency(currency, pageable)` |
| No | dateFrom + dateTo | `findByPaidAtBetween(dateFrom, dateTo, pageable)` |
| No | *(none)* | `findAll(pageable)` |

**Additional methods:**

| Method | Description |
|---|---|
| `getPaymentsByUserId(userId, Pageable)` | Payments for a specific user |
| `findByReceiptNumber(receiptNumber, Pageable)` | Exact receipt lookup |
| `indexPayment(PaymentSearchIndex)` | Async event publish (CREATE) |
| `indexPaymentSync(PaymentSearchIndex)` | Direct save |
| `deletePayment(String paymentId)` | Async event publish (DELETE) |
| `deletePaymentsByUserId(String userId)` | Direct `deleteByUserId()` |

---

## Repositories

All three repositories extend `ElasticsearchRepository<TDocument, String>` which provides `save()`, `saveAll()`, `findById()`, `deleteById()`, `findAll(Pageable)`, and `count()` out of the box. The custom methods below add domain-specific queries.

### `DocumentSearchRepository`

| Method | Query Type | Description |
|---|---|---|
| `findByUserId(userId, Pageable)` | Derived | All documents for a user |
| `findByUserIdAndStatus(userId, status, Pageable)` | Derived | Filter by status |
| `findByUserIdAndFileType(userId, fileType, Pageable)` | Derived | Filter by extension |
| `searchByUserIdAndExtractedText(userId, query, Pageable)` | `@Query` | `bool.must[term userId] + must[match extractedText]` |
| `searchDocuments(userId, query, Pageable)` | `@Query` | Multi-field boosted search — `extractedText ×2`, `fileName ×1.5`, `fileName.keyword wildcard ×1` |
| `findByUserIdOrderByCreatedAtDesc(userId)` | Derived | All docs sorted by newest |
| `deleteByUserId(userId)` | Derived | Bulk delete by user |
| `deleteByCollectionId(collectionId)` | Derived | Bulk delete by collection |

---

### `UserSearchRepository`

| Method | Query Type | Description |
|---|---|---|
| `findByEmail(email, Pageable)` | Derived | Exact email lookup |
| `findByRole(role, Pageable)` | Derived | Filter by role |
| `findByIsActive(isActive, Pageable)` | Derived | Filter by active status |
| `findByIsVerified(isVerified, Pageable)` | Derived | Filter by verification |
| `findByCountry(country, Pageable)` | Derived | Filter by country |
| `searchUsers(query, Pageable)` | `@Query` | Multi-field: `firstName ×2`, `lastName ×2`, `email wildcard ×1.5`, `organization ×1`, `profession ×1` |
| `searchUsersByRole(query, role, Pageable)` | `@Query` | `bool.must[term role] + should[firstName/lastName/email wildcard]` |
| `searchUsersByActiveStatus(query, isActive, Pageable)` | `@Query` | `bool.must[term isActive] + should[firstName/lastName/email wildcard]` |

---

### `PaymentSearchRepository`

| Method | Query Type | Description |
|---|---|---|
| `findByUserId(userId, Pageable)` | Derived | All payments for a user |
| `findByReceiptNumber(receiptNumber, Pageable)` | Derived | Exact receipt number lookup |
| `findByPaymentProvider(paymentProvider, Pageable)` | Derived | Filter by provider |
| `findByStatus(status, Pageable)` | Derived | Filter by status |
| `findByCurrency(currency, Pageable)` | Derived | Filter by currency |
| `findByPaidAtBetween(start, end, Pageable)` | Derived | Date range query |
| `searchPayments(query, Pageable)` | `@Query` | Multi-field: `receiptNumber term`, `userEmail wildcard`, `userName match`, `description match`, `externalPaymentId term` |
| `searchPaymentsByProvider(query, provider, Pageable)` | `@Query` | `bool.must[term paymentProvider] + should[receiptNumber/userEmail/userName]` |
| `searchPaymentsByStatus(query, status, Pageable)` | `@Query` | `bool.must[term status] + should[receiptNumber/userEmail/userName]` |
| `deleteByUserId(userId)` | Derived | Bulk delete by user |

---

## DTOs

### `SearchRequest`
**Package:** `com.extractor.unraveldocs.elasticsearch.dto`

Generic, reusable search request used across all three search services.

| Field | Type | Default | Constraints | Description |
|---|---|---|---|---|
| `query` | `String` | `null` | — | Full-text search query; if null or blank, filters-only mode |
| `page` | `Integer` | `0` | `@Min(0)` | Zero-indexed page number |
| `size` | `Integer` | `10` | `@Min(1)`, `@Max(100)` | Results per page |
| `sortBy` | `String` | `"createdAt"` | — | Field name to sort by |
| `sortDirection` | `String` | `"desc"` | — | `"asc"` or `"desc"` |
| `filters` | `Map<String, Object>` | `{}` | — | Key-value filter pairs (e.g., `{"role": "ADMIN"}`, `{"isActive": true}`) |
| `dateFrom` | `OffsetDateTime` | `null` | — | Start of date range (payments only) |
| `dateTo` | `OffsetDateTime` | `null` | — | End of date range (payments only) |
| `includeHighlights` | `Boolean` | `true` | — | Reserved for future highlight extraction |

---

### `SearchResponse<T>`
**Package:** `com.extractor.unraveldocs.elasticsearch.dto`

Generic, pageable search response wrapping any index document type.

| Field | Type | Description |
|---|---|---|
| `results` | `List<T>` | Page of matched documents |
| `totalHits` | `Long` | Total number of matching documents across all pages |
| `page` | `Integer` | Current zero-indexed page number |
| `size` | `Integer` | Page size |
| `totalPages` | `Integer` | Total number of pages |
| `took` | `Long` | Query execution time in ms (populated when available) |
| `highlights` | `Map<String, List<String>>` | Highlighted snippets keyed by document ID (future use) |
| `facets` | `Map<String, Map<String, Long>>` | Aggregation counts (future use) |

**Helper methods:**
- `hasNext()` — `true` if `page < totalPages - 1`
- `hasPrevious()` — `true` if `page > 0`
- `fromPage(Page<T>)` — static factory from Spring Data `Page<T>`

---

### `DocumentSearchResult`
**Package:** `com.extractor.unraveldocs.elasticsearch.dto`

Enriched result DTO used specifically for document search — adds text preview and highlights list.

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Document UUID |
| `collectionId` | `String` | Collection UUID |
| `fileName` | `String` | Original file name |
| `fileType` | `String` | File extension |
| `fileSize` | `Long` | Size in bytes |
| `status` | `String` | Collection status |
| `ocrStatus` | `String` | OCR processing status |
| `textPreview` | `String` | First 200 characters of extracted text + `"..."` if truncated |
| `highlights` | `List<String>` | Highlight snippets (populated in future) |
| `fileUrl` | `String` | Download URL |
| `uploadTimestamp` | `OffsetDateTime` | When uploaded |
| `createdAt` | `OffsetDateTime` | Record creation |

---

## Endpoints

### Document Search (User)

**Base path:** `/api/v1/search/documents`  
**Auth:** Authenticated (`@AuthenticationPrincipal User`)  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

#### POST `/api/v1/search/documents` — Full Search
Advanced search with body.

**Request body:** `SearchRequest`
```json
{
  "query": "invoice agreement",
  "page": 0,
  "size": 10,
  "sortBy": "createdAt",
  "sortDirection": "desc",
  "filters": {},
  "includeHighlights": true
}
```

**Response — `200 OK`:** `SearchResponse<DocumentSearchResult>`
```json
{
  "results": [
    {
      "id": "doc-uuid",
      "collectionId": "col-uuid",
      "fileName": "contract.pdf",
      "fileType": "pdf",
      "fileSize": 204800,
      "status": "COMPLETED",
      "ocrStatus": "COMPLETED",
      "textPreview": "This service agreement is entered into on January 1, 2026 between...",
      "highlights": [],
      "fileUrl": "https://...",
      "uploadTimestamp": "2026-01-15T09:00:00Z",
      "createdAt": "2026-01-15T09:01:00Z"
    }
  ],
  "totalHits": 42,
  "page": 0,
  "size": 10,
  "totalPages": 5
}
```

---

#### GET `/api/v1/search/documents/content` — OCR Content Search
Searches only within extracted OCR text.

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `query` | `String` | ✅ | — | Text to search within extracted OCR content |
| `page` | `int` | ❌ | `0` | Page number |
| `size` | `int` | ❌ | `10` | Page size |

**Response:** `200 OK` — `SearchResponse<DocumentSearchResult>`

---

#### GET `/api/v1/search/documents` — Quick Search
Simple query-param search.

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `query` | `String` | ❌ | `null` | Search term |
| `page` | `int` | ❌ | `0` | Page number |
| `size` | `int` | ❌ | `10` | Page size |
| `sortBy` | `String` | ❌ | `"createdAt"` | Sort field |
| `sortDirection` | `String` | ❌ | `"desc"` | `"asc"` or `"desc"` |

**Response:** `200 OK` — `SearchResponse<DocumentSearchResult>`

---

### Admin User Search

**Base path:** `/api/v1/admin/search`  
**Auth:** `ROLE_ADMIN` or `ROLE_SUPER_ADMIN`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

#### POST `/api/v1/admin/search/users` — Advanced User Search

**Request body:** `SearchRequest` with `filters` map supporting keys:

| Filter Key | Type | Description |
|---|---|---|
| `role` | `String` | Filter by user role (e.g., `"ADMIN"`, `"USER"`) |
| `isActive` | `Boolean` | Filter by active status |
| `country` | `String` | Filter by country (no-query mode only) |

**Response — `200 OK`:** `SearchResponse<UserSearchIndex>`

---

#### GET `/api/v1/admin/search/users` — Quick User Search

| Parameter | Type | Required | Description |
|---|---|---|---|
| `query` | `String` | ❌ | Full-text search term |
| `role` | `String` | ❌ | Role filter |
| `isActive` | `Boolean` | ❌ | Active status filter |
| `country` | `String` | ❌ | Country filter |
| `page` | `int` | ❌ | Default `0` |
| `size` | `int` | ❌ | Default `10` |
| `sortBy` | `String` | ❌ | Default `"createdAt"` |
| `sortDirection` | `String` | ❌ | Default `"desc"` |

> **Note:** `role`, `isActive`, and `country` are mutually exclusive — only the first non-null one is applied.

**Response — `200 OK`:** `SearchResponse<UserSearchIndex>`

---

### Admin Payment Search

#### POST `/api/v1/admin/search/payments` — Advanced Payment Search

**Request body:** `SearchRequest` with `filters` supporting:

| Filter Key | Type | Description |
|---|---|---|
| `paymentProvider` | `String` | `"STRIPE"`, `"PAYSTACK"`, `"PAYPAL"` |
| `status` | `String` | Payment status |
| `currency` | `String` | ISO currency code |

Also supports `dateFrom` / `dateTo` for date-range queries (no-query mode only).

**Response — `200 OK`:** `SearchResponse<PaymentSearchIndex>`

---

#### GET `/api/v1/admin/search/payments` — Quick Payment Search

| Parameter | Type | Required | Description |
|---|---|---|---|
| `query` | `String` | ❌ | Searches receipt number, email, name, description |
| `paymentProvider` | `String` | ❌ | Provider filter |
| `status` | `String` | ❌ | Status filter |
| `currency` | `String` | ❌ | Currency filter |
| `page` | `int` | ❌ | Default `0` |
| `size` | `int` | ❌ | Default `10` |
| `sortBy` | `String` | ❌ | Default `"createdAt"` |
| `sortDirection` | `String` | ❌ | Default `"desc"` |

**Response — `200 OK`:** `SearchResponse<PaymentSearchIndex>`

---

#### GET `/api/v1/admin/search/payments/receipt/{receiptNumber}` — Receipt Lookup

| Parameter | Type | Description |
|---|---|---|
| `receiptNumber` | `String` (path) | Exact receipt number |

**Response — `200 OK`:** `SearchResponse<PaymentSearchIndex>` (0 or 1 result)

---

### Admin Sync Endpoints

**Base path:** `/api/v1/admin/elasticsearch`  
**Auth:** `ROLE_ADMIN` or `ROLE_SUPER_ADMIN`  
**Conditional:** `@ConditionalOnProperty(name = "spring.elasticsearch.uris")`

#### POST `/api/v1/admin/elasticsearch/sync` — Full Sync (Async)

Triggers `syncAll()` in the background. Returns immediately.

**Response — `202 Accepted`:**
```json
{
  "message": "Full synchronization started in background",
  "status": "STARTED"
}
```

---

#### POST `/api/v1/admin/elasticsearch/sync/users` — Sync Users (Synchronous)

**Response — `200 OK`:**
```json
{
  "message": "User synchronization completed",
  "usersIndexed": 1234
}
```

---

#### POST `/api/v1/admin/elasticsearch/sync/documents` — Sync Documents (Synchronous)

**Response — `200 OK`:**
```json
{
  "message": "Document synchronization completed",
  "documentsIndexed": 5678
}
```

---

#### POST `/api/v1/admin/elasticsearch/sync/payments` — Sync Payments (Synchronous)

**Response — `200 OK`:**
```json
{
  "message": "Payment synchronization completed",
  "paymentsIndexed": 910
}
```

---

## Elasticsearch Queries Reference

### Document Full-Text Search (`searchDocuments`)

Targets the `documents` index. Requires a matching `userId` and at least one of the `should` clauses.

```json
{
  "bool": {
    "must": [
      { "term": { "userId": "<userId>" } }
    ],
    "should": [
      { "match": { "extractedText": { "query": "<query>", "boost": 2 } } },
      { "match": { "fileName":      { "query": "<query>", "boost": 1.5 } } },
      { "wildcard": { "fileName.keyword": { "value": "*<query>*", "boost": 1 } } }
    ],
    "minimum_should_match": 1
  }
}
```

**Boost priorities:** OCR text ×2 > filename match ×1.5 > filename wildcard ×1

---

### Document Content-Only Search (`searchByUserIdAndExtractedText`)

Strict search within the `extractedText` field only.

```json
{
  "bool": {
    "must": [
      { "term":  { "userId": "<userId>" } },
      { "match": { "extractedText": "<query>" } }
    ]
  }
}
```

---

### User Multi-Field Search (`searchUsers`)

Targets the `users` index. No required `must` clause — searches broadly.

```json
{
  "bool": {
    "should": [
      { "match":    { "firstName":    { "query": "<query>", "boost": 2 } } },
      { "match":    { "lastName":     { "query": "<query>", "boost": 2 } } },
      { "wildcard": { "email":        { "value": "*<query>*", "boost": 1.5 } } },
      { "match":    { "organization": { "query": "<query>", "boost": 1 } } },
      { "match":    { "profession":   { "query": "<query>", "boost": 1 } } }
    ],
    "minimum_should_match": 1
  }
}
```

---

### User Search with Role Filter (`searchUsersByRole`)

```json
{
  "bool": {
    "must": [
      { "term": { "role": "<role>" } }
    ],
    "should": [
      { "match":    { "firstName": { "query": "<query>", "boost": 2 } } },
      { "match":    { "lastName":  { "query": "<query>", "boost": 2 } } },
      { "wildcard": { "email":     { "value": "*<query>*", "boost": 1.5 } } }
    ],
    "minimum_should_match": 1
  }
}
```

---

### Payment Multi-Field Search (`searchPayments`)

```json
{
  "bool": {
    "should": [
      { "term":     { "receiptNumber":    "<query>" } },
      { "wildcard": { "userEmail":        { "value": "*<query>*" } } },
      { "match":    { "userName":         "<query>" } },
      { "match":    { "description":      "<query>" } },
      { "term":     { "externalPaymentId": "<query>" } }
    ],
    "minimum_should_match": 1
  }
}
```

---

## Data Mapping Reference

### JPA Entity → Elasticsearch Index Document

| JPA Source | ES Index | Key Mapping Notes |
|---|---|---|
| `User` | `users` | Subscription plan/status derived from `user.subscription.plan.name.name()` — safe-null handled |
| `DocumentCollection + FileEntry + OcrData` | `documents` | `extractedText` uses `ocrData.editedContent` if available; `ocrData` may be `null` for unprocessed files |
| `Receipt` | `payments` | `status` always `"COMPLETED"`; `userName` concatenated from firstName + lastName |

### When Indexing Is Triggered

| Entity | Trigger | Method Called |
|---|---|---|
| `User` | Registration, profile update, role change | `ElasticsearchIndexingService.indexUser(user, CREATE/UPDATE)` |
| `User` | Account deletion | `ElasticsearchIndexingService.deleteUserFromIndex(userId)` |
| `FileEntry + DocumentCollection` | Document upload, OCR completion | `ElasticsearchIndexingService.indexDocument(collection, file, ocr, CREATE/UPDATE)` |
| `FileEntry` | Document deletion | `ElasticsearchIndexingService.deleteDocumentFromIndex(documentId)` |
| `Receipt` | Payment completion | `ElasticsearchIndexingService.indexPayment(receipt, CREATE)` |

---

## Configuration Reference

Minimum `application.properties` to enable Elasticsearch:

```properties
# Required — activates all @ConditionalOnProperty Elasticsearch beans
spring.elasticsearch.uris=http://localhost:9200

# Optional — adjust for secured clusters
# spring.elasticsearch.username=elastic
# spring.elasticsearch.password=changeme
```

To **disable Elasticsearch** (e.g., local dev without an ES cluster): omit `spring.elasticsearch.uris` — all Elasticsearch beans are silently skipped.

---

## Flow Diagrams

### Real-Time Indexing Flow (Write Path)

```
Domain Service (e.g., SignupUserImpl, DocumentUploadImpl)
  │
  └─ elasticsearchIndexingService.indexUser(user, CREATE)
         │
         ├─ Map User → UserSearchIndex
         ├─ eventPublisher.toJsonPayload(searchIndex)
         ├─ ElasticsearchIndexEvent.createEvent(userId, USER, jsonPayload)
         └─ eventPublisher.publishUserIndexEvent(event)
                 │
                 └─ Message.of(event, TOPIC_ELASTICSEARCH, userId, {"event-type": "elasticsearch.index.user"})
                         │
                         ▼
                 KafkaMessageProducer.send()  ← async, non-blocking
                         │
                         ▼
                 Kafka: unraveldocs-elasticsearch (partition = hash(userId))
                         │
                         ▼
                 ElasticsearchIndexConsumer (@KafkaListener)
                 group: elasticsearch-consumer-group
                         │
                         ├─ switch(indexType = USER)
                         ├─ action = CREATE
                         └─ deserialize JSON → UserSearchIndex
                                 │
                                 ▼
                         userSearchRepository.save(userSearchIndex)
                                 │
                                 ▼
                         Elasticsearch: users index ✓
```

---

### Search Flow (Read Path)

```
Authenticated User / Admin HTTP Client
  │
  ├─ POST /api/v1/search/documents
  │   { "query": "contract 2026" }
  │
  ▼
DocumentSearchController.searchDocuments(user, request)
  │
  ▼
DocumentSearchService.searchDocuments(userId, request)
  │
  ├─ query != blank?
  │   └─ YES → documentSearchRepository.searchDocuments(userId, "contract 2026", pageable)
  │                   │
  │                   └─ Elasticsearch DSL query (bool/must/should with boosts)
  │                         ├─ extractedText match ×2
  │                         ├─ fileName match ×1.5
  │                         └─ fileName wildcard ×1
  │
  ├─ Map each DocumentSearchIndex → DocumentSearchResult
  │   └─ textPreview = first 200 chars of extractedText
  │
  └─ Wrap in SearchResponse<DocumentSearchResult>
           │
           ▼
  200 OK { results, totalHits, page, size, totalPages }
```

---

### Bulk Sync Flow (Admin)

```
Admin → POST /api/v1/admin/elasticsearch/sync
  │
  ▼
ElasticsearchSyncController.syncAll()
  │
  └─ syncService.syncAll()   ← @Async — runs in background thread pool
         │
         ├─ syncAllUsers()
         │   ├─ Page 0: userRepository.findAll(PageRequest(0, 100))
         │   │   └─ map each User → UserSearchIndex → userSearchRepository.saveAll(batch)
         │   ├─ Page 1: ... (repeat until no more pages)
         │   └─ return totalIndexed
         │
         ├─ syncAllDocuments()
         │   ├─ Page 0: documentCollectionRepository.findAll(PageRequest(0, 100))
         │   │   ├─ Collect all documentIds from all FileEntries in the page
         │   │   ├─ ocrDataRepository.findByDocumentIdIn(ids)  ← single batch query
         │   │   │   └─ Map<String, OcrData> for O(1) lookup
         │   │   └─ map each FileEntry → DocumentSearchIndex → documentSearchRepository.saveAll(batch)
         │   └─ return totalIndexed
         │
         └─ syncAllPayments()
             ├─ Page 0: receiptRepository.findAll(PageRequest(0, 100))
             │   └─ map each Receipt → PaymentSearchIndex → paymentSearchRepository.saveAll(batch)
             └─ return totalIndexed

Admin ← 202 Accepted { "status": "STARTED" }
         (sync runs asynchronously — check logs for completion)
```

---

### Consumer Error Handling Flow

```
ElasticsearchIndexConsumer receives malformed event
  │
  ├─ event == null
  │     └─ acknowledge + skip (no retry, no DLQ)
  │
  ├─ JSON deserialization fails (e.g., schema mismatch)
  │     └─ RuntimeException → rethrow
  │           └─ DefaultErrorHandler
  │                 ├─ Is RuntimeException non-retryable? → YES (ClassCastException etc.) → DLQ immediately
  │                 └─ NO → ExponentialBackOff (1s → 2s → 4s)
  │                           └─ After 3 attempts → unraveldocs-elasticsearch-dlq
  │
  └─ Repository save fails (ES connectivity issue)
        └─ Exception → rethrow
              └─ DefaultErrorHandler → ExponentialBackOff → retry → DLQ
```

