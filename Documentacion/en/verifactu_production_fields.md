# Production Go-Live Fields — Verifactu AEAT

> Technical document detailing all environment-dependent configurations and parameters required when transitioning from testing/staging to live production with AEAT.

---

## Summary

During local development and testing, transactions are executed against the AEAT staging endpoint using dummy or self-signed test certificates (`12345678Z` / `SAMPLE COMPANY S.L.`).

In production, the issuer must use a valid corporate digital certificate (FNMT Representative or Corporate Seal) registered with the Spanish Tax Agency (AEAT).

---

## 1. `VerifactuXmlBuilder.java` — `buildSistemaInformatico()`

Path: `src/main/java/.../verifactu/VerifactuXmlBuilder.java`

| XML Field | Staging / Test Value | Production Value | XSD Type |
|---|---|---|---|
| `NombreRazon` | Test Issuer (`SAMPLE COMPANY S.L.`) | **Registered Corporate Name** in AEAT | TextMax120 |
| `NIF` | Test Tax ID (`12345678Z`) | **Corporate Tax ID (NIF)** registered in Verifactu | TextMax9 |
| `NombreSistemaInformatico` | `NovaPay` | `NovaPay` ✅ (unchanged) | TextMax30 |
| `IdSistemaInformatico` | `01` | `01` ✅ (unchanged) | TextMax2 |
| `Version` | `1.0` | `1.0` ✅ (unchanged) | TextMax5 |
| `NumeroInstalacion` | `NOVAPAY-001` | `NOVAPAY-001` ✅ (unchanged) | TextMax30 |

---

## 2. Environment Variables (`.env`)

| Variable | Staging / Test Value | Production Value |
|---|---|---|
| `CERT_ALIAS` | `test-alias` | FNMT Corporate Certificate Alias |
| `CERT_P12_BASE64` | Self-signed test .p12 Base64 | **Corporate PKCS#12 Certificate** Base64 string |
| `CERT_PASSWORD` | `changeit` | Corporate Certificate Passphrase |
| `CERT_SIGNING_ENABLED` | `true` | `true` ✅ |
| `VERIFACTU_PRODUCTION` | `false` (staging endpoint) | **`true`** (live production endpoint) |
| `VERIFACTU_ENDPOINT_URL` | Staging AEAT URL | **Production AEAT URL** |

---

## 3. Web Service Endpoints

| Environment | Endpoint URL |
|---|---|
| **Staging (Homologación)** | `https://prewww1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP` |
| **Production** | `https://www1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP` |

Toggle via `VERIFACTU_PRODUCTION=true/false` in `.env`.
