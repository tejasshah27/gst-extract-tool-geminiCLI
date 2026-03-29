# REFINED.md

Refined guidance for Claude Code working on the GST Data Extraction Tool prototype.
This is a **prototype** — avoid over-engineering, premature abstractions, and production-grade ceremony.

---

## Project Overview

**GST Data Extraction Tool** — A Java CLI application that reads Indian GST return data from
`.xlsx` files exported from the GSTN portal, validates it, reconciles it against a purchase
register, and exports results for reporting or filing purposes.

**This is a prototype.** Favour simplicity and working code over architecture purity.

---

## Scope

### In Scope — Return Types

| Return | Direction | Nature | Notes |
|--------|-----------|--------|-------|
| GSTR-1 | Outward (sales) | Invoice-level | Filed by supplier |
| GSTR-2A | Inward (purchases) | Invoice-level, dynamic | Auto-populated from counterparty GSTR-1; updates as suppliers file |
| GSTR-2B | Inward (purchases) | Invoice-level, static | Monthly snapshot on 14th; used for ITC claims from Apr 2021 onwards |
| GSTR-3B | Summary | Aggregate only | Self-declared monthly summary; **no invoice-level data** |

GSTR-2A and GSTR-2B have nearly identical data models — share the same parser and model
classes, distinguished only by a `sourceType` enum field. This keeps the code lean.

### Explicitly Out of Scope

- GSTR-9 (Annual Return), GSTR-9C (Reconciliation Statement)
- E-way bill data
- GST portal API / live data fetching (tool reads static XLSX exports only)
- IRN (Invoice Reference Number) generation or validation
- E-invoice schema validation
- Multi-entity / multi-GSTIN client management
- QRMP scheme quarterly filing logic

---

## GST Domain Reference

### GSTIN Format

```
<SS><XXXXXXXXXX><E><Z><C>
 |       |        |  |  |
 |       |        |  |  └─ Check digit (MOD-36 algorithm, alphanumeric 0-9/A-Z)
 |       |        |  └──── Reserved, always 'Z'
 |       |        └─────── Entity code: 1st registration = 1, then 2–9, A–Z (alphanumeric)
 |       └──────────────── 10-character PAN of the entity
 └──────────────────────── 2-digit state code (01–37; 97 = other territory, 99 = centre)
```

- Always 15 characters, uppercase alphanumeric only (normalise to uppercase before validation).
- Check digit uses MOD-36 with a specific character set weighting — implement as a dedicated
  `GstinValidator.isValid(String gstin)` method.
- Valid state codes: 01–37. Codes 97 (other territory) and 99 (centre/CBIC) also appear.

### Tax Components

| Component | Full Name | Applies When |
|-----------|-----------|--------------|
| CGST | Central GST | Intra-state supply |
| SGST | State GST | Intra-state supply (states with legislature) |
| UTGST | Union Territory GST | Intra-state supply in UTs without legislature* |
| IGST | Integrated GST | Inter-state supply or import |
| CESS | GST Cess | On top of 28% slab (luxury/sin goods) |

*UTs without legislature: Andaman & Nicobar, Lakshadweep, Dadra & Nagar Haveli,
Daman & Diu, Chandigarh, Leh/Ladakh.

**Mutual exclusivity rule** (critical for tax cross-check validation):
- A transaction has **either** CGST+SGST **or** CGST+UTGST **or** IGST. Never a mix.
- Intra-state: `CGST = taxable_value × (rate / 2)`, `SGST = taxable_value × (rate / 2)`
- Inter-state: `IGST = taxable_value × rate`
- CGST and SGST are independent calculations on taxable value — they are NOT derived from IGST.
  The formula "CGST + SGST = IGST/2" is **incorrect** and must not be used.

### GST Rate Slabs

Valid GST rates in practice: `0, 0.1, 0.25, 1.5, 3, 5, 7.5, 12, 18, 28` (percent).
Flag any rate outside this set as a validation warning (not a hard error — rates can change).

### Place of Supply (POS)

