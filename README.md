# Resume Tailor

Resume Tailor is a full-stack application that customizes resumes for a job description, generates a tailored PDF (and optional cover letter), tracks usage analytics, and emails the generated documents to a user.

The project consists of:
- A Java 17 AWS Lambda backend (`backend`) that integrates with AWS Bedrock, S3, SES, and DynamoDB.
- A React + TypeScript + Vite frontend (`frontend`) built with Material UI and React Router.

## Features

- Upload a master resume PDF and paste a job description.
- Generate a tailored resume PDF using Amazon Bedrock.
- Optionally generate a tailored cover letter PDF.
- Compute an ATS-fit score and extract top requirements from the job description.
- Email generated documents through Amazon SES.
- Track usage metrics in DynamoDB.
- Manage a user-scoped master resume (upload/get/delete) stored in S3 with metadata in DynamoDB.
- View basic analytics in an admin dashboard.

## Repository Structure

```text
Resume Tailor/
  backend/                     # Java AWS Lambda backend
    src/main/java/com/resumetailor/
      config/                  # Environment config
      handler/                 # Lambda handlers
      model/                   # Request/response records
      service/                 # AWS + PDF + AI services
  frontend/                    # React + TypeScript client
    src/
      components/              # Shared UI layout
      pages/                   # Route pages
      services/                # Typed API service layer
      types/                   # Shared API types
      test/                    # Vitest setup and tests
```

## Architecture Overview

### Backend (`backend`)

The backend is implemented as AWS Lambda handlers using the AWS Java SDK v2.

Main handlers:
- `TailorResumeHandler`: accepts resume + job description, saves original PDF to S3, extracts text, calls Bedrock for tailoring/scoring/requirements extraction, returns generated PDFs and metadata.
- `SendEmailHandler`: sends generated files as email attachments via SES.
- `AnalyticsHandler`: records an analytics event (`metricType`) to DynamoDB.
- `GetAnalyticsHandler`: returns counts for `resume_tailored` and `website_visit`.
- `MasterResumeHandler`: PUT/GET/DELETE for per-user master resume using `x-user-id` header.

Core services:
- `BedrockService`: prompt-based resume/cover-letter tailoring, ATS scoring, and requirement extraction.
- `PdfService` / `ResumePdfGenerator`: PDF text extraction and generation.
- `S3Service`: PDF object upload/retrieve/delete.
- `EmailService`: SES raw-email with PDF attachments.
- `AnalyticsService`: writes and queries metrics in DynamoDB.
- `MasterResumeMetadataService`: maintains user-to-S3 key mapping in DynamoDB.

### Frontend (`frontend`)

The frontend is a React SPA with typed service calls and page-level routes:
- `/` -> Tailor form and result preview/download workflow.
- `/master-resume` -> Upload/get/delete user master resume.
- `/admin` -> Analytics dashboard.

Key technologies:
- React 18, TypeScript, Vite
- Material UI
- React Router
- Axios (typed client)
- Vitest + Testing Library

## Prerequisites

- Java 17 (JDK)
- Node.js 18+ and npm
- AWS account with permissions for:
  - Bedrock Runtime (`bedrock:InvokeModel`)
  - S3 (put/get/delete on configured bucket)
  - SES (`ses:SendRawEmail`)
  - DynamoDB (put/get/query/delete on configured tables)
- AWS credentials available to runtime (environment variables, profile, or IAM role)

## Environment Variables

Set these for backend runtime (local/dev/deployed Lambda):

- `SES_SOURCE_EMAIL` (required): verified SES sender email.
- `S3_BUCKET_NAME` (required): bucket for resumes.
- `AWS_REGION` (optional, default `us-east-1`): primary AWS region.
- `BEDROCK_REGION` (optional, defaults to `AWS_REGION`): Bedrock runtime region.
- `BEDROCK_MODEL_ID` (optional, default `anthropic.claude-3-haiku-20240307-v1:0`): Bedrock model.
- `DYNAMODB_TABLE_NAME` (required by analytics service): analytics events table.
- `MASTER_RESUME_TABLE_NAME` (optional, default `resume-tailor-master-resume`): user->resume metadata table.

Frontend:
- `VITE_API_URL` (optional): base URL for backend API. Defaults to `http://localhost:8080`.

## Local Development

### 1) Backend setup

From `backend/`:

```bash
./gradlew clean build
```

Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

Build output:
- Shadow/fat jar is produced by `shadowJar` with classifier `aws`.

Run backend locally:
- The project currently contains Lambda handlers. To run locally, expose handlers through your chosen local AWS/Lambda emulation or API Gateway simulation setup.
- Ensure all required environment variables are present before invoking handlers.

### 2) Frontend setup

From `frontend/`:

```bash
npm install
npm run dev
```

Additional scripts:
- `npm run build` -> production build
- `npm run preview` -> preview production build
- `npm run lint` -> eslint
- `npm run typecheck` -> TypeScript check
- `npm run test` -> Vitest test run

## API Endpoints

The frontend calls these backend routes:

- `POST /tailorResume`
  - Request:
    - `resumePdfBase64: string`
    - `jobDescription: string`
    - `includeCoverLetter?: boolean`
  - Response includes:
    - `result.pdfBase64`
    - `result.coverLetterPdfBase64` (optional)
    - `result.atsScore` (0-100)
    - `result.extractedRequirements` (string[])
    - `storageKey` (S3 key for original upload)

- `POST /sendEmail`
  - Request:
    - `email: string`
    - `pdfBase64?: string`
    - `coverLetterPdfBase64?: string`
    - `jobUrl?: string`

- `POST /track`
  - Request:
    - `metricType: string`

- `GET /analytics`
  - Response:
    - `resume_tailored: number`
    - `website_visit: number`

- `GET /masterResume`
  - Header:
    - `x-user-id: string`
  - Response:
    - `s3Key: string`
    - `resumePdfBase64: string`

- `PUT /masterResume`
  - Header:
    - `x-user-id: string`
  - Request:
    - `resumePdfBase64: string`

- `DELETE /masterResume`
  - Header:
    - `x-user-id: string`

## Data Model Notes

- Resume and cover letter content are transported as Base64-encoded PDF payloads.
- Analytics table expects partition key design compatible with query:
  - `metric_type` as partition key
  - `timestamp` as sort key (numeric)
- Master resume metadata table expects:
  - `user_id` as key
  - `s3_key` and `updated_at` attributes

## Testing

### Backend

From `backend/`:

```bash
./gradlew test
```

### Frontend

From `frontend/`:

```bash
npm run test
```

## Troubleshooting

- `Required environment variable ... is not set`:
  - Confirm backend env vars are exported in the process running handlers.
- SES send failures:
  - Verify sender identity and SES sandbox/production status.
- Bedrock invocation issues:
  - Check model access in target region and IAM permissions.
- Empty analytics counts:
  - Validate DynamoDB table keys and that `metricType` values are being tracked.
- CORS/API errors in frontend:
  - Confirm backend routes are reachable at `VITE_API_URL` and returning JSON.

## Security & Operational Notes

- Do not commit secrets or AWS credentials.
- Keep sensitive values in environment variables or secure secret management.
- Restrict IAM policies to minimum required actions and resources.
- Consider adding request validation/rate limiting/authentication before production use.

## Current Status

- Frontend and backend are actively being developed.
- Root documentation now reflects the current Lambda handler and React routing/service structure in this repository.
