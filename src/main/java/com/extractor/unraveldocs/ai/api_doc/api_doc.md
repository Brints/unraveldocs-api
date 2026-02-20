# AI Operations API Documentation

## Base URL

| Audience | Base Path      |
|----------|----------------|
| User     | `/api/v1/ai`   |

> All endpoints require authentication via `Authorization: Bearer <token>`.

---

## Endpoints

### 1. Summarize Document Text
**`POST /api/v1/ai/summarize`**

Generates an AI-powered summary of a document's OCR-extracted text. Supports short (1-2 sentences) and detailed (bullet-pointed) summaries.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "summaryType": "SHORT",
  "modelPreference": "openai"
}
```

| Field             | Type   | Required | Notes                                                      |
|-------------------|--------|----------|------------------------------------------------------------|
| `documentId`      | string | ✅        | UUID of the document with completed OCR                    |
| `summaryType`     | string | ❌        | `SHORT` (default) or `DETAILED`                            |
| `modelPreference` | string | ❌        | `openai` or `mistral`. Defaults to configured provider     |

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document summarized successfully.",
  "data": {
    "documentId": "22cc6b55-be2f-4b27-a56e-54041519e5c8",
    "summary": "The document is a job posting for a Senior Backend Engineer (Java) position at Rubik's Technologies, outlining key responsibilities such as software development, code reviews, and collaboration with stakeholders, and requiring 3+ years of experience in agile backend development, proficiency in Java and related technologies, and fluency in German and English. The company emphasizes equal opportunities and diversity.",
    "summaryType": "SHORT",
    "modelUsed": "Mistral Small",
    "creditsCharged": 1,
    "billingSource": "credits"
  }
}
```

**Detailed Summary Response Example:**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document summarized successfully.",
  "data": {
    "documentId": "22cc6b55-be2f-4b27-a56e-54041519e5c8",
    "summary": "**Overview:**\nThe document is a job posting for a Senior Backend Engineer (Java) position at Rubik's Technologies, shared by Samia Kousar, an HR Ops Coordinator.\n\n**Key Points:**\n- **Job Title:** Senior Backend Engineer (Java)\n- **Job Duties:**\n  - Improve digital sales platforms and contribute new ideas as part of an agile software team.\n  - Manage delivery processes, code reviews, refactoring, and deployments using CI/CD.\n  - Oversee the full software development lifecycle, providing solutions for risks, code quality, and process optimization.\n  - Collaborate with Product Owners and advise stakeholders on technical topics.\n  - Contribute to IT security, DevOps, and service optimization and stabilization.\n  - Help define test strategies and implement IT security measures.\n- **Required Profile:**\n  - 3+ years of experience in agile backend software development (Scrum/Kanban).\n  - Proficient in Java, Spring Boot, JPA (Hibernate), REST & OpenAPI, and AWS.\n  - Experienced with CI/CD, Git, GitLab CI, Docker, Kubernetes, and PostgreSQL.\n  - Familiar with architecture principles, microservices, and test automation in microservice environments.\n  - Knowledge of implementing security measures such as overload protection and OWASP Top 10.\n  - Fluent in German and proficient in English, both spoken and written.\n- **Company Values:**\n  - Promote equal opportunities, diversity, and inclusion.\n  - Value every application regardless of gender, nationality, ethnic or social origin, religion, belief, disability, age, or sexual orientation.\n\n**Notable Details:**\n- The job posting was edited and shared 1 day ago.\n- The position is hiring on behalf of a colleague.\n- The company values and promotes diversity and inclusion.",
    "summaryType": "DETAILED",
    "modelUsed": "Mistral Small",
    "creditsCharged": 2,
    "billingSource": "credits"
  }
}
```

**Error Responses:**

| Code | Scenario                                                    |
|------|-------------------------------------------------------------|
| 400  | OCR not complete, no extracted text, or insufficient quota  |
| 403  | User not authenticated                                      |
| 404  | Document or OCR data not found                              |

---

### 2. Classify Document
**`POST /api/v1/ai/classify/{documentId}`**

Classifies a document into a type and generates descriptive keyword tags from the OCR-extracted text.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

| Param        | Type   | Required | Notes                                   |
|--------------|--------|----------|-----------------------------------------|
| `documentId` | string | ✅        | UUID of the document with completed OCR |

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Document classified successfully.",
  "data": {
    "documentId": "22cc6b55-be2f-4b27-a56e-54041519e5c8",
    "documentType": "other",
    "tags": [
      "job",
      "recruitment",
      "technology",
      "backend",
      "engineer"
    ],
    "confidence": 0.95,
    "modelUsed": "GPT-4o-mini",
    "creditsCharged": 1,
    "billingSource": "credits"
  }
}
```

**Supported Document Types:**