- A 2-digit state code field present in GSTR-1 and GSTR-2A/2B line items.
- Determines intra-state (POS == supplier's state) vs inter-state (POS != supplier's state).
- Drives which tax components are expected on the invoice.

### Return Period Format

- Portal column headers use text like `"April 2025"`.
- Portal file names and internal fields use `MMYYYY` (e.g., `042025` for April 2025).
- Handle both formats in parsing; normalise to `MMYYYY` internally.

### Reverse Charge Mechanism (RCM)

- A boolean flag (`Y/N`) on invoice line items in GSTR-2A/2B.
- RCM invoices: tax paid by recipient, not supplier.
- ITC eligibility logic must check RCM flag — RCM ITC has different claim rules.

### Invoice Number Rules

- Up to 16 characters, alphanumeric, may contain `/` and `-`.
- No spaces allowed. Case-insensitive for matching (normalise to uppercase).

### HSN / SAC Codes

- HSN: Harmonized System of Nomenclature for goods (2–8 digits).
- SAC: Service Accounting Code for services (6 digits, starts with 99).
- Present in GSTR-1 line items and HSN summary sheet.
- Not required for validation in this prototype, but parse and preserve the field.

---

## GSTR-1 Sub-Tables (Portal XLSX Sheets)

The portal XLSX for GSTR-1 contains multiple sheets, one per sub-table:

| Sheet / Sub-table | Contents |
|-------------------|----------|
| B2B | Business-to-business invoices (registered recipients) |
| B2CL | B2C large: inter-state invoices > ₹2.5 lakh to unregistered recipients |
| B2CS | B2C small: intra-state and small inter-state to unregistered |
| CDNR | Credit/Debit Notes for registered recipients |
| CDNUR | Credit/Debit Notes for unregistered recipients |
| EXP | Export invoices |
| AT | Advance tax received |
| ATADJ | Adjustment of advance tax |
| EXEMP | Nil-rated, exempt, non-GST supplies |
| HSNSAC | HSN-wise summary of outward supplies |
| DOCS | Summary of documents issued |

**Amendment sub-tables** (suffix `A`): B2BA, CDNRA, EXPA — amendments to previously filed
invoices. Parse these alongside their base tables; mark amended records with an `isAmendment`
flag.

For the prototype, prioritise **B2B and CDNR** as they cover the primary reconciliation use
case. Remaining tables can be stubbed with a "not yet supported" log message.

---

## GSTR-2A / GSTR-2B Sub-Tables (Portal XLSX Sheets)

| Sheet / Sub-table | Contents |
|-------------------|----------|
| B2B | Inward supplies from registered suppliers |
| B2BA | Amendments to B2B |
| CDNR | Credit/Debit Notes received from registered suppliers |
| CDNRA | Amendments to CDNR |
| ISD | Input Service Distributor invoices |
| ISDA | Amendments to ISD |
| TDS | Tax Deducted at Source entries |
| TCS | Tax Collected at Source entries |

For the prototype, prioritise **B2B and CDNR**. ISD, TDS, TCS can be parsed but reconciliation
logic for them is out of scope.

---

## GSTR-3B Structure

GSTR-3B is **aggregate only** — no invoice-level data exists. It contains:

| Table | Contents |
|-------|----------|
| 3.1 | Nature-wise outward supply totals (taxable, exempt, nil, non-GST) |
| 3.2 | Inter-state supply breakup by recipient state |
| 4 | ITC claimed (IGST, CGST, SGST broken by input goods/services/capital goods) |
| 5 | Values of exempt, nil, non-GST inward supplies |
| 5.1 | Interest and late fees payable |

**Consequence**: Invoice validation (Feature 5) does not apply to GSTR-3B. The GSTR-3B
parser produces a `SummaryReturn` object, not a list of `Invoice` objects.

---

## Target Features

### Core Extraction
1. **Multi-sheet XLSX Parsing** — Parse GSTR-1, GSTR-2A, GSTR-2B, and GSTR-3B from portal
   XLSX exports. Auto-detect return type from sheet names or filename. Handle merged cells,
   multi-row headers, and blank rows — these are common in portal exports.
