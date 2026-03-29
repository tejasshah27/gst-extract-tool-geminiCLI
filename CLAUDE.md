# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**GST Data Extraction Tool** — A Java application that reads GST (Goods and Services Tax) data from `.xlsx` files and processes, validates, and exports it for reporting or filing purposes.

---

## Proposed Features

### Core Extraction
1. **Multi-sheet XLSX Parsing** — Read GSTR-1, GSTR-2A, GSTR-3B, and other return formats from Excel files with auto-detection of sheet structure.
2. **Dynamic Column Mapping** — Handle variations in column headers across different GST portal exports (e.g., "GSTIN of Supplier" vs "Supplier GSTIN").
3. **Batch File Processing** — Process multiple `.xlsx` files from a directory in one run.

### Data Validation
4. **GSTIN Validation** — Verify 15-character GSTIN format using checksum rules (state code + PAN + entity + check digit).
5. **Invoice Validation** — Check invoice number format, date range, and duplicate detection within a batch.
6. **Tax Amount Cross-check** — Validate that CGST + SGST = IGST/2 where applicable, and that taxable value × rate = tax amount within tolerance.

### Reconciliation
7. **GSTR-2A vs Purchase Register Matching** — Match supplier invoices from the portal export against a local purchase register, flagging mismatches in GSTIN, invoice number, date, or amount.
8. **ITC Eligibility Flagging** — Mark line items as eligible/ineligible for Input Tax Credit based on configurable rules.

### Output & Reporting
9. **Filtered Export to XLSX/CSV** — Export validated/reconciled data with applied filters (date range, GSTIN, return period, mismatch-only).
10. **Summary Report** — Generate a summary sheet: total taxable value, CGST/SGST/IGST totals, eligible ITC, mismatch count.
11. **Error Report** — Separate output file listing all validation failures with row references and failure reasons.

### Usability
12. **CLI Interface** — Command-line runner with flags for input file/directory, output path, return type, and validation mode.
13. **Configuration File** — YAML/properties config for column name mappings, tax rates, and tolerance thresholds.

---

## Planned Tech Stack

| Concern | Library |
|---|---|
| XLSX reading/writing | Apache POI (`poi-ooxml`) |
| CLI argument parsing | Picocli |
| Configuration | SnakeYAML or `java.util.Properties` |
| Build tool | Maven (`pom.xml`) |
| Testing | JUnit 5 + AssertJ |
| Logging | SLF4J + Logback |

---

## Build & Run Commands

> Commands will be filled in once the Maven project is initialized.

```bash
# Build
mvn clean package

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=GstinValidatorTest

# Run the tool (after build)
java -jar target/gst-extract-tool.jar --input ./data/GSTR2A_Apr2025.xlsx --output ./out/
```

---

## Project Structure (Target)

```
src/
  main/java/com/gstextract/
    cli/          # CLI entry point and argument parsing
    parser/       # XLSX sheet readers per return type
    model/        # POJOs: Invoice, LineItem, GstReturn
    validator/    # GSTIN, invoice, and tax amount validators
    reconciler/   # Matching logic between two datasets
    exporter/     # XLSX/CSV writers
    config/       # Configuration loading
  main/resources/
    config.yml    # Default column mappings and thresholds
  test/java/com/gstextract/
    ...           # Mirrors main structure
```

---

## GST Domain Notes

- **GSTIN format**: `<2-digit state code><10-char PAN><1 entity number><Z><1 check digit>`
- **Return types in scope**: GSTR-1 (outward supplies), GSTR-2A (auto-populated inward), GSTR-3B (summary return)
- **Tax components**: CGST (central), SGST (state), IGST (integrated, for inter-state), CESS (for specific goods)
- **Return period**: Month + Year (e.g., `042025` for April 2025)
