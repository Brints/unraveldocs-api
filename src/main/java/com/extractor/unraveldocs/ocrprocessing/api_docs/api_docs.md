# OCR Processing API Documentation

> **Base URL:** `/api/v1/collections`
>
> **Authentication:** All endpoints require a valid JWT Bearer token in the `Authorization` header.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Endpoints](#endpoints)
  - [Upload & Extract All](#1-upload--extract-all)
  - [Extract Single Document](#2-extract-text-from-single-document)
  - [Get Collection Results](#3-get-collection-results)
  - [Get Document OCR Data](#4-get-document-ocr-data)
  - [Update Edited Content](#5-update-edited-content)
- [Data Models](#data-models)
  - [OcrData Entity](#ocrdata-entity)
  - [Enums](#enums)
- [Request / Response DTOs](#request--response-dtos)
  - [DocumentCollectionResponse\<T\>](#documentcollectionresponset)
  - [FileResultData](#fileresultdata)
  - [CollectionResultResponse](#collectionresultresponse)
  - [UpdateOcrContentRequest](#updateocrcontentrequest)
  - [DocumentCollectionUploadData](#documentcollectionuploaddata)
- [Processing Pipeline](#processing-pipeline)
  - [OCR Providers](#ocr-providers)
  - [Processing Flow](#processing-flow)
  - [Elasticsearch Integration](#elasticsearch-integration)
  - [Notifications](#notifications)
- [Rich Text Editing](#rich-text-editing)
  - [Content Formats](#content-formats)
  - [HTML Sanitization](#html-sanitization)
- [Error Handling](#error-handling)
- [Database Schema](#database-schema)

---

## Overview

The OCR Processing module extracts text from uploaded documents (images, PDFs, Office files) using configurable OCR providers. It supports:

- **Bulk upload & extraction** — upload multiple files and trigger OCR for all at once.
- **Single document extraction** — extract text from a specific file with optional page selection for PDFs.
- **Result retrieval** — fetch OCR results per document or per collection.
- **Rich text editing** — users can review, correct, and format OCR-extracted text with HTML or Markdown.
- **Full-text search** — extracted/edited content is indexed in Elasticsearch.
- **AI enhancements** — AI-generated summaries, document classification, and tagging.
- **Push notifications** — users are notified when OCR processing starts, completes, or fails.

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  OcrDocumentController                    │
│   POST /upload/extract-all                               │
│   POST /{collectionId}/document/{documentId}/extract     │
│   GET  /{collectionId}/document/results                  │
│   GET  /{collectionId}/document/{documentId}/ocr-data    │
│   PUT  /{collectionId}/document/{documentId}/content     │
└────────────────────────┬─────────────────────────────────┘
                         │
                    ┌────▼────┐
                    │OcrService│  (Facade)
                    └────┬────┘
                         │
          ┌──────────────┼──────────────────────┐
          │              │                      │
          ▼              ▼                      ▼
    ┌───────────┐  ┌──────────┐    ┌─────────────────────┐
    │ Bulk      │  │ Extract  │    │ UpdateOcrContent     │
    │ Upload &  │  │ Single   │    │ Impl                 │
    │ Extract   │  │ Document │    │ (Rich text editing)  │
    └─────┬─────┘  └────┬─────┘    └──────────┬──────────┘
          │              │                     │
          ▼              ▼                     ▼
    ┌───────────────────────────────────────────────┐
    │              OcrDataRepository                │
    └──────────────────┬────────────────────────────┘
                       │
    ┌──────────────────┼────────────────┐
    │                  │                │
    ▼                  ▼                ▼
┌─────────┐   ┌──────────────┐  ┌───────────┐
│ ProcessOcr│  │Elasticsearch │  │ Database  │
│ (async)   │  │Indexing      │  │ (ocr_data)│
└─────┬─────┘  └──────────────┘  └───────────┘
      │
      ▼
┌───────────────────┐
│ OcrProviderFactory│
│  ├─ Tesseract     │
│  └─ Google Vision │
└───────────────────┘
```

### Package Structure

| Package          | Purpose                                           |
| ---------------- | ------------------------------------------------- |
| `controller`     | REST endpoints for OCR operations                 |
| `service`        | Facade (`OcrService`), processing (`ProcessOcr`)  |
| `impl`           | Service interface implementations                 |
| `interfaces`     | Service contracts (one per operation)              |
| `model`          | JPA entities                                      |
| `dto/request`    | Inbound request DTOs                              |
| `dto/response`   | Outbound response DTOs                            |
| `datamodel`      | Enums (`OcrStatus`, `ContentFormat`)              |
| `provider`       | OCR provider abstraction (Tesseract, Google Cloud) |
| `repository`     | Spring Data JPA repositories                      |
| `utils`          | Shared utilities (validation, file storage, etc.) |
| `events`         | Event-driven OCR processing triggers              |
| `config`         | OCR-specific configuration                        |
| `metrics`        | OCR processing metrics                            |
| `quota`          | OCR quota management                              |
| `exception`      | OCR-specific exceptions                           |

---

## Endpoints

### 1. Upload & Extract All

Upload multiple files and trigger OCR extraction for the entire collection.

```
POST /api/v1/collections/upload/extract-all
Content-Type: multipart/form-data
```

**Request:**

| Parameter | Type               | Required | Description              |
| --------- | ------------------ | -------- | ------------------------ |
| `files`   | `MultipartFile[]`  | ✅       | Array of files to upload |

**Response:** `200 OK`

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Documents uploaded and OCR extraction initiated.",
  "data": {
    "collectionId": "abc-123",
    "collectionName": "My Documents",
    "documents": [
      {
        "documentId": "doc-456",
        "originalFileName": "invoice.pdf",
        "fileType": "application/pdf",
        "fileSize": 102400,
        "fileUrl": "https://storage.example.com/invoice.pdf"
      }
    ]
  }
}
```

**Errors:**

| Code  | Condition                          |
| ----- | ---------------------------------- |
| `400` | No files provided                  |
| `403` | User not authenticated             |

---

### 2. Extract Text from Single Document

Trigger OCR text extraction for a single document. Returns existing data if already processed.

```
POST /api/v1/collections/{collectionId}/document/{documentId}/extract
```

**Path Parameters:**

| Parameter      | Type     | Description                 |
| -------------- | -------- | --------------------------- |
| `collectionId` | `String` | ID of the document collection |
| `documentId`   | `String` | ID of the document            |

**Query Parameters (PDF only):**

| Parameter   | Type            | Required | Description                                    |
| ----------- | --------------- | -------- | ---------------------------------------------- |
| `startPage` | `Integer`       | No       | Start page (1-indexed, inclusive)               |
| `endPage`   | `Integer`       | No       | End page (1-indexed, inclusive)                 |
| `pages`     | `List<Integer>` | No       | Specific pages (comma-separated). Overrides start/end page. Example: `?pages=3,8,16` |

**Response:** `200 OK`

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Text extraction completed successfully.",
  "data": {
    "id": "ocr-789",
    "documentId": "doc-456",
    "status": "COMPLETED",
    "extractedText": "Lorem ipsum dolor sit amet...",
    "editedContent": null,
    "contentFormat": null,
    "editedBy": null,
    "editedAt": null,
    "errorMessage": null,
    "aiSummary": null,
    "documentType": null,
    "aiTags": null,
    "createdAt": "2026-02-21T12:00:00Z",
    "updatedAt": "2026-02-21T12:01:30Z"
  }
}
```

**Errors:**

| Code  | Condition                          |
| ----- | ---------------------------------- |
| `403` | User not authorized / not logged in |
| `404` | Collection or file not found       |
| `500` | OCR processing failed              |

---

### 3. Get Collection Results

Retrieve OCR results for all documents in a collection.

```
GET /api/v1/collections/{collectionId}/document/results
```

**Path Parameters:**

| Parameter      | Type     | Description                  |
| -------------- | -------- | ---------------------------- |
| `collectionId` | `String` | ID of the document collection |

**Response:** `200 OK`

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Collection results retrieved successfully.",
  "data": {
    "collectionId": "abc-123",
    "overallStatus": "PROCESSED",
    "files": [
      {
        "documentId": "doc-456",
        "originalFileName": "invoice.pdf",
        "status": "COMPLETED",
        "errorMessage": null,
        "createdAt": "2026-02-21T12:00:00Z",
        "extractedText": "Lorem ipsum...",
        "editedContent": "<p>Corrected text...</p>",
        "contentFormat": "HTML",
        "editedBy": "user-789",
        "editedAt": "2026-02-21T13:00:00Z",
        "aiSummary": "An invoice for services rendered...",
        "documentType": "invoice",
        "aiTags": ["finance", "invoice", "billing"]
      }
    ]
  }
}
```

**Errors:**

| Code  | Condition              |
| ----- | ---------------------- |
| `403` | User not authorized    |
| `404` | Collection not found   |

---

### 4. Get Document OCR Data

Retrieve OCR data for a specific document.

```
GET /api/v1/collections/{collectionId}/document/{documentId}/ocr-data
```

**Path Parameters:**

| Parameter      | Type     | Description                  |
| -------------- | -------- | ---------------------------- |
| `collectionId` | `String` | ID of the document collection |
| `documentId`   | `String` | ID of the document            |

**Response:** `200 OK`

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "OCR data retrieved successfully.",
  "data": {
    "documentId": "doc-456",
    "originalFileName": "contract.pdf",
    "status": "COMPLETED",
    "errorMessage": null,
    "createdAt": "2026-02-21T12:00:00Z",
    "extractedText": "Original OCR output text...",
    "editedContent": null,
    "contentFormat": null,
    "editedBy": null,
    "editedAt": null,
    "aiSummary": "A legal contract between...",
    "documentType": "legal_contract",
    "aiTags": ["legal", "contract"]
  }
}
```

**Errors:**

| Code  | Condition                         |
| ----- | --------------------------------- |
| `403` | User not authorized / not logged in |
| `404` | Document or OCR data not found    |

---

### 5. Update Edited Content

Save user-reviewed and edited content for a document. Supports both HTML and Markdown.

```
PUT /api/v1/collections/{collectionId}/document/{documentId}/content
Content-Type: application/json
```

**Path Parameters:**

| Parameter      | Type     | Description                  |
| -------------- | -------- | ---------------------------- |
| `collectionId` | `String` | ID of the document collection |
| `documentId`   | `String` | ID of the document            |

**Request Body:**

```json
{
  "editedContent": "<p>User-corrected <b>formatted</b> text with <a href=\"https://example.com\">links</a></p>",
  "contentFormat": "HTML"
}
```

| Field           | Type            | Required | Description                            |
| --------------- | --------------- | -------- | -------------------------------------- |
| `editedContent` | `String`        | ✅       | The user-edited content                |
| `contentFormat` | `ContentFormat` | ✅       | Format of the content: `HTML` or `MARKDOWN` |

**Response:** `200 OK`

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document content updated successfully.",
  "data": {
    "documentId": "doc-456",
    "originalFileName": "contract.pdf",
    "status": "COMPLETED",
    "errorMessage": null,
    "createdAt": "2026-02-21T12:00:00Z",
    "extractedText": "Original OCR output text...",
    "editedContent": "<p>User-corrected <b>formatted</b> text with <a href=\"https://example.com\">links</a></p>",
    "contentFormat": "HTML",
    "editedBy": "user-789",
    "editedAt": "2026-02-21T13:05:00Z",
    "aiSummary": "A legal contract between...",
    "documentType": "legal_contract",
    "aiTags": ["legal", "contract"]
  }
}
```

**Errors:**

| Code  | Condition                                       |
| ----- | ----------------------------------------------- |
| `400` | OCR not completed, or invalid/blank input       |
| `403` | User not authorized or not logged in            |
| `404` | Collection, document, or OCR data not found     |

> [!IMPORTANT]
> The original `extractedText` is **never overwritten**. Edited content is stored separately in `editedContent`.

---

## Data Models

### OcrData Entity

The core JPA entity stored in the `ocr_data` table:

| Column           | Type                | Nullable | Description                                         |
| ---------------- | ------------------- | -------- | --------------------------------------------------- |
| `id`             | `VARCHAR (UUID)`    | No       | Primary key, auto-generated                         |
| `document_id`    | `VARCHAR`           | No       | Foreign key to `file_entries`, unique                |
| `status`         | `VARCHAR (enum)`    | No       | OCR processing status                               |
| `extracted_text`  | `TEXT`             | Yes      | Raw OCR-extracted text                               |
| `edited_content`  | `TEXT`             | Yes      | User-edited content (HTML or Markdown)               |
| `content_format`  | `VARCHAR(20)`      | Yes      | Format of edited content (`HTML` / `MARKDOWN`)       |
| `edited_by`       | `VARCHAR(255)`     | Yes      | User ID of the editor                                |
| `edited_at`       | `TIMESTAMPTZ`      | Yes      | Timestamp of the last edit                           |
| `error_message`   | `VARCHAR`          | Yes      | Error details if OCR failed                          |
| `ai_summary`      | `TEXT`             | Yes      | AI-generated summary of the document                 |
| `document_type`   | `VARCHAR(50)`      | Yes      | AI-classified document type (e.g., "invoice")        |
| `ai_tags`         | `VARCHAR(500)`     | Yes      | Comma-separated AI-generated tags                    |
| `created_at`      | `TIMESTAMPTZ`      | No       | Record creation timestamp (auto)                     |
| `updated_at`      | `TIMESTAMPTZ`      | No       | Record update timestamp (auto)                       |

### Enums

#### OcrStatus

Represents the OCR processing lifecycle:

| Value        | Description                           |
| ------------ | ------------------------------------- |
| `PENDING`    | OCR queued but not yet started        |
| `PROCESSING` | OCR extraction is in progress         |
| `COMPLETED`  | OCR extraction finished successfully  |
| `FAILED`     | OCR extraction encountered an error   |

#### ContentFormat

Represents the format of user-edited content:

| Value      | Description                                        |
| ---------- | -------------------------------------------------- |
| `HTML`     | HTML content — sanitized server-side via Jsoup      |
| `MARKDOWN` | Markdown content — stored as-is, rendered by frontend |

---

## Request / Response DTOs

### DocumentCollectionResponse\<T\>

Generic wrapper for all API responses:

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Human-readable message",
  "data": { }
}
```

| Field        | Type      | Description                       |
| ------------ | --------- | --------------------------------- |
| `statusCode` | `int`     | HTTP status code                  |
| `status`     | `String`  | `"success"` or `"error"`          |
| `message`    | `String`  | Human-readable result message     |
| `data`       | `T`       | Payload (varies by endpoint)      |

### FileResultData

Returned when querying individual document OCR data:

| Field              | Type               | Description                                  |
| ------------------ | ------------------ | -------------------------------------------- |
| `documentId`       | `String`           | Unique document identifier                   |
| `originalFileName` | `String`           | Original uploaded file name                  |
| `status`           | `String`           | OCR status (`PENDING`/`PROCESSING`/`COMPLETED`/`FAILED`) |
| `errorMessage`     | `String`           | Error message if OCR failed                  |
| `createdAt`        | `OffsetDateTime`   | When the OCR record was created              |
| `extractedText`    | `String`           | Original OCR-extracted text                  |
| `editedContent`    | `String`           | User-edited content (if any)                 |
| `contentFormat`    | `String`           | Format of edited content (`HTML`/`MARKDOWN`) |
| `editedBy`         | `String`           | User ID who last edited                      |
| `editedAt`         | `OffsetDateTime`   | When the content was last edited             |
| `aiSummary`        | `String`           | AI-generated summary                         |
| `documentType`     | `String`           | AI-classified document type                  |
| `aiTags`           | `List<String>`     | AI-generated tags                            |

### CollectionResultResponse

Returned when querying all results for a collection:

| Field           | Type                   | Description                         |
| --------------- | ---------------------- | ----------------------------------- |
| `collectionId`  | `String`               | Collection ID                       |
| `overallStatus` | `DocumentStatus`       | Aggregated status of the collection |
| `files`         | `List<FileResultData>` | OCR results for each file           |

### UpdateOcrContentRequest

Request body for the content update endpoint:

| Field           | Type            | Required | Validation                       |
| --------------- | --------------- | -------- | -------------------------------- |
| `editedContent` | `String`        | ✅       | Must not be blank                |
| `contentFormat` | `ContentFormat` | ✅       | Must be `HTML` or `MARKDOWN`     |

### DocumentCollectionUploadData

Returned after bulk upload — contains the new collection ID and uploaded file metadata.

---

## Processing Pipeline

### OCR Providers

The system uses a **provider abstraction** pattern for OCR processing, with automatic provider selection based on subscription plan and credit balance:

| Provider              | Description                             | Used When                          |
| --------------------- | --------------------------------------- | ---------------------------------- |
| **Tesseract**         | Open-source OCR engine (local)          | Free plan users without credits    |
| **Google Cloud Vision** | Cloud-based OCR with superior accuracy | Paid plan users, or free users with credits |

#### Provider Selection Rules

| User Plan | Has Credits? | Provider Used    | Credits Deducted? |
| --------- | ------------ | ---------------- | ----------------- |
| Free      | No           | Tesseract        | No                |
| Free      | Not enough   | Tesseract        | No                |
| Free      | Yes          | Google Vision    | **Yes** (1 credit per document) |
| Paid      | Any          | Google Vision    | **No**            |

The `OcrProcessingService.resolveProvider(userId)` method implements this logic by checking the user's subscription via `SubscriptionFeatureService` and credit balance via `CreditBalanceService`.

If the primary provider fails and `fallbackEnabled` is `true`, the system falls back to the secondary provider via the `OcrProviderFactory`.

#### OcrRequest

Input to OCR providers:

| Field             | Type      | Description                              |
| ----------------- | --------- | ---------------------------------------- |
| `documentId`      | `String`  | Document identifier                      |
| `collectionId`    | `String`  | Collection identifier                    |
| `imageUrl`        | `String`  | URL of the file to process               |
| `mimeType`        | `String`  | MIME type of the file                    |
| `userId`          | `String`  | User ID (for provider resolution & quota)|
| `fallbackEnabled` | `boolean` | Whether fallback to secondary is enabled |
| `startPage`       | `Integer` | Start page for PDFs (1-indexed, optional)|
| `endPage`         | `Integer` | End page for PDFs (1-indexed, optional)  |
| `pages`           | `List<Integer>` | Specific pages for PDFs (optional) |

#### OcrResult

Output from OCR providers:

| Field           | Type      | Description                       |
| --------------- | --------- | --------------------------------- |
| `extractedText` | `String`  | Extracted text content            |
| `success`       | `boolean` | Whether extraction succeeded      |
| `errorMessage`  | `String`  | Error details if failed           |
| `providerType`  | `String`  | Which provider was used           |

### Processing Flow

```
1. User uploads files via POST /upload/extract-all
   │
2. Files are stored in cloud storage (S3)
   │
3. DocumentCollection + FileEntry records created
   │
4. OcrData record created per file (status: PENDING)
   │
5. OCR event published asynchronously
   │
6. ProcessOcr picks up the event
   │
   ├─ Sets OcrData status → PROCESSING
   ├─ Sends "OCR Started" push notification
   ├─ Builds OcrRequest and calls OcrProcessingService
   │   └─ resolveProvider(userId) selects provider:
   │       ├─ Paid plan → Google Vision
   │       ├─ Free + credits → Google Vision
   │       └─ Free + no credits → Tesseract
   │       (Fallback to secondary provider if primary fails)
   ├─ Updates OcrData with extracted text or error
   ├─ Deducts 1 credit (only if free plan + Google Vision used)
   ├─ Updates collection status (PROCESSED / FAILED_OCR / PROCESSING)
   ├─ Indexes in Elasticsearch (on COMPLETED)
   └─ Sends "OCR Completed" or "OCR Failed" push notification
```

#### Collection Status Resolution

The collection status is derived from individual file statuses:

| Condition                      | Collection Status |
| ------------------------------ | ----------------- |
| All files completed            | `PROCESSED`       |
| All files completed or failed  | `FAILED_OCR`      |
| Some files still pending       | `PROCESSING`      |

### Elasticsearch Integration

Successfully processed documents are indexed in Elasticsearch for full-text search:

- **On OCR completion:** Indexed with `IndexAction.CREATE`
- **On content edit:** Re-indexed with `IndexAction.UPDATE`
- **Search priority:** If `editedContent` exists, it is used as the searchable text. Otherwise, `extractedText` is used.

The Elasticsearch document includes:

| Field              | Source                                          |
| ------------------ | ----------------------------------------------- |
| `id`               | `FileEntry.documentId`                          |
| `userId`           | `DocumentCollection.user.id`                    |
| `collectionId`     | `DocumentCollection.id`                         |
| `fileName`         | `FileEntry.originalFileName`                    |
| `fileType`         | `FileEntry.fileType`                            |
| `fileSize`         | `FileEntry.fileSize`                            |
| `status`           | `DocumentCollection.collectionStatus`           |
| `ocrStatus`        | `OcrData.status`                                |
| `extractedText`    | `OcrData.editedContent ?? OcrData.extractedText` |
| `fileUrl`          | `FileEntry.fileUrl`                             |

### Notifications

Push notifications are sent to users during the OCR lifecycle:

| Event                    | Notification Type             |
| ------------------------ | ----------------------------- |
| OCR processing started   | `OCR_PROCESSING_STARTED`      |
| OCR processing completed | `OCR_PROCESSING_COMPLETED`    |
| OCR processing failed    | `OCR_PROCESSING_FAILED`       |

Each notification includes `documentId` and `collectionId` as metadata.

---

## Rich Text Editing

### Content Formats

The system supports two content formats for user-edited text:

| Format       | Server Behavior                                 | Frontend Responsibility          |
| ------------ | ----------------------------------------------- | -------------------------------- |
| **HTML**     | Sanitized via Jsoup before storage              | Render sanitized HTML            |
| **MARKDOWN** | Stored as-is (no server-side processing)        | Render Markdown and sanitize XSS |

### HTML Sanitization

HTML content is sanitized using [Jsoup](https://jsoup.org/) (v1.22.1) with a `relaxed` safelist extended with additional formatting tags.

**Allowed Tags:**

All tags in Jsoup's `Safelist.relaxed()`, plus: `span`, `div`, `br`, `hr`, `pre`, `code`, `mark`, `sub`, `sup`, `u`, `s`

**Allowed Attributes:**

| Tag    | Allowed Attributes                      |
| ------ | --------------------------------------- |
| `a`    | `href`, `title`, `target`, `rel`        |
| `span` | `style`                                 |
| `p`    | `style`                                 |
| `div`  | `style`                                 |

**Allowed URL Protocols (for `<a href>`):** `http`, `https`, `mailto`

**Stripped Content:**
- `<script>` tags and inline JavaScript
- `<iframe>`, `<object>`, `<embed>` tags
- `onclick`, `onerror`, and other event handler attributes
- `javascript:` protocol URLs

**Example:**

```
Input:  <p>Text</p><script>alert('xss')</script><b>Bold</b>
Output: <p>Text</p><b>Bold</b>
```

---

## Error Handling

All errors follow the `DocumentCollectionResponse` format:

```json
{
  "statusCode": 400,
  "status": "error",
  "message": "Human-readable error description",
  "data": null
}
```

### Common Error Scenarios

| HTTP Code | Exception              | Scenarios                                                |
| --------- | ---------------------- | -------------------------------------------------------- |
| `400`     | `BadRequestException`  | No files provided; OCR not completed (editing attempted) |
| `403`     | `ForbiddenException`   | Not logged in; user doesn't own the collection           |
| `404`     | `NotFoundException`    | Collection/document/OCR data not found                   |
| `500`     | Internal Server Error  | OCR engine failure; Elasticsearch failure                |

### Pre-conditions for Content Editing

Before content can be edited, the following must be true:

1. User must be authenticated (valid JWT)
2. User must own the document collection
3. OCR status must be `COMPLETED`
4. `editedContent` must not be blank
5. `contentFormat` must be a valid enum value (`HTML` or `MARKDOWN`)

---

## Database Schema

### Table: `ocr_data`

```sql
CREATE TABLE ocr_data (
    id              VARCHAR(255) PRIMARY KEY,
    document_id     VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(255) NOT NULL,
    extracted_text  TEXT,
    edited_content  TEXT,
    content_format  VARCHAR(20),
    edited_by       VARCHAR(255),
    edited_at       TIMESTAMP WITH TIME ZONE,
    error_message   VARCHAR(255),
    ai_summary      TEXT,
    document_type   VARCHAR(50),
    ai_tags         VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### Flyway Migrations (OCR-related)

| Version | Description                                     |
| ------- | ----------------------------------------------- |
| `V11`   | Create `ocr_data` table                         |
| `V12`   | Add cascade delete on `ocr_data` foreign key    |
| `V54`   | Add AI fields (`ai_summary`, `document_type`, `ai_tags`) |
| `V55`   | Add rich text editing fields (`edited_content`, `content_format`, `edited_by`, `edited_at`) |