2. **Dynamic Column Mapping** — Resolve column header variations via `config.yml`
   (e.g., `"GSTIN of Supplier"` and `"Supplier GSTIN"` both map to canonical `gstin`).
3. **Batch File Processing** — Process multiple `.xlsx` files from a directory in one run.

### Data Validation
4. **GSTIN Validation** — 15-char format check + MOD-36 checksum + valid state code range.
5. **Invoice Validation** — Format (≤16 chars, allowed chars), date within return period,
   duplicate detection within batch. **Applies to GSTR-1, GSTR-2A, GSTR-2B only.**
6. **Tax Amount Cross-check** — Validate: `CGST ≈ SGST ≈ taxable × (rate/2)` for intra-state;
   `IGST ≈ taxable × rate` for inter-state. Use configurable tolerance (default ₹1.00).
   Check mutual exclusivity of tax components.

### Reconciliation
7. **GSTR-2A/2B vs Purchase Register Matching** — Match supplier invoices from portal export
   against a local purchase register (also XLSX), flagging mismatches in GSTIN, invoice
   number, date, or amount.
8. **ITC Eligibility Flagging** — Mark line items eligible/ineligible based on: supplier
   GSTIN valid, invoice not duplicate, RCM flag, and configurable block-list rules.

### Output & Reporting
9. **Filtered Export to XLSX/CSV** — Export with filters: date range, GSTIN, return period,
   mismatch-only.
10. **Summary Report** — Sheet with totals: taxable value, CGST/SGST/IGST/CESS, eligible ITC,
    mismatch count.
11. **Error Report** — Separate sheet/file listing all validation failures with sheet name,
    row number, field, and failure reason.

### Usability
12. **CLI Interface** — Flags: `--input`, `--output`, `--return-type`, `--mode`
    (validate | reconcile | export | all).
13. **Configuration File** — `config.yml` for column name mappings, tolerance thresholds,
    and ITC block rules.

---

## Tech Stack

| Concern | Library | Notes |
|---------|---------|-------|
| XLSX reading/writing | Apache POI `poi-ooxml` | Use streaming (SAX/SXSSF) for files with 1000+ rows |
| CLI argument parsing | Picocli | |
| Configuration | SnakeYAML | Chosen over `Properties` — config is nested (return type → sheet → column aliases) |
| Build tool | Maven | |
| Testing | JUnit 5 + AssertJ | |
| Logging | SLF4J + Logback | |

**Mandatory**: Use `BigDecimal` for **all** monetary amounts (taxable value, tax amounts).
Never use `double` or `float` — floating-point errors will cause false validation failures.

**Java version**: Target Java 17 (LTS). Use records for immutable model objects where
appropriate (e.g., `ValidationError`, `MatchResult`).

---

## Build & Run Commands

```bash
# Build
mvn clean package

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=GstinValidatorTest

# Run the tool (after build)
java -jar target/gst-extract-tool.jar --input ./data/GSTR2A_Apr2025.xlsx --output ./out/

# Reconcile GSTR-2B against purchase register
java -jar target/gst-extract-tool.jar \
  --input ./data/GSTR2B_Apr2025.xlsx \
  --purchase-register ./data/PurchaseRegister_Apr2025.xlsx \
  --output ./out/ \
  --mode reconcile
```

---

## Project Structure

