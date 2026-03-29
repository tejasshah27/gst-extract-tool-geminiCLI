# GST Extract Tool - Phase 1 Implementation Plan

## Task 1: Backend Core (Phase 1)
**Goal:** Endpoint live; GSTR-2A/2B B2B parsed and validated; result saved on server and returned as stream.

**Implementation Details:**
- Initialize Spring Boot 4.0.0 Maven project in `backend/` with Java 21, POI 5.5.1.
- Configure `application.properties`, `logback.xml`, `config.yml`.
- Create Enums & Records (`ReturnType`, `SourceType`, `MatchStatus`, `ValidationError`, `MatchResult`, `Invoice`, `InwardInvoice`, `SummaryReturn`).
- Create configuration classes (`AppConfig`, `CorsConfig`).
- Implement Parsers: `ParseUtils`, `SubTableParser`, `Gstr2aParser`.
- Implement Validators: `GstinValidator`, `InvoiceValidator`, `TaxAmountValidator`.
- Implement Exporter: `XlsxExporter` (Invoices + Errors sheets only for now).
- Implement Storage: `OutputStorageService`.
- Implement Orchestrator: `ProcessOrchestrator`.
- Implement API: `ProcessController` (POST /api/process multipart).
- Create Main Application class.
- Include unit tests: `GstinValidatorTest`, `ParseUtilsTest`, `Gstr2aParserTest`, `TaxAmountValidatorTest`, `ProcessControllerTest`.

## Task 2: Frontend Basic UI (Phase 1)
**Goal:** Angular UI to upload GSTR-2A XLSX, submit, and download result.xlsx.

**Implementation Details:**
- Scaffold Angular CLI project in `frontend/` (`ng new gst-extract-ui --standalone --skip-tests=false --style=css --routing=false`).
- Update `process.service.ts` to POST file to backend (`responseType: 'blob'`).
- Update `app.component.ts`, `app.component.html`, `app.component.css` for file upload UI.
- Handle success (trigger file download) and error (display message).

## Task 3: Integration & Verification (Main Agent)
- Start backend server and frontend dev server.
- Run end-to-end testing (manual verification using curl or browser simulation if possible).
- Verify backend tests pass.
- Verify frontend builds correctly.
