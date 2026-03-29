# GST Extract Tool — Implementation Plan

## Context

Web application for processing Indian GST return data from GSTN portal `.xlsx` exports.
Angular frontend + Spring Boot backend. User uploads files via browser, backend processes
and returns a downloadable XLSX result while also persisting a copy server-side.
Scoped to GSTR-1, GSTR-2A, GSTR-2B, GSTR-3B. Prototype — simplicity over architecture.

---

## Project Structure

```
GST Extract Tool/
├── backend/      # Spring Boot Maven project
└── frontend/     # Angular CLI project
```

---

## Architecture

```
Browser (Angular)
   │  POST /api/process  (multipart/form-data)
   │  Fields: returnFile, purchaseRegisterFile (optional),
   │          returnType, mode
   ▼
ProcessController          (Spring Boot REST controller)
   │
   ├── ProcessOrchestrator (coordinates the pipeline)
   │     ├── AppConfig            (loaded from config.yml)
   │     ├── ReturnTypeDetector
   │     ├── Gstr1Parser / Gstr2aParser / Gstr3bParser
   │     │        └── SubTableParser
   │     ├── GstinValidator / InvoiceValidator / TaxAmountValidator
   │     ├── InvoiceMatcher / ItcEligibilityChecker
   │     └── XlsxExporter / CsvExporter
   │
   └── OutputStorageService
         └── Saves copy to: output/{originalFilename}/{yyyyMMdd_HHmmss}/result.xlsx
```

**Single endpoint**: `POST /api/process`
**Response**: streams `result.xlsx` as `application/octet-stream` with
`Content-Disposition: attachment; filename="result.xlsx"`. Frontend triggers browser download.
Backend also writes the same file to the output directory before sending.

---

## Key Decisions

| Decision | Choice | Why |
|---|---|---|
| Backend framework | Spring Boot 4.0.0 (no CLI) | Web service |
| Frontend | Angular — single standalone component, no routing | Per requirement |
| Endpoint design | Single `POST /api/process` multipart | One endpoint, params distinguish behaviour |
| Output | Streamed download + server copy | Per requirement |
| Server output path | `output/{originalFilename_noext}/{yyyyMMdd_HHmmss}/` | File-wise subdirectory, timestamped |
| Monetary types | `BigDecimal` everywhere | Floating-point causes false validation failures |
| Java version | **21** (LTS, recommended by Spring Boot 4.0) | Virtual threads, records, modern syntax |
| Error handling | Collect-all | Parsers accumulate `List<ValidationError>`, continue |
| XLSX reading | `XSSFWorkbook` (in-memory) | Portal exports < 5000 rows; streaming adds complexity |
| CORS | Enabled for `localhost:4200` in dev | Angular dev server |
| Test fixtures | Programmatic POI in `@BeforeAll` | No binary XLSX in repo; self-documenting |

---

## Backend

### `backend/pom.xml` — Dependencies

```
spring-boot-starter-web          # managed by Spring Boot 4.0.0
spring-boot-starter              # managed
poi-ooxml 5.5.1                  # NOT managed by Spring Boot — declare version explicitly
                                  # (SnakeYAML 2.2, SLF4J 2.0.17, Logback 1.5.32,
                                  #  AssertJ 3.27.7, JUnit Jupiter 6.0.3 — all managed)
spring-boot-starter-test         # test scope; pulls in JUnit 6 + AssertJ automatically
```

**Maven 3.6.3+ required** (Spring Boot 4.0.0 minimum).
No Picocli. Build plugin: `spring-boot-maven-plugin 4.0.0` (produces executable JAR).

### Package Structure

```
src/main/java/com/gstextract/
  api/
    ProcessController.java       # @RestController, single @PostMapping("/api/process")
    ProcessRequest.java          # Holds MultipartFile + String params (returnType, mode)
    ProcessResponse.java         # Used only for error responses (JSON)
  orchestrator/
    ProcessOrchestrator.java     # Owns the pipeline; called by controller
  parser/
    ParseUtils.java
    SubTableParser.java
    Gstr1Parser.java
    Gstr2aParser.java            # Handles GSTR-2A and GSTR-2B via SourceType enum
    Gstr3bParser.java
    PurchaseRegisterParser.java
    ReturnTypeDetector.java
  model/
    ReturnType.java              # enum: GSTR1, GSTR2A, GSTR2B, GSTR3B
    SourceType.java              # enum: GSTR2A, GSTR2B
    MatchStatus.java             # enum: MATCHED, AMOUNT_MISMATCH, NOT_FOUND, ...
    ValidationError.java         # record: sheet, row, field, reason, Severity
    MatchResult.java             # record: portalInvoice, registerRow, status, mismatches
    Invoice.java                 # All monetary fields BigDecimal; includes placeOfSupply (2-digit state code), utgst, isAmendment
    InwardInvoice.java           # Extends Invoice fields + sourceType, itcEligible, reverseCharge (RCM flag)
    SummaryReturn.java           # GSTR-3B aggregate amounts (BigDecimal)
  validator/
    GstinValidator.java          # MOD-36 checksum + state code range
    InvoiceValidator.java
    TaxAmountValidator.java
  reconciler/
    InvoiceMatcher.java
    ItcEligibilityChecker.java
  exporter/
    XlsxExporter.java
    CsvExporter.java
  storage/
    OutputStorageService.java    # Saves output file to disk; returns saved path
  config/
    AppConfig.java               # SnakeYAML load from config.yml
    CorsConfig.java              # Enables CORS for localhost:4200

src/main/resources/
  application.properties         # server.port=8080, output.base-dir=./output
                                 # spring.servlet.multipart.max-file-size=50MB
                                 # spring.servlet.multipart.max-request-size=50MB
  config.yml                     # Column mappings, tolerances, ITC block rules
  logback.xml
```

