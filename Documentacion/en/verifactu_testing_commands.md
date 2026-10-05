# Testing & CLI Reference Guide (PowerShell / Maven)

> Execute commands from the repository root folder.

---

## Required Environment Variables

Set test certificate environment variables prior to running test suites:

```powershell
# Load test environment variables for current session
$env:JWT_SECRET     = "dGhpc2lzYWZha2VzZWNyZXRrZXlmb3JkZXZlbG9wbWVudG9ubHkxMjM0NTY3ODkw"
$env:CERT_ALIAS     = "test-alias"
$env:CERT_PASSWORD  = "changeit"
$env:CERT_SIGNING_ENABLED = "true"
```

---

## Test Suites Execution

### 1. Unit Tests (Isolated)
Runs XML Builder, XMLDSig Signer, and Exception Handler unit tests without database or network connections:

```powershell
.\mvnw.cmd test "-Dtest=VerifactuXmlBuilderTest,XmlSignerImplTest,GlobalExceptionHandlerTest,JwtTokenProviderTest,EmitInvoiceServiceTest,FiscalEvidenceServiceTest,RetryFiscalSubmissionServiceTest,HashServiceImplTest"
```

### 2. Live Staging E2E Submission (`VerifactuRealE2ETest`)
Submits a test invoice payload to the AEAT staging webservice:

```powershell
.\mvnw.cmd test -Dtest=VerifactuRealE2ETest
```

### 3. Inspection & Diagnostic (`VerifactuDiagnosticoTest`)
Reads the latest database record and writes sent/received XML payloads to disk (`target/last_sent.xml` and `target/last_response.xml`):

```powershell
.\mvnw.cmd test -Dtest=VerifactuDiagnosticoTest
```