```
src/
  main/java/com/gstextract/
    cli/            # CLI entry point (Main.java, GstExtractCommand.java)
    parser/         # One parser class per return type
      Gstr1Parser.java
      Gstr2aParser.java   # Handles both GSTR-2A and GSTR-2B (sourceType field)
      Gstr3bParser.java
      SubTableParser.java # Shared logic for sub-table sheet detection
    model/          # Immutable data objects
      Invoice.java         # GSTR-1 outward invoice (B2B, CDNR, etc.)
      InwardInvoice.java   # GSTR-2A/2B inward invoice
      SummaryReturn.java   # GSTR-3B aggregate data
      ValidationError.java # record: sheet, row, field, reason
      MatchResult.java     # record: invoice pair + mismatch details
      ReturnType.java      # enum: GSTR1, GSTR2A, GSTR2B, GSTR3B
      SourceType.java      # enum: GSTR2A, GSTR2B (for shared inward model)
    validator/      # Stateless validators
      GstinValidator.java  # MOD-36 checksum + state code check
      InvoiceValidator.java
      TaxAmountValidator.java
    reconciler/     # GSTR-2A/2B vs purchase register matching
      InvoiceMatcher.java
      ItcEligibilityChecker.java
    exporter/       # XLSX/CSV output writers
      XlsxExporter.java
      CsvExporter.java
    config/         # Config loading
      AppConfig.java       # Loaded from config.yml via SnakeYAML
  main/resources/
    config.yml      # Default column mappings and thresholds (see schema below)
    logback.xml
  test/java/com/gstextract/
    # Mirrors main structure
    # test/resources/ must contain sample synthetic XLSX files for parser tests
```

---

## config.yml Schema (Skeleton)

```yaml
tolerance:
  taxAmount: 1.00       # ₹ tolerance for tax cross-check (BigDecimal)

columnMappings:
  gstr1:
    b2b:
      gstin: ["GSTIN of Recipient", "Recipient GSTIN", "GSTIN/UIN of Recipient"]
      invoiceNumber: ["Invoice Number", "Invoice No", "Invoice No."]
      invoiceDate: ["Invoice Date", "Invoice Dt"]
      taxableValue: ["Taxable Value", "Total Taxable Value"]
      igst: ["Integrated Tax Amount", "IGST Amount", "IGST"]
      cgst: ["Central Tax Amount", "CGST Amount", "CGST"]
      sgst: ["State/UT Tax Amount", "SGST/UTGST Amount", "SGST"]
  gstr2a:
    b2b:
      gstin: ["GSTIN of Supplier", "Supplier GSTIN"]
      # ... same pattern

itcBlockRules:
  - field: supplierGstin
    blocklist: []          # Add specific GSTINs to block ITC
  - rcmOnly: false         # If true, only allow ITC on RCM invoices
```

---

## Error Handling Strategy

This is a prototype — use **collect-all errors** (not fail-fast):
- Parsers collect `ValidationError` objects and continue to the next row.
- Print a consolidated error report at the end.
- Only abort on unrecoverable errors (file not found, unreadable XLSX, unknown return type).
- Do not throw checked exceptions from validators — return `Optional<ValidationError>` or
  a `List<ValidationError>`.

---

## Portal XLSX Quirks (Important for Parser Implementation)

1. **Merged cells** — Header rows often span multiple columns. Use POI's
   `MergedRegion` APIs or unmerge before reading.
2. **Multi-row headers** — Some sheets have 2–3 header rows before data starts.
   Detect the data start row by looking for the first non-empty cell in column A
   matching a known field name.
3. **Blank rows** — Portal exports insert blank rows between sections. Skip rows
   where all cells are empty or null.
4. **Numeric date cells** — Dates may be stored as POI numeric cells, not strings.
   Always use `DateUtil.isCellDateFormatted()` before reading date cells.
5. **Amount formatting** — Tax amounts may have commas (e.g., `"1,23,456.78"`).
   Strip commas before parsing to `BigDecimal`.

---

## Feature-to-Return-Type Matrix

| Feature | GSTR-1 | GSTR-2A | GSTR-2B | GSTR-3B |
|---------|--------|---------|---------|---------|
| XLSX Parsing | B2B, CDNR (others stub) | B2B, CDNR (others stub) | B2B, CDNR (others stub) | All 5 tables |
| GSTIN Validation | Yes | Yes | Yes | Supplier GSTINs only |
| Invoice Validation | Yes | Yes | Yes | **No** (aggregate only) |
| Tax Cross-check | Yes | Yes | Yes | Yes (aggregate totals) |
| Reconciliation | Source | Yes (match vs PR) | Yes (match vs PR) | No |
| ITC Flagging | No | Yes | Yes | No |
| Export | Yes | Yes | Yes | Yes |
