# 💳 NovaPay Backend — Hexagonal Architecture & Verifactu AEAT Integration

🇺🇸 **English** | 🇪🇸 [Leer en Español](README.md)

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20Ports%20%26%20Adapters-blue.svg)](#system-architecture-ports--adapters)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Case Study / Technical Portfolio**: Enterprise backend in **Java 21** and **Spring Boot 3** designed using **Hexagonal Architecture (Ports and Adapters)** and **Domain-Driven Design (DDD)**. Implements the Spanish electronic invoicing regulation **Verifactu (AEAT - Royal Decree 1007/2023)** featuring cryptographic record chaining (SHA-256), XMLDSig digital signatures with PKCS#12 keystores, and stateless JWT authentication.

---

## 📌 Executive Summary

**NovaPay Backend** is the core processing engine for simplified and full electronic invoicing designed to integrate with POS terminals (e.g., Flutter/Mobile apps) and commercial management systems.

The system ensures strict regulatory compliance with **Spanish Royal Decree 1007/2023 (Verifactu Regulation / AEAT)**:
1. **Tamper-Evident Chained Hashing**: Generation of sequential SHA-256 hashes per company/issuer.
2. **Advanced Digital Signatures**: Signing SOAP/XML messages via XAdES/XMLDSig (RSA-SHA256) standards using PKCS#12 keystores.
3. **Direct Integration with AEAT**: Client SOAP mTLS communication with Spanish Tax Agency web services.
4. **QR Code Verification**: Generation of Secure Verification Codes (CSV) and public verification URLs.

---

## 🏛️ System Architecture (Ports & Adapters)

The project enforces clean separation of concerns across three core layers:

```
                          ┌──────────────────────────────────────────────────────────┐
                          │                      INBOUND LAYER                       │
                          │   (REST Controllers / OpenAPI / DTOs / JWT Security)    │
                          └────────────────────────────┬─────────────────────────────┘
                                                       │
                                                       ▼  (Driving Ports)
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                              DOMAIN LAYER                                               │
│                                                                                                         │
│  ┌────────────────────────┐         ┌────────────────────────┐         ┌─────────────────────────────┐  │
│  │     Models & Value     │         │   Use Cases / App      │         │     Domain Enums &          │  │
│  │    Objects (Money,     │ <------ │   Services (Emit,      │ <------ │    Exceptions (InvoiceType,  │  │
│  │    Invoice, Record)    │         │   Retry, Verification) │         │    TaxType, Status)         │  │
│  └────────────────────────┘         └────────────────────────┘         └─────────────────────────────┘  │
└──────────────────────────────────────────────────────┬──────────────────────────────────────────────────┘
                                                       │  (Driven Ports)
                                                       ▼
                          ┌──────────────────────────────────────────────────────────┐
                          │                      OUTBOUND LAYER                      │
                          │  - Verifactu SOAP Adapter (AEAT WebService Client)       │
                          │  - XML Signer Adapter (Apache Santuario / PKCS#12)       │
                          │  - Persistence Adapter (Spring Data JPA + Flyway)        │
                          │  - Hashing Adapter (SHA-256 Chaining)                     │
                          └──────────────────────────────────────────────────────────┘
```

### Directory Structure

```
src/main/java/levelup42/novapay_backend_hex/
├── domain/                               # Core business logic (framework-decoupled)
│   ├── model/                            # Entities & Value Objects (Money, Invoice, FiscalRecord)
│   ├── port/
│   │   ├── in/                           # Driving Ports (InvoiceCreateUseCase, etc.)
│   │   └── out/                          # Driven Ports (FiscalAgencyPort, XmlSignerPort, Repositories)
│   └── service/                          # Pure domain logic & tax calculation services
├── application/                          # Use case orchestration (EmitInvoiceService, RetryService)
└── infrastructure/                       # Technical implementation & adapters
    ├── adapter/
    │   ├── in/rest/                      # REST Controllers, DTOs & Mappers (MapStruct/Jackson)
    │   └── out/
    │       ├── fiscal/verifactu/         # AEAT SOAP Client, XML Builder & Response Parser
    │       ├── persistence/              # JPA Repositories & Database Entities
    │       └── signing/                  # XMLDSig Digital Signature / PKCS#12
    ├── config/                           # SecurityConfig, GlobalExceptionHandler (RFC 7807), OpenApiConfig
    └── security/jwt/                     # JWT Provider & Auth Filters
```

---

## 🔒 DevSecOps & Sanitization (Public Edition)

This portfolio repository has been fully sanitized following corporate DevSecOps best practices:
- 🛡️ **Zero Hardcoded Secrets**: All private keys, database credentials, and JWT secrets utilize environment variable injection with safe development fallbacks.
- 🔑 **Keystore Management**: Real PKCS#12 certificates are stripped. Self-signed test certificate generation scripts (`scripts/generate-dummy-cert.sh` / `.ps1`) are provided.
- 📄 **Data Anonymization**: All Tax IDs (NIF/CIF), company names, and production endpoints have been replaced with RFC compliant placeholders (e.g., `12345678Z`, `SAMPLE COMPANY S.L.`, `https://api.example.com`).
- 🚫 **Robust .gitignore**: Strict exclusion of `.p12`, `.key`, `.env`, log files, and build artifacts.

---

## 🚀 Quick Start Guide (Local Development)

### Prerequisites

- **Java 21 JDK** or higher.
- **Docker Desktop** (for local PostgreSQL database).
- **Maven 3.9+** (or included `./mvnw` wrapper).

### 1. Clone the Repository
```bash
git clone https://github.com/AaronSGomez/novapay-backend-public.git
cd novapay-backend-public
```

### 2. Environment Setup
Copy the environment template:
```bash
cp .env.example .env
```

### 3. Generate Self-Signed Test Certificate
Run the test PKCS#12 certificate generator:

**Linux / macOS (Bash):**
```bash
chmod +x scripts/generate-dummy-cert.sh
./scripts/generate-dummy-cert.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\generate-dummy-cert.ps1
```

### 4. Start PostgreSQL with Docker
```bash
docker compose up -d
```
> Flyway will automatically execute all SQL migrations (`V2` to `V7`) upon boot.

### 5. Compile and Run
```bash
./mvnw spring-boot:run
```

The server will listen on `http://localhost:8080`.

---

## 📖 API Documentation (OpenAPI / Swagger)

Once running, interactive Swagger UI documentation is accessible at:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: `http://localhost:8080/v3/api-docs`

### Core REST Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/v1/auth/token` | JWT Token Generation for POS Clients | None (Public with Client ID/Secret) |
| `POST` | `/api/v1/invoices` | Create & Submit Invoice (Simplified/Full) | `Bearer JWT` |
| `POST` | `/api/v1/invoices/{id}/cancel` | Cancel Issued Invoice | `Bearer JWT` |
| `GET` | `/api/v1/fiscal/status/{invoiceId}` | Query Fiscal Status (CSV, QR URL) | `Bearer JWT` |
| `POST` | `/api/v1/fiscal/retry/{invoiceId}` | Manual Retry for Failed/Pending Invoices | `Bearer JWT` |

---

## 🧪 Automated Testing

The project includes unit and integration tests powered by **JUnit 5** and **Mockito**:

```bash
# Run unit test suite
./mvnw test "-Dtest=VerifactuXmlBuilderTest,XmlSignerImplTest,GlobalExceptionHandlerTest,JwtTokenProviderTest,EmitInvoiceServiceTest,FiscalEvidenceServiceTest,RetryFiscalSubmissionServiceTest,HashServiceImplTest"

# Run E2E test against AEAT staging endpoint (requires test certificate)
./mvnw test -Dtest=VerifactuRealE2ETest
```

---

## 📚 Technical Documentation (English)

Detailed English documentation available in the `Documentacion/en/` directory:
- [Architecture & Master Backend Guide](Documentacion/en/master_backend_guide.md)
- [Invoice Emission & Fiscal Flow](Documentacion/en/invoice_emission_flow.md)
- [Verifactu Production Fields & Compliance](Documentacion/en/verifactu_production_fields.md)
- [Testing & Powershell Guide](Documentacion/en/verifactu_testing_commands.md)

---

## 📜 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
