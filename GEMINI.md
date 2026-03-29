# GEMINI.md - GST Extract Tool

## Project Overview
The **GST Data Extraction Tool** is a Java application designed to automate the processing of Indian Goods and Services Tax (GST) data. It reads `.xlsx` files exported from the GSTN portal, performs data validation, and reconciles inward supplies against a purchase register.

### Core Technologies
- **Backend:** Java 21, Spring Boot 4.0.0, Apache POI (XLSX parsing), SnakeYAML (Configuration).
- **Frontend:** Angular (Standalone components).
- **Build Tool:** Maven 3.6.3+.
- **Testing:** JUnit 5/6, AssertJ.

### Architecture
The project follows a Research -> Strategy -> Execution lifecycle for development.
- **Backend (`/backend`):** A REST API providing a single endpoint `POST /api/process` for file processing.
- **Frontend (`/frontend`):** A single-page Angular application for file uploads and result visualization.

---

## Building and Running

### Prerequisites
- JDK 21
- Maven 3.6.3+
- Node.js & npm (for frontend)

### Backend
```bash
# Navigate to backend directory
cd backend

# Build the project
mvn clean package

# Run tests
mvn test

# Run the Spring Boot application
mvn spring-boot:run
```
The API will be available at `http://localhost:8080`.

### Frontend
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start the development server
ng serve
```
The UI will be available at `http://localhost:4200`.

---

## Development Conventions

### Coding Standards
- **Monetary Values:** ALWAYS use `BigDecimal` for tax and taxable amounts to avoid floating-point errors.
- **Immutability:** Use Java `records` for data transfer objects (DTOs) and models (e.g., `ValidationError`, `MatchResult`).
- **Error Handling:** Use a "collect-all" strategy for parsers. Accumulate `ValidationError` objects instead of failing fast on a single row.
- **Naming:** Follow standard Java/Spring and Angular naming conventions.

### Testing Practices
- **Validation First:** New features or bug fixes must include unit tests (e.g., `GstinValidatorTest`, `TaxAmountValidatorTest`).
- **Mocking:** Use `Mockito` for service-level tests and `MockMvc` for controller tests.
- **XLSX Fixtures:** Use programmatic POI generation in `@BeforeAll` for test data rather than storing binary files in the repository.

### Domain Specifics
- **GSTIN Validation:** Follow the MOD-36 checksum algorithm.
- **Tax Rules:** 
  - Intra-state: CGST + SGST (or UTGST).
  - Inter-state: IGST.
  - Mutual exclusivity: A transaction never has both IGST and CGST/SGST.
- **Place of Supply (POS):** 2-digit state code determines tax applicability.

---

## Key Files
- `backend/src/main/resources/config.yml`: Defines column mappings for different portal exports and tax tolerances.
- `backend/src/main/resources/application.properties`: Server configuration and file storage paths.
- `PLAN.md`: Detailed implementation roadmap and architectural decisions.
- `REFINED.md`: Technical deep-dive into GST domain rules and parser logic.
- `CLAUDE.md`: High-level guide for AI interactions.
