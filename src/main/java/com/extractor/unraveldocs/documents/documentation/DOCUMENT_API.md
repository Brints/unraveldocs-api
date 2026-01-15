# UnravelDocs API - Document Management

API endpoints for managing documents in UnravelDocs.

## Base URL

The base URL for all API endpoints is:
`http://localhost:8080/api/v1`

(This is derived from `app.base.url` in your `application.properties` and the `@RequestMapping("/api/v1/documents")` on the controller).

## Authentication

All endpoints under `/api/v1/documents/` require authentication.
The API uses **Bearer Token (JWT)** authentication.
You must include an `Authorization` header with your JWT token:

`Authorization: Bearer <your_jwt_token>`

---

## Endpoints

### 1. Upload Documents

Allows authenticated users to upload multiple documents as a collection.

*   **Method:** `POST`
*   **Endpoint:** `/documents/upload`
*   **Authentication:** Required (Bearer Token)
*   **Content-Type:** `multipart/form-data`

**Request Parameters:**

| Parameter          | Type    | Required | Description                                                     |
|--------------------|---------|----------|-----------------------------------------------------------------|
| `files`            | file[]  | Yes      | The document files to upload                                    |
| `collectionName`   | string  | No       | Custom name for the collection (auto-generated if not provided) |
| `enableEncryption` | boolean | No       | Enable AES-256 encryption (premium feature, default: false)     |

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/documents/upload" \
  -H "Authorization: Bearer <token>" \
  -F "files=@document1.pdf" \
  -F "files=@document2.png" \
  -F "collectionName=My Documents" \
  -F "enableEncryption=false"
```

**Response (200 OK):**
```json
{
  "data": {
    "collectionId": "b8254421-c1ee-4db7-91d9-db8117892465",
    "files": [
      {
        "documentId": "f626d95b-3535-4954-9e0b-4946c54e5817",
        "originalFileName": "docs2.jpeg",
        "displayName": null,
        "fileSize": 180086,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/0f3df40b-89b1-43b8-a38c-f34b879233d9-docs2.jpeg",
        "status": "success",
        "encrypted": false
      },
      {
        "documentId": "5511b2fc-b4b8-4037-91e3-fce723d078f9",
        "originalFileName": "docs1.jpeg",
        "displayName": null,
        "fileSize": 87367,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/461b1766-f32a-4725-a949-b1c95f0ffd45-docs1.jpeg",
        "status": "success",
        "encrypted": false
      }
    ],
    "overallStatus": "completed"
  },
  "message": "All 2 document(s) uploaded successfully.",
  "status": "success",
  "statusCode": 200
}
```

---

### 2. Get All Collections

Retrieves all document collections for the authenticated user.

*   **Method:** `GET`
*   **Endpoint:** `/documents/my-collections`
*   **Authentication:** Required (Bearer Token)

**Response (200 OK):**
```json
{
  "data": [
    {
      "collectionStatus": "completed",
      "createdAt": "2026-01-13T16:18:37.181629Z",
      "fileCount": 2,
      "hasEncryptedFiles": false,
      "id": "b8254421-c1ee-4db7-91d9-db8117892465",
      "name": "Mandaline",
      "updatedAt": "2026-01-13T16:18:37.181629Z",
      "uploadTimestamp": "2026-01-13T16:18:37.152895Z"
    },
    {
      "collectionStatus": "processed",
      "createdAt": "2026-01-11T15:13:55.437249Z",
      "fileCount": 1,
      "hasEncryptedFiles": false,
      "id": "5af125c9-9d5a-4c98-a8ae-647531183c51",
      "name": "Collection-5af125c9",
      "updatedAt": "2026-01-11T15:14:06.803058Z",
      "uploadTimestamp": "2026-01-11T15:13:55.414684Z"
    },
    {
      "collectionStatus": "completed",
      "createdAt": "2026-01-11T02:35:47.918967Z",
      "fileCount": 2,
      "hasEncryptedFiles": false,
      "id": "a7dfb6a3-3d92-49b3-ae41-ec9e5dbcbb72",
      "name": "Collection-a7dfb6a3",
      "updatedAt": "2026-01-11T02:35:47.918967Z",
      "uploadTimestamp": "2026-01-11T02:35:47.864689Z"
    },
    {
      "collectionStatus": "completed",
      "createdAt": "2026-01-11T02:23:42.309359Z",
      "fileCount": 1,
      "hasEncryptedFiles": false,
      "id": "da95a5c2-c506-42ec-894a-c6cc1302e1cd",
      "name": "Collection-da95a5c2",
      "updatedAt": "2026-01-11T02:23:42.309359Z",
      "uploadTimestamp": "2026-01-11T02:23:42.291068Z"
    }
  ],
  "message": "Document collections retrieved successfully.",
  "status": "success",
  "statusCode": 200
}
```

---

### 3. Get Collection by ID

Retrieves a specific document collection with all its files.

*   **Method:** `GET`
*   **Endpoint:** `/documents/collection/{collectionId}`
*   **Authentication:** Required (Bearer Token)

**Response (200 OK):**
```json
{
  "data": {
    "collectionStatus": "completed",
    "createdAt": "2026-01-13T16:18:37.181629Z",
    "files": [
      {
        "documentId": "f626d95b-3535-4954-9e0b-4946c54e5817",
        "originalFileName": "docs2.jpeg",
        "displayName": null,
        "fileSize": 180086,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/0f3df40b-89b1-43b8-a38c-f34b879233d9-docs2.jpeg",
        "status": "success",
        "encrypted": false
      },
      {
        "documentId": "5511b2fc-b4b8-4037-91e3-fce723d078f9",
        "originalFileName": "docs1.jpeg",
        "displayName": null,
        "fileSize": 87367,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/461b1766-f32a-4725-a949-b1c95f0ffd45-docs1.jpeg",
        "status": "success",
        "encrypted": false
      }
    ],
    "id": "b8254421-c1ee-4db7-91d9-db8117892465",
    "name": "Mandaline",
    "updatedAt": "2026-01-13T16:18:37.181629Z",
    "uploadTimestamp": "2026-01-13T16:18:37.152895Z",
    "userId": "d28feb5b-4fc9-4653-aae4-285ce0a70975"
  },
  "message": "Document collection retrieved successfully.",
  "status": "success",
  "statusCode": 200
}
```

---

### 4. Get File from Collection

Retrieves a specific file from a collection.

*   **Method:** `GET`
*   **Endpoint:** `/documents/collection/{collectionId}/document/{documentId}`
*   **Authentication:** Required (Bearer Token)

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "File retrieved successfully.",
  "data": {
    "documentId": "uuid-doc-1",
    "originalFileName": "document1.pdf",
    "displayName": "My Custom Name",
    "fileSize": 1024768,
    "fileUrl": "https://s3.amazonaws.com/bucket/documents/uuid-doc-1.pdf",
    "status": "SUCCESS",
    "isEncrypted": false
  }
}
```

