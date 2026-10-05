# Flujo Completo de Emisión de Facturas — NovaPay → AEAT (Verifactu)

> Documento técnico para desarrollo Flutter. Describe el ciclo completo desde la petición del TPV hasta la verificación QR del ticket.

---

## 1. Lo que recibe el Backend desde Flutter (POST Emisión)

**Endpoint:** `POST /api/v1/invoices`  
**Auth:** Bearer JWT (`Authorization: Bearer <token>`)  
**Response HTTP:** `201 Created`

### Request Body

```json
{
  "series":     "TEST",
  "number":     1024,
  "type":       "SIMPLIFICADA",
  "companyId":  "uuid-de-la-empresa",
  "terminalId": "uuid-del-tpv",
  "issueDate":  "2026-03-12",
  "lines": [
    {
      "description": "Café con leche",
      "quantity":    1,
      "unitPrice":   1.50,
      "taxType":     "IVA_REDUCIDO"
    },
    {
      "description": "Tostada",
      "quantity":    2,
      "unitPrice":   2.00,
      "taxType":     "IVA_REDUCIDO"
    }
  ],
  "rectifiedInvoiceId": null
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `series` | `String` | Serie de la factura (ej. `F`, `TEST`) |
| `number` | `int` | Número de la factura en la serie |
| `type` | `SIMPLIFICADA` / `COMPLETA` | Tipo de factura Verifactu |
| `companyId` | `UUID` | ID de la empresa en BD |
| `terminalId` | `UUID` | ID del TPV que realiza la venta |
| `issueDate` | `yyyy-MM-dd` | Fecha de expedición |
| `lines[].description` | `String` | Descripción del artículo |
| `lines[].quantity` | `decimal` | Cantidad vendida |
| `lines[].unitPrice` | `decimal` | Precio sin IVA por unidad |
| `lines[].taxType` | `TaxType` | Ver tabla de tipos de IVA abajo |
| `rectifiedInvoiceId` | `UUID` o `null` | Solo para facturas rectificativas |

### Tipos de IVA (`TaxType`)

| Valor | Descripción | % Tipo |
|---|---|---|
| `IVA_GENERAL` | IVA general | 21% |
| `IVA_REDUCIDO` | IVA reducido (hostelería, alimentación) | 10% |
| `IVA_SUPERREDUCIDO` | IVA superreducido (productos básicos) | 4% |
| `EXENTO` | Exento de IVA | 0% |
| `NO_SUJETO` | No sujeto a IVA | — |

---

## 2. Lo que el Backend Hace Internamente

```
Flutter → POST /api/v1/invoices
                │
                ▼
        InvoiceController
                │
                ▼
        EmitInvoiceService
          ├── Valida empresa y TPV
          ├── TaxCalculationService → calcula bases e IVA
          ├── InvoiceRepository.save() → persiste la factura
          ├── FiscalEvidenceService.prepareFiscalEvidence()
          │     ├── Lee hash del último FiscalRecord → encadenamiento
          │     ├── HashService.computeHash() → genera hash actual
          │     └── FiscalRecordRepository.save(PENDIENTE_ENVIO)
          └── FiscalAgencyPort.submit()  [adaptador Verifactu]
                ├── VerifactuXmlBuilder → genera SOAP XML
                ├── XmlSignerPort → firma digital con certificado PKCS#12
                ├── VerifactuSoapClient → envía a AEAT
                └── VerifactuResponseParser → procesa respuesta
                      └── FiscalRecordRepository.save(ACEPTADO / RECHAZADO)
```

---

## 3. Lo que se Envía a AEAT (XML Verifactu)

El XML firmado sigue la especificación `SuministroInformacion.xsd`. Ejemplo real de estructura:

```xml
<env:Envelope xmlns:sum="...SuministroLR.xsd" xmlns:sum1="...SuministroInformacion.xsd">
  <env:Body>
    <sum:RegFactuSistemaFacturacion>
      <sum:Cabecera>
        <sum1:ObligadoEmision>
          <sum1:NombreRazon>EMPRESA DE PRUEBAS S.L.</sum1:NombreRazon>
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
            <sum1:NombreRazon>EMPRESA DE PRUEBAS S.L.</sum1:NombreRazon>
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
  <ds:Signature>... firma digital RSA-SHA256 ...</ds:Signature>