### Endpoint Contract

```
POST /api/process
Consumes: multipart/form-data
Produces: application/octet-stream (success) | application/json (error)

Parts:
  returnFile             required  — GST return XLSX
  purchaseRegisterFile   optional  — required only for mode=reconcile or mode=all
  returnType             required  — GSTR1 | GSTR2A | GSTR2B | GSTR3B
  mode                   required  — validate | reconcile | export | all

Success response:
  HTTP 200
  Content-Disposition: attachment; filename="result.xlsx"
  Body: raw XLSX bytes (browser triggers download)

Error response:
  HTTP 400 / 500
  Body: { "error": "message" }
```

### `OutputStorageService` Path Convention

```
Base dir : configured via application.properties → output.base-dir
Sub-path : {base-dir}/{originalFilename_without_ext}/{yyyyMMdd_HHmmss}/result.xlsx
Behaviour: creates directories if absent; logs saved path; does NOT send path to client
```

---

## Frontend

### Scaffold

```bash
ng new gst-extract-ui --standalone --skip-tests=false --style=css --routing=false
```

### File Structure

```
src/app/
  app.component.ts      # Form state, submit handler, download trigger
  app.component.html    # Full UI template
  app.component.css     # Minimal layout styling
  process.service.ts    # HttpClient POST → responseType: 'blob'
  app.config.ts         # provideHttpClient()
```

### UI Layout

```
┌─────────────────────────────────────────────────────┐
│  GST Extract Tool                                   │
├─────────────────────────────────────────────────────┤
│  Return Type:  [GSTR-2A            ▾]               │
│                                                     │
│  GST Return File:                                   │
│  [     Choose File     ]  GSTR2A_Apr2025.xlsx       │
│                                                     │
│  Purchase Register:  ← shown only for Reconcile/All │
│  [     Choose File     ]  PurchaseReg.xlsx          │
│                                                     │
│  Mode:  ○ Validate  ○ Reconcile  ○ Export  ● All   │
│                                                     │
│              [       Submit       ]                 │
├─────────────────────────────────────────────────────┤
│  ✓ Processing complete                              │
│  ⚠ 3 validation errors found                       │
│                                                     │
│  [ ⬇  Download result.xlsx ]                       │
└─────────────────────────────────────────────────────┘
```

### Behaviour Rules

- Purchase Register input: hidden unless mode is `reconcile` or `all`
- Submit button: disabled while request in-flight; show spinner
- On success: show download link (blob URL via `URL.createObjectURL`)
- On error: show error message from JSON response body

---

## Phase 1 — Backend Core + Basic UI

**Goal**: Endpoint live; GSTR-2A/2B B2B parsed and validated; result downloadable in browser
and saved on server.

### File Creation Order

```
Backend
 1. backend/pom.xml
 2. src/main/resources/application.properties
 3. src/main/resources/logback.xml
 4. src/main/resources/config.yml
 5. model/ReturnType.java, SourceType.java, MatchStatus.java      (enums)
 6. model/ValidationError.java, MatchResult.java                  (records)
 7. model/Invoice.java, InwardInvoice.java, SummaryReturn.java
 8. config/AppConfig.java
 9. config/CorsConfig.java
10. parser/ParseUtils.java
11. parser/SubTableParser.java       ← hardest file; merged cells + multi-row headers
12. validator/GstinValidator.java    ← MOD-36; write + test before parsers
13. validator/InvoiceValidator.java
14. validator/TaxAmountValidator.java
15. parser/Gstr2aParser.java
16. exporter/XlsxExporter.java       (Phase 1: Invoices + Errors sheets only)
17. storage/OutputStorageService.java
18. orchestrator/ProcessOrchestrator.java
19. api/ProcessController.java
20. GstExtractApplication.java       (@SpringBootApplication)

Frontend
21. ng new gst-extract-ui (scaffold)
22. process.service.ts
23. app.component.ts / .html / .css
```

### Phase 1 Tests

| Test | Coverage |
|---|---|
| `GstinValidatorTest` | MOD-36, state code range, length, null/empty |
| `ParseUtilsTest` | BigDecimal comma stripping, date cell formats |
| `Gstr2aParserTest` | 3 valid rows + 1 bad GSTIN + 1 amount mismatch → correct counts |
| `TaxAmountValidatorTest` | Mutual exclusivity, intra/inter-state, tolerance boundary |
| `ProcessControllerTest` | MockMvc POST with mock XLSX → HTTP 200, file response headers |