---

### 5. Move Document Between Collections (Premium)

Moves a document from one collection to another. **Requires Starter+ subscription.**

*   **Method:** `POST`
*   **Endpoint:** `/documents/move`
*   **Authentication:** Required (Bearer Token)

**Request Body:**
```json
{
  "sourceCollectionId": "uuid-source-collection",
  "targetCollectionId": "uuid-target-collection",
  "documentId": "uuid-document-id"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "documentId": "f626d95b-3535-4954-9e0b-4946c54e5817",
    "originalFileName": "docs2.jpeg",
    "displayName": null,
    "fileSize": 180086,
    "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/0f3df40b-89b1-43b8-a38c-f34b879233d9-docs2.jpeg",
    "status": "success",
    "encrypted": false
  },
  "message": "Document moved successfully to collection: Collection-5af125c9",
  "status": "success",
  "statusCode": 200
}
```

**Error (403 Forbidden - Free tier):**
```json
{
  "statusCode": 403,
  "error": "Forbidden",
  "message": "This feature requires a Starter or higher subscription. Please upgrade your plan to access document move."
}
```

---

### 6. Update Collection Name

Updates the name of a document collection.

*   **Method:** `PUT`
*   **Endpoint:** `/documents/collection/{collectionId}`
*   **Authentication:** Required (Bearer Token)

**Request Body:**
```json
{
  "name": "New Collection Name"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "collectionStatus": "processed",
    "createdAt": "2026-01-11T15:13:55.437249Z",
    "files": [
      {
        "documentId": "c4d1e9e1-e3e8-4440-8e7a-8748fdbc17c4",
        "originalFileName": "docs1.jpeg",
        "displayName": null,
        "fileSize": 87367,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/8912bdb4-24b0-41fe-aaad-c47c915e9aa3-docs1.jpeg",
        "status": "success",
        "encrypted": false
      },
      {
        "documentId": "f626d95b-3535-4954-9e0b-4946c54e5817",
        "originalFileName": "docs2.jpeg",
        "displayName": null,
        "fileSize": 180086,
        "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/0f3df40b-89b1-43b8-a38c-f34b879233d9-docs2.jpeg",
        "status": "success",
        "encrypted": false
      }
    ],
    "id": "5af125c9-9d5a-4c98-a8ae-647531183c51",
    "name": "Octopus",
    "updatedAt": "2026-01-13T16:40:47.548812Z",
    "uploadTimestamp": "2026-01-11T15:13:55.414684Z",
    "userId": "d28feb5b-4fc9-4653-aae4-285ce0a70975"
  },
  "message": "Collection name updated successfully",
  "status": "success",
  "statusCode": 200
}
```

