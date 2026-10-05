# NovaPay Backend — Architecture & System Master Guide

> Technical documentation explaining the system architecture, domain design, fiscal compliance engine (Verifactu AEAT), security Model, and database schema.

---

## 🏛️ 1. Architecture Overview (Hexagonal / Ports & Adapters)

NovaPay Backend is built following **Hexagonal Architecture** (also known as Ports and Adapters) paired with **Domain-Driven Design (DDD)**.

The primary goal of this architecture is to decouple core business logic from framework infrastructure, external HTTP clients, and database persistence layers.

### Key Layers:

1. **Domain Layer (`levelup42.novapay_backend_hex.domain`)**:
   - Contains pure Java entities (`Invoice`, `FiscalRecord`, `Company`, `PosTerminal`, `ApiClient`).
   - Value Objects: `Money`, `InvoiceNumber`, `TaxId`.
   - Driving Ports (Use Case interfaces): `InvoiceCreateUseCase`, `InvoiceCancelUseCase`, `FiscalRetrySubmitUseCase`, `FiscalGetStatusUseCase`.
   - Driven Ports (Adapter interfaces): `FiscalAgencyPort`, `XmlSignerPort`, `InvoiceRepositoryPort`, `FiscalRecordRepositoryPort`, etc.
   - Domain Services: `TaxCalculationService`, `TokenGenerationService`.

2. **Application Layer (`levelup42.novapay_backend_hex.application`)**:
   - Implements Use Case interfaces.
   - Orchestrates transactions, domain services, and driven ports.
   - Examples: `EmitInvoiceService`, `FiscalEvidenceService`, `RetryFiscalSubmissionService`.

3. **Infrastructure Layer (`levelup42.novapay_backend_hex.infrastructure`)**:
   - **Inbound Adapters**: REST Controllers (`InvoiceController`, `FiscalController`, `AuthController`), OpenAPI configuration, and Spring Security filters (`JwtAuthFilter`).
   - **Outbound Adapters**: 
     - `VerifactuAdapter`: AEAT WebService Client, XML Builder (`VerifactuXmlBuilder`), and Response Parser (`VerifactuResponseParser`).
     - `XmlSignerImpl`: XMLDSig RSA-SHA256 digital signature engine via Apache Santuario and PKCS#12 keystore.
     - `Persistence Adapters`: JPA Repositories (`JpaInvoiceRepository`, `JpaFiscalRecordRepository`) and MapStruct Mappers.

---

## 🔒 2. Spanish Fiscal Regulation Compliance (Verifactu AEAT)

The backend implements Spanish **Royal Decree 1007/2023 (Verifactu)**:

- **Tamper-Evident Chained Hashing (SHA-256)**: Each invoice generates a SHA-256 fingerprint computed from the current invoice data concatenated with the hash of the immediately preceding invoice for the same issuer.
- **SOAP/XML Envelope Construction**: Generates XML compliance payloads adhering to the `SuministroInformacion.xsd` schema.
- **XMLDSig Digital Signing**: Signs outgoing SOAP Envelopes using RSA-SHA256 with client certificate PKCS#12.
- **AEAT Response Processing**: Parses AEAT web service responses (`EstadoEnvio`, `CSV`, `RespuestaLinea`) and stores verification codes for QR generation.

---

## 🔐 3. Security & Authentication

- **Stateless JWT**: Authentication uses JSON Web Tokens signed with HMAC-SHA256.
- **API Client Credentials**: POS terminals authenticate via `POST /api/v1/auth/token` with `clientId` and `clientSecret`.
- **Spring Security 6 Filter Chain**: Enforces bearer token verification on protected endpoints (`/api/v1/invoices/**`, `/api/v1/fiscal/**`).
- **RFC 7807 Error Handling**: `GlobalExceptionHandler` returns standard HTTP Problem Details for validation, authorization, and domain errors.

---

## 🗄️ 4. Database Schema (PostgreSQL + Flyway)

Database migrations are managed via **Flyway** in `src/main/resources/db/migration/`:

- `V2__create_api_clients.sql`: API client tables and client roles.
- `V3__add_email_authentication_fields.sql`: Extended client authentication fields.
- `V4__add_fiscal_migration_fields.sql`: Company linking and hash chain seeding.
- `V5__create_verifactu_subscriptions.sql`: Verifactu subscription billing tiers.
- `V6__add_payment_status_to_verifactu_subscriptions.sql`: Subscription payment tracking.
- `V7__create_core_fiscal_tables.sql`: Core domain tables (`companies`, `pos_terminals`, `invoices`, `invoice_lines`, `fiscal_records`).

---

## 🧪 5. Testing Strategy

- **Unit Tests**: Test XML builders, digital signers, domain services, exception handlers, and JWT providers without database or network dependencies.
- **Integration Tests**: `@SpringBootTest` tests for authentication flows and JPA persistence with H2/PostgreSQL.
- **E2E Staging Tests**: `VerifactuRealE2ETest` performs live SOAP transactions against the AEAT staging server using test certificates.