| Type           | Description                        |
|----------------|------------------------------------|
| `invoice`      | Bills, invoices, payment requests  |
| `receipt`      | Purchase receipts, payment proofs  |
| `contract`     | Legal agreements, employment terms |
| `letter`       | Formal or informal correspondence  |
| `id_document`  | IDs, passports, driver's licenses  |
| `medical`      | Medical records, prescriptions     |
| `legal`        | Court documents, legal filings     |
| `academic`     | Papers, transcripts, certificates  |
| `report`       | Business or technical reports      |
| `form`         | Applications, registration forms   |
| `other`        | Unclassified documents             |

**Error Responses:**

| Code | Scenario                                                    |
|------|-------------------------------------------------------------|
| 400  | OCR not complete, no extracted text, or insufficient quota  |
| 403  | User not authenticated                                      |
| 404  | Document or OCR data not found                              |

---

## Billing Model

AI operations use a **hybrid billing model**: subscription allowance is consumed first, then credits act as overflow.

### Billing Priority

```
1. Subscription monthly allowance  →  consumed first (free with plan)
2. Purchased credit balance        →  overflow if allowance exhausted
3. Denied                          →  if neither is available
```

### Per-Tier Monthly AI Allowances

| Tier     | Monthly Allowance | Model Access                                 |
|----------|-------------------|----------------------------------------------|
| Free     | 5 operations      | GPT-4o-mini / Mistral Small only             |
| Starter  | 50 operations     | GPT-4o-mini / Mistral Small                  |
| Pro      | 200 operations    | All models (including GPT-4o, Mistral Large) |
| Business | 500 operations    | All models + priority queue                  |

Allowances reset on the 1st of each month at midnight UTC alongside other monthly quotas.

### Credit Costs Per Operation

| Operation          | Credit Cost |
|--------------------|-------------|
| Short Summary      | 1 credit    |
| Detailed Summary   | 2 credits   |
| Document Classify  | 1 credit    |

> **Note:** When billed against the subscription allowance, `creditsCharged` will be `0` and `billingSource` will be `"subscription"`. When credits are used, `creditsCharged` reflects the actual cost and `billingSource` will be `"credits"`.

### Billing Example Scenarios

| Scenario                           | Allowance | Credits | Result                   |
|------------------------------------|-----------|---------|--------------------------|
| Starter user, 10/50 ops used       | ✅ 40 left | —       | Billed to subscription   |
| Starter user, 50/50 ops used       | ❌ 0 left  | 45 bal  | Billed 1 credit          |
| Free user, 5/5 ops used, 0 credits | ❌ 0 left  | ❌ 0 bal | **Denied** — buy credits |

---

## AI Model Providers

| Provider   | Default Model | Role     | Notes                              |
|------------|---------------|----------|------------------------------------|
| OpenAI     | GPT-4o-mini   | Primary  | Best cost/quality balance          |
| Mistral AI | Mistral Small | Fallback | Activates if OpenAI is unavailable |

If the primary model fails, the system automatically retries with the fallback provider. The `modelUsed` field in responses indicates which model ultimately processed the request.

---

## Response Format

All responses follow the standard `UnravelDocsResponse<T>` format:

```json
{
  "statusCode": 200,
  "status": "success",
  "message": "Human-readable message",
  "data": {}
}
```

Error responses:
```json
{
  "statusCode": 400,
  "status": "error",
  "message": "You have used all your AI operations for this month and have insufficient credits. Please purchase more credits or upgrade your subscription plan.",
  "data": null
}
```

---

## Configuration Reference

All AI settings are configurable via `application.properties` or environment variables.

| Property                                        | Default                | Description                     |
|-------------------------------------------------|------------------------|---------------------------------|
| `ai.default-provider`                           | `OPENAI`               | Primary model provider          |
| `ai.fallback-provider`                          | `MISTRAL_AI`           | Fallback model provider         |
| `ai.fallback-enabled`                           | `true`                 | Enable automatic fallback       |
| `ai.timeout-seconds`                            | `60`                   | API call timeout                |
| `ai.max-retries`                                | `2`                    | Max retry attempts              |
| `ai.max-input-length`                           | `30000`                | Max characters sent to AI model |
| `ai.quota.enabled`                              | `true`                 | Enable billing enforcement      |
| `ai.summarization.short-summary-credit-cost`    | `1`                    | Credits for short summary       |
| `ai.summarization.detailed-summary-credit-cost` | `2`                    | Credits for detailed summary    |
| `ai.classification.classification-credit-cost`  | `1`                    | Credits for classification      |
| `spring.ai.openai.chat.options.model`           | `gpt-4o-mini`          | OpenAI model name               |
| `spring.ai.openai.chat.options.temperature`     | `0.3`                  | OpenAI sampling temperature     |
| `spring.ai.mistralai.chat.options.model`        | `mistral-small-latest` | Mistral model name              |
| `spring.ai.mistralai.chat.options.temperature`  | `0.3`                  | Mistral sampling temperature    |
