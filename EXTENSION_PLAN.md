# Resume Tailor — Extension Plan

This document outlines a phased plan to extend the Resume Tailor project. It assumes the current stack: **React (Vite)** frontend, **Java 17 Lambda** backend, **Terraform** on AWS (API Gateway, S3, SES, DynamoDB). AI is migrated from **Gemini** to **AWS Bedrock** for resume tailoring and related NLU tasks.

---

## Current State Summary

| Layer | Stack | Key Capabilities |
|-------|--------|------------------|
| **Frontend** | React, Vite, React Router, Axios | Upload PDF → paste JD → tailor → preview PDF → email; Admin dashboard (analytics) |
| **Backend** | Java 17, AWS Lambda, Gson | Tailor resume (Gemini → **Bedrock**), send email (SES), PDF extract/generate (PDFBox), S3 upload, DynamoDB analytics |
| **Infrastructure** | Terraform | API Gateway, 4 Lambdas, S3, DynamoDB, SES |

**Gaps vs. your frontend standards:** No TypeScript, no Material UI, no dedicated typed service layer; API calls live in components.

---

## Phase 0 — AI Migration: Gemini → AWS Bedrock

**Goal:** Replace Google Gemini with AWS Bedrock for all AI features so everything runs on AWS and uses a single provider.

| # | Initiative | Description | Notes |
|---|------------|-------------|--------|
| 0.1 | **Bedrock client & model** | Use AWS SDK for Java 2.x Bedrock Runtime (e.g. `InvokeModel` / `InvokeModelWithResponseStream`). Choose a model: e.g. **Claude (Anthropic)** or **Titan** on Bedrock. | Remove `com.google.genai` and `GEMINI_API_KEY`; add IAM permissions for `bedrock:InvokeModel`. |
| 0.2 | **Refactor AI service** | Replace `GeminiService` with `BedrockService` (or keep interface, swap implementation). Same inputs/outputs: `tailorResume(resumeText, jobDescription)` → tailored JSON; later `tailorCoverLetter(...)`, keyword extraction. | Preserve existing prompt design; adapt to Bedrock request/response format (e.g. message format for Claude). |
| 0.3 | **Config & infra** | Terraform: drop `GEMINI_API_KEY` from Lambda env; add Bedrock model ID and region. Optional: store model ID in SSM Parameter Store. | Lambda execution role needs `bedrock:InvokeModel` for the chosen model. |

**Deliverables:** All tailoring and AI features use Bedrock; no Gemini dependency.

---

## Phase 1 — Foundation & Standards

**Goal:** Align frontend with your standards and improve maintainability without changing behavior.

| # | Initiative | Description | Effort |
|---|------------|-------------|--------|
| 1.1 | **TypeScript migration** | Convert `frontend/src` to TypeScript (`.tsx`/`.ts`), add types for props, state, and API responses. | Medium |
| 1.2 | **Material UI (MUI)** | Introduce `@mui/material` and `@mui/icons-material`; replace custom CSS/Layout with MUI theme, `Box`, `Paper`, `Button`, `TextField`, etc. | Medium |
| 1.3 | **Typed service layer** | Add `frontend/src/services/` (or `api/`) with typed functions for `tailorResume`, `sendEmail`, `getAnalytics`; define request/response DTOs; use a single HTTP client (e.g. Axios instance with generics). | Small |
| 1.4 | **React Query (optional)** | Use TanStack Query for `GET /analytics` and optionally for tailor/send with mutations; centralize loading/error state. | Small |
| 1.5 | **Admin route protection** | Protect `/admin` and `GET /analytics`: e.g. API key in header, or AWS Cognito (or Lambda authorizer). Document in infra. | Small–Medium |

**Deliverables:** TypeScript + MUI + services in place; admin optionally behind auth; no new user-facing features yet.

---

## Phase 2 — Core Product Extensions

**Goal:** Add high-value features that fit the existing “tailor resume to job” flow.

| # | Initiative | Description | Backend | Frontend | Infra |
|---|------------|-------------|---------|----------|--------|
| 2.1 | **Cover letter generation** | Same flow: user gets a tailored cover letter (text or PDF) from master resume + JD. | New Lambda handler + `BedrockService.tailorCoverLetter()`; reuse PDF or plain text response. | New step or toggle: “Include cover letter”; preview/download. | New route `POST /tailorCoverLetter` (or extend tailor response). |
| 2.2 | **Job description parser / checklist** | Parse JD with Bedrock to extract key skills, requirements, nice-to-haves; show as checklist or “match” view. | New handler or extend tailor: return `{ tailoredResume, extractedRequirements }`; or dedicated `POST /parseJobDescription`. | New component: “Key requirements” list or ATS-style checklist. | Optional new route. |
| 2.3 | **ATS fit score** | Pre- and/or post-tailor “ATS compatibility” score (e.g. 0–100) based on keyword overlap. | New service (e.g. `ATSScoreService`) using Bedrock or rule-based keyword match; return score in tailor response or separate endpoint. | Show score in UI before/after tailor; optional simple gauge or progress bar. | None if in existing tailor Lambda. |