</env:Envelope>
```

---

## 4. Respuesta de AEAT y lo que Guardamos

AEAT devuelve un `tikR:RespuestaRegFactuSistemaFacturacion` con:

| Campo AEAT | Descripción | Guardado en BD |
|---|---|---|
| `EstadoEnvio` | `Correcto` / `ParcialmenteCorrecto` / `Incorrecto` | `fiscal_records.status` |
| `CSV` | Código Seguro de Verificación | `fiscal_records.agency_invoice_id` |
| `EstadoRegistro` | Estado por línea de factura | — |
| `CodigoErrorRegistro` | Código numérico de error | — |

**Respuesta real recibida** (test de homologación):
```
estadoEnvio    = ParcialmenteCorrecto
csv            = A-GG87NRMDWM7HHQ
estadoRegistro = AceptadoConErrores
```

---

## 5. Lo que Devuelve el Backend a Flutter

### `POST /api/v1/invoices` → `201 Created`

```json
{
  "id":          "550e8400-e29b-41d4-a716-446655440000",
  "series":      "TEST",
  "number":      1234,
  "type":        "SIMPLIFICADA",
  "status":      "EMITIDA",
  "issueDate":   "2026-03-12",
  "totalAmount": 3.50
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | ID de la factura en nuestra BD |
| `series` | `String` | Serie del número de factura |
| `number` | `int` | Número de la factura |
| `type` | `String` | `SIMPLIFICADA` / `COMPLETA` |
| `status` | `String` | `EMITIDA` incluso si AEAT falla (reintento en background) |
| `issueDate` | `yyyy-MM-dd` | Fecha de expedición |
| `totalAmount` | `decimal` | Importe total (base + IVA) |

> ⚠️ **`status = EMITIDA` siempre** en la respuesta inmediata, incluso si AEAT tarda. El registro fiscal se actualiza en background.

---

## 6. Consulta del Estado Fiscal (Para el QR del Ticket)

Después de emitir, Flutter puede hacer polling para obtener el CSV y la URL de verificación (necesarios para el QR del ticket).

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

| Campo | Tipo | Descripción |
|---|---|---|
| `status` | `String` | `PENDIENTE_ENVIO` / `ACEPTADO` / `RECHAZADO` / `ERROR_TECNICO` |
| `secureVerificationCode` | `String` | CSV de AEAT — usar para generar el QR |
| `verificationUrl` | `String` | URL completa para el QR impreso en el ticket |

### Lógica de Polling en Flutter (sugerida)

```dart
// Después de emitir la factura:
// 1. Guardar invoiceId de la respuesta
// 2. Hacer polling cada 2 segundos hasta que status != PENDIENTE_ENVIO
// 3. Cuando status == ACEPTADO → mostrar QR con verificationUrl
// 4. Timeout: máximo 30 segundos, luego mostrar ticket sin QR

Future<FiscalStatus> pollFiscalStatus(String invoiceId) async {
  for (int i = 0; i < 15; i++) {
    final response = await http.get('/api/v1/fiscal/status/$invoiceId');
    final status = FiscalStatus.fromJson(jsonDecode(response.body));
    if (status.status != 'PENDIENTE_ENVIO') return status;
    await Future.delayed(Duration(seconds: 2));
  }
  throw TimeoutException('AEAT no respondió a tiempo');
}
```

---

## 7. Reintento Manual

Si una factura queda en `RECHAZADO` o `ERROR_TECNICO`, se puede reintentar manualmente:

### `POST /api/v1/fiscal/retry/{invoiceId}` → `204 No Content`

No tiene body. El sistema reintenta el envío a AEAT y actualiza el `FiscalRecord`.

---

## 8. Anulación de Factura

### `POST /api/v1/invoices/{id}/cancel` → `200 OK`

```json
{
  "reason": "Error en el artículo"
}
```

Devuelve el mismo `InvoiceResponse` con `status: ANULADA`.

---

## 9. Diagrama de Flujo Completo

```
[Flutter TPV]
     │
     │ POST /api/v1/invoices (JSON venta)
     │
     ▼
[Backend NovaPay]
     ├── Persiste factura en BD
     ├── Calcula hash encadenado
     ├── Genera y firma XML Verifactu
     ├── Envía a AEAT
     │
     │ ──── respuesta instantánea ────►  { "status": "EMITIDA", "id": "..." }
     │
     └── (background) Actualiza FiscalRecord con CSV y status AEAT
          │
          ▼
[Flutter] ── polling GET /api/v1/fiscal/status/{invoiceId}
               │
               ▼ status = ACEPTADO
          Genera QR con verificationUrl
               │
               ▼
          Imprime ticket con QR de verificación AEAT
```
