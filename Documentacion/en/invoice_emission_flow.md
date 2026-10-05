# Complete Invoice Emission Flow — NovaPay → AEAT (Verifactu)

> Technical reference guide for client integration (e.g., Flutter / Mobile POS / Frontend). Details the full lifecycle from POS purchase to ticket QR verification.

---

## 1. What the Backend Receives (POST Emission)

**Endpoint:** `POST /api/v1/invoices`  
**Auth Header:** `Authorization: Bearer <token>`  
**HTTP Response:** `201 Created`

### Request Body Example

```json
{
  "series":     "TEST",
  "number":     1024,
  "type":       "SIMPLIFICADA",
  "companyId":  "550e8400-e29b-41d4-a716-446655440000",
  "terminalId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "issueDate":  "2026-03-12",
  "lines": [
    {
      "description": "Espresso Coffee",
      "quantity":    1,
      "unitPrice":   1.50,
      "taxType":     "IVA_REDUCIDO"
    },
    {
      "description": "Toast",
      "quantity":    2,
      "unitPrice":   2.00,
      "taxType":     "IVA_REDUCIDO"
    }
  ],
  "rectifiedInvoiceId": null
}
```

### Parameter Description

| Field | Type | Description |
|---|---|---|
| `series` | `String` | Invoice series prefix (e.g., `F`, `TEST`) |
| `number` | `int` | Sequential invoice number |
| `type` | `SIMPLIFICADA` / `COMPLETA` | Verifactu invoice type (Simplified / Full) |
| `companyId` | `UUID` | Issuing company ID in database |
| `terminalId` | `UUID` | POS terminal ID performing the sale |
| `issueDate` | `yyyy-MM-dd` | Date of issuance |
| `lines[].description` | `String` | Item description |
| `lines[].quantity` | `decimal` | Unit quantity sold |
| `lines[].unitPrice` | `decimal` | Net unit price before tax |
| `lines[].taxType` | `TaxType` | VAT tax rate (see table below) |
| `rectifiedInvoiceId` | `UUID` or `null` | Target invoice ID if this is a corrective invoice |

### Tax Types (`TaxType`)

| Enum Value | Description | Rate |
|---|---|---|
| `IVA_GENERAL` | Standard VAT rate | 21% |
| `IVA_REDUCIDO` | Reduced VAT rate (hospitality, food) | 10% |
| `IVA_SUPERREDUCIDO` | Super-reduced VAT rate (basic items) | 4% |
| `EXENTO` | Tax Exempt | 0% |
| `NO_SUJETO` | Non-taxable transaction | — |

---

## 2. Internal Backend Workflow

```
Client App → POST /api/v1/invoices
                 │
                 ▼
         InvoiceController
                 │
                 ▼
         EmitInvoiceService
           ├── Validates Company & POS Terminal
           ├── TaxCalculationService → computes net base & VAT amounts
           ├── InvoiceRepository.save() → persists invoice entity
           ├── FiscalEvidenceService.prepareFiscalEvidence()
           │     ├── Fetches hash of previous FiscalRecord → chain linkage
           │     ├── HashService.computeHash() → generates current SHA-256 hash
           │     └── FiscalRecordRepository.save(PENDIENTE_ENVIO)
           └── FiscalAgencyPort.submit()  [Verifactu Adapter]
                 ├── VerifactuXmlBuilder → builds SOAP XML Envelope
                 ├── XmlSignerPort → signs XML via RSA-SHA256 (PKCS#12)
                 ├── VerifactuSoapClient → transmits payload to AEAT
                 └── VerifactuResponseParser → parses response SOAP
                       └── FiscalRecordRepository.save(ACEPTADO / RECHAZADO)
```

---

## 3. SOAP XML Payload Transmitted to AEAT

The digitally signed XML complies with `SuministroInformacion.xsd`. Structure excerpt:

```xml
<env:Envelope xmlns:sum="...SuministroLR.xsd" xmlns:sum1="...SuministroInformacion.xsd">
  <env:Body>
    <sum:RegFactuSistemaFacturacion>
      <sum:Cabecera>
        <sum1:ObligadoEmision>
          <sum1:NombreRazon>SAMPLE COMPANY S.L.</sum1:NombreRazon>
          <sum1:NIF>12345678Z</sum1:NIF>
        </sum1:ObligadoEmision>
      </sum:Cabecera>
      <sum:RegistroFactura>
        <sum1:RegistroAlta>
          <sum1:IDVersion>1.0</sum1:IDVersion>
          <sum1:IDFactura>
            <sum1:IDEmisorFactura>12345678Z</sum1:IDEmisorFactura>
            <sum1:NumSerieFactura>TEST-01234</sum1:NumSerieFactura>
            <sum1:FechaExpedicionFactura>12-03-2026</sum1:FechaExpedicionFactura>
          </sum1:IDFactura>
          <sum1:TipoFactura>F2</sum1:TipoFactura>
          <sum1:Desglose>
            <sum1:DetalleDesglose>
              <sum1:ClaveRegimen>01</sum1:ClaveRegimen>
              <sum1:CalificacionOperacion>S1</sum1:CalificacionOperacion>
              <sum1:TipoImpositivo>10.00</sum1:TipoImpositivo>
              <sum1:BaseImponibleOimporteNoSujeto>3.18</sum1:BaseImponibleOimporteNoSujeto>
              <sum1:CuotaRepercutida>0.32</sum1:CuotaRepercutida>
            </sum1:DetalleDesglose>
          </sum1:Desglose>
          <sum1:CuotaTotal>0.32</sum1:CuotaTotal>
          <sum1:ImporteTotal>3.50</sum1:ImporteTotal>
          <sum1:Encadenamiento>...</sum1:Encadenamiento>
          <sum1:SistemaInformatico>
            <sum1:NombreRazon>SAMPLE COMPANY S.L.</sum1:NombreRazon>
            <sum1:NIF>12345678Z</sum1:NIF>
            <sum1:NombreSistemaInformatico>NovaPay</sum1:NombreSistemaInformatico>
            <sum1:IdSistemaInformatico>01</sum1:IdSistemaInformatico>
            <sum1:Version>1.0</sum1:Version>
          </sum1:SistemaInformatico>
          <sum1:Huella>D8CD666FD5CF5B17...</sum1:Huella>
        </sum1:RegistroAlta>
      </sum:RegistroFactura>
    </sum:RegFactuSistemaFacturacion>
  </env:Body>
  <ds:Signature>... RSA-SHA256 XMLDSig ...</ds:Signature>
</env:Envelope>
```

---

## 4. Querying Fiscal Status (Ticket QR Generation)

Client applications poll the status endpoint to retrieve the Secure Verification Code (CSV) and AEAT verification URL for ticket QR codes.

### `GET /api/v1/fiscal/status/{invoiceId}` → `200 OK`

```json
{
  "invoiceId":              "550e8400-e29b-41d4-a716-446655440000",
  "status":                 "ACEPTADO",
  "retryCount":             0,
  "sentAt":                 "2026-03-12T10:11:23+01:00",
  "respondedAt":            "2026-03-12T10:11:24+01:00",
  "responseCode":           null,
  "responseDescription":    null,
  "submissionId":           null,
  "secureVerificationCode": "A-GG87NRMDWM7HHQ",
  "verificationUrl":        "https://www2.agenciatributaria.gob.es/wlpl/inwinv/es/es.aeat.dit.adu.einv.qr.QRWidget?csv=A-GG87NRMDWM7HHQ"
}
```