> Tests use JUnit Jupiter 6 (`@Test` from `org.junit.jupiter.api`) + AssertJ — both provided by `spring-boot-starter-test`, no separate declarations needed.

---

## Phase 2 — Reconciliation + GSTR-1

**Goal**: GSTR-1 parsing, purchase register upload, reconcile pipeline wired end-to-end.

```
Backend
24. parser/PurchaseRegisterParser.java
25. reconciler/InvoiceMatcher.java
26. reconciler/ItcEligibilityChecker.java
27. parser/Gstr1Parser.java
28. parser/ReturnTypeDetector.java
29. ProcessOrchestrator.java     — wire reconcile mode
30. ProcessController.java       — handle purchaseRegisterFile part
    XlsxExporter.java            — add Reconciliation sheet

Frontend
31. Show/hide Purchase Register input reactively on mode change
    config.yml — add purchaseRegister column aliases section
```

### Phase 2 Tests

| Test | Coverage |
|---|---|
| `InvoiceMatcherTest` | Exact match, amount mismatch, NOT_FOUND, IN_REGISTER_ONLY, case-insensitive keys |
| `ItcEligibilityCheckerTest` | Blocklist, validation errors → ineligible, RCM flag |
| `Gstr1ParserTest` | B2B + CDNR sheets with synthetic fixture |

---

## Phase 3 — GSTR-3B + Summary Sheet + Polish

**Goal**: Full pipeline; GSTR-3B aggregate parsing; Summary sheet in output; structured UI result.

```
Backend
32. parser/Gstr3bParser.java     — row-label scan for tables 3.1, 4, 5.1 → SummaryReturn
    XlsxExporter.java            — add Summary sheet
    ProcessOrchestrator.java     — wire all modes end-to-end

Frontend
33. Replace plain-text result area with structured summary:
    invoice count, error count, match stats (from response headers or a pre-download JSON)
```

### Phase 3 Tests

| Test | Coverage |
|---|---|
| `Gstr3bParserTest` | All 5 GSTR-3B tables; assert SummaryReturn fields |
| `XlsxExporterTest` | Write + re-read with POI; assert 4 sheet names, row counts, totals |
| `EndToEndTest` | `@SpringBootTest`: POST GSTR-2B + purchase register, mode=all → assert response headers, server output file created |

---

## Stub vs Implement

| Item | Decision |
|---|---|
| B2CL, B2CS, CDNUR, EXP, AT, ATADJ, EXEMP, HSNSAC, DOCS sheets | Log "not yet supported", skip |
| ISD, TDS, TCS in GSTR-2A/2B | Log "not yet supported", skip |
| MOD-36 checksum | Implement fully — pure function, ~20 lines |
| Amendment sub-tables (B2BA, CDNRA) | Set `isAmendment=true`, no special logic |
| Streaming POI reads (SAX) | Skip — use in-memory XSSFWorkbook |
| Angular routing | Skip — single page |
| Auth / login | Out of scope |
| Multi-GSTIN / multi-entity management | Out of scope |

---

## Domain Rules to Encode

- **Place of Supply (POS)**: 2-digit state code on every invoice; drives intra vs inter-state determination (POS == supplier state → intra-state; POS != supplier state → inter-state)
- **Tax mutual exclusivity**: transaction has EITHER `CGST+SGST` OR `CGST+UTGST` OR `IGST` — never mixed; UTGST applies to Union Territories without legislature (e.g. Andaman, Lakshadweep)
- **Intra-state**: `CGST = SGST (or UTGST) = taxable × (rate / 2)` — determined by POS, independent of IGST
- **Inter-state**: `IGST = taxable × rate` — determined by POS
- **Valid GST rate slabs**: `0, 0.1, 0.25, 1.5, 3, 5, 7.5, 12, 18, 28` (warn on others)
- **GSTIN MOD-36**: charset `"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"`, weighted sum of 14 chars, `% 36` → lookup
- **Portal XLSX quirks**: merged cells, multi-row headers, blank rows, numeric date cells, comma-formatted amounts

---

## Running the App

```bash
# Backend (http://localhost:8080)
cd backend && mvn spring-boot:run

# Frontend (http://localhost:4200)
cd frontend && npm install && ng serve

# Backend tests
cd backend && mvn test
```

---

## Verification Checklist

| Phase | How to verify |
|---|---|
| Phase 1 | `mvn test` passes; open `localhost:4200`, upload GSTR-2A XLSX, mode=validate, submit → browser downloads `result.xlsx`; check `output/` on server for copy |
| Phase 2 | Upload GSTR-2B + purchase register, mode=reconcile → `result.xlsx` contains Reconciliation sheet |
| Phase 3 | Upload GSTR-3B, mode=all → `result.xlsx` has 4 sheets: Invoices, Reconciliation, Errors, Summary |