**Suggested order:** 2.1 (cover letter) → 2.2 (parser) or 2.3 (score).

---

## Phase 3 — User Accounts & One Master Resume per User

**Goal:** Identify users and store exactly one master resume per user with full CRUD.

| # | Initiative | Description | Backend | Frontend | Infra |
|---|------------|-------------|---------|----------|--------|
| 3.1 | **User accounts (Cognito)** | Sign-up/sign-in so every action is scoped to a user. | Cognito User Pool; JWT validation in Lambda authorizer; pass `user_id` (sub) to Lambdas. | Login/signup; protected routes; user context. | Cognito User Pool, authorizer, app client. |
| 3.2 | **One master resume per user** | Each user has at most one master resume. Add, update, or delete it. | APIs: `GET /masterResume` (metadata + presigned download), `PUT /masterResume` (upload PDF), `DELETE /masterResume`. Store file in S3 (`users/{user_id}/master.pdf`); DynamoDB item: `user_id` (PK), `s3_key`, `updated_at`. Enforce one resume: overwrite on PUT, delete S3 object on DELETE. | “My master resume”: upload/replace, preview, delete; show “No master resume” state. | S3 paths per user; DynamoDB table (e.g. `user_master_resume`) or GSI. |
| 3.3 | **Save tailored PDF to S3 + link** | After tailoring, optionally “Save” to S3 with a short-lived signed URL; store mapping in DynamoDB. | New or extended Lambda: generate presigned URL; store key keyed by user/session. | “Save” button → show link or “Copy link” for 24h. | S3 lifecycle or existing bucket. |

**Order:** 3.1 (auth) first, then 3.2 (master resume CRUD); 3.3 optional.

---

## Phase 4 — Reliability & Scale

**Goal:** Harden production and support higher or programmatic use.

| # | Initiative | Description | Notes |
|---|------------|-------------|--------|
| 4.1 | **Input validation** | Validate PDF size/type, JD length, email format in Lambda; return 400 with clear messages. | Reduces bad requests and improves security. |
| 4.2 | **Structured logging & errors** | JSON logging in Lambda; correlation id; consistent error response shape. | Easier debugging and CloudWatch Insights. |
| 4.3 | **Retry / idempotency** | Retry Bedrock or S3 on transient failures; optional idempotency key for tailor to avoid duplicate charges. | Backend-only. |
| 4.4 | **API key for programmatic access** | Optional header `X-API-Key` for power users; track usage per key. | API Gateway usage plan or custom authorizer. |

---

## Phase 5 — Testing & Quality

| # | Initiative | Description |
|---|------------|-------------|
| 5.1 | **Backend unit tests** | JUnit 5 tests for `BedrockService` (mocked client), `PdfService`, `AnalyticsService`, and handlers with mock context. |
| 5.2 | **Frontend tests** | React Testing Library for main flows (upload, tailor, preview, master resume CRUD); mock service layer. |
| 5.3 | **E2E (optional)** | Playwright or Cypress against local or staging API. |

---

## Suggested Roadmap

| Priority | Phase | Focus |
|----------|--------|--------|
| **P0** | 0 | AI migration: Gemini → AWS Bedrock. |
| **P1** | 1 | Foundation: TypeScript, MUI, service layer, optional admin auth. |
| **P2** | 2.1 | Cover letter generation. |
| **P3** | 2.2 or 2.3 | JD parser or ATS score (pick one first). |
| **P4** | 3.1, 3.2 | User accounts (Cognito) + one master resume per user (add/update/delete). |
| **P5** | 4.x | Input validation, structured logging, retry, optional API key. |
| **P6** | 5.1, 5.2 | Unit tests for backend and frontend. |

---

## Quick Reference: Adding a New Lambda + Route

1. **Backend:** New handler in `com.resumetailor.handler`; add to shadow JAR (same JAR).
2. **Terraform:** New `module "lambda_*"` in `infrastructure/main.tf`; add integration + route in `modules/networking/main.tf`; pass new invoke ARN/name from root to networking module.
3. **Frontend:** New typed service method; call from component or hook.

---

## Document Info

- **Created:** Extension plan for Resume Tailor.
- **Scope:** Frontend (React), Backend (Java Lambda), Infrastructure (Terraform/AWS).
- **Next step:** Run Phase 0 (Bedrock migration) first so all AI features use AWS; then Phase 1 (foundation), then Phase 3 (auth + master resume). Optionally break Phase 1 into tickets (TypeScript → MUI → services → React Query → admin auth).