**Error (409 Conflict):**
```json
{
  "statusCode": 409,
  "error": "Conflict",
  "message": "A collection with this name already exists"
}
```

---

### 7. Update Document Display Name

Updates the display name of a document within a collection.

*   **Method:** `PUT`
*   **Endpoint:** `/documents/collection/{collectionId}/document/{documentId}`
*   **Authentication:** Required (Bearer Token)

**Request Body:**
```json
{
  "displayName": "Custom Document Name"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "documentId": "f626d95b-3535-4954-9e0b-4946c54e5817",
    "originalFileName": "docs2.jpeg",
    "displayName": "Custom Document Name",
    "fileSize": 180086,
    "fileUrl": "https://unraveldocs-s3.s3.eu-central-1.amazonaws.com/documents/0f3df40b-89b1-43b8-a38c-f34b879233d9-docs2.jpeg",
    "status": "success",
    "encrypted": false
  },
  "message": "Document name updated successfully",
  "status": "success",
  "statusCode": 200
}
```

---

### 8. Delete Collection

Deletes a document collection and all its files.

*   **Method:** `DELETE`
*   **Endpoint:** `/documents/collection/{collectionId}`
*   **Authentication:** Required (Bearer Token)

**Response (204 No Content):** Success, no body returned.

---

### 9. Delete File from Collection

Deletes a specific file from a collection.

*   **Method:** `DELETE`
*   **Endpoint:** `/documents/collection/{collectionId}/document/{documentId}`
*   **Authentication:** Required (Bearer Token)

**Response (204 No Content):** Success, no body returned.

---

### 10. Clear All Collections

Deletes all document collections for the authenticated user.

*   **Method:** `DELETE`
*   **Endpoint:** `/documents/clear-all`
*   **Authentication:** Required (Bearer Token)

**Response (204 No Content):** Success, no body returned.

---

## Schemas

### DocumentCollectionSummary
```json
{
  "id": "string (uuid)",
  "name": "string",
  "collectionStatus": "COMPLETED | PARTIALLY_COMPLETED | FAILED_UPLOAD | PENDING",
  "fileCount": "integer",
  "hasEncryptedFiles": "boolean",
  "uploadTimestamp": "string (ISO 8601)",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### FileEntryData
```json
{
  "documentId": "string (uuid)",
  "originalFileName": "string",
  "displayName": "string (nullable)",
  "fileSize": "integer (int64)",
  "fileUrl": "string (url)",
  "status": "SUCCESS | FAILED_VALIDATION | FAILED_STORAGE_UPLOAD",
  "isEncrypted": "boolean"
}
```

### MoveDocumentRequest
```json
{
  "sourceCollectionId": "string (uuid, required)",
  "targetCollectionId": "string (uuid, required)",
  "documentId": "string (uuid, required)"
}
```

### UpdateCollectionRequest
```json
{
  "name": "string (required, max 255 chars)"
}
```

### UpdateDocumentRequest
```json
{
  "displayName": "string (required, max 255 chars)"
}
```

### ErrorResponse
```json
{
  "statusCode": "integer",
  "error": "string (Bad Request, Unauthorized, Forbidden, Not Found, Conflict)",
  "message": "string",
  "timestamp": "string (ISO 8601)",
  "path": "string (request path)"
}
```

---

## Premium Features

The following features require a **Starter or higher** subscription:

| Feature       | Endpoint                                              | Description                        |
|---------------|-------------------------------------------------------|------------------------------------|
| Document Move | `POST /documents/move`                                | Move documents between collections |
| Encryption    | `POST /documents/upload` with `enableEncryption=true` | AES-256-GCM encryption             |

Free tier users attempting to access these features will receive a `403 Forbidden` response with an upgrade prompt.

---

## Encryption

Documents can be encrypted with **AES-256-GCM** (bank-level encryption) when uploading.

**Requirements:**
- Starter+ subscription
- `enableEncryption=true` parameter on upload
- Server must have `ENCRYPTION_MASTER_KEY` environment variable configured

**Key Generation:**
```bash
openssl rand -base64 32
```

Encrypted files are stored with `isEncrypted: true` in their metadata.
